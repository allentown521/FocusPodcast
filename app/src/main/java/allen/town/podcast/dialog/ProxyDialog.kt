package allen.town.podcast.dialog

import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.R
import android.widget.EditText
import android.widget.ArrayAdapter
import allen.town.podcast.core.pref.Prefs
import android.widget.TextView
import android.widget.Spinner
import io.reactivex.disposables.Disposable
import allen.town.podcast.core.service.download.PodcastHttpClient
import allen.town.podcast.model.download.ProxyConfig
import android.widget.AdapterView
import android.text.TextWatcher
import android.text.Editable
import androidx.core.content.ContextCompat
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AlertDialog
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.lang.NumberFormatException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketAddress
import java.util.ArrayList
import java.util.concurrent.TimeUnit

class ProxyDialog(private val context: Context) {
    private var dialog: AlertDialog? = null
    private var spType: Spinner? = null
    private var etHost: EditText? = null
    private var etPort: EditText? = null
    private var etUsername: EditText? = null
    private var etPassword: EditText? = null
    private var testSuccessful = false
    private var txtvMessage: TextView? = null
    private var disposable: Disposable? = null
    fun show(): Dialog? {
        val content = View.inflate(context, R.layout.proxy_settings, null)
        spType = content.findViewById(R.id.spType)
        dialog = AccentMaterialDialog(
            context,
            R.style.MaterialAlertDialogTheme
        )
            .setTitle(R.string.pref_proxy_title)
            .setView(content)
            .setNegativeButton(R.string.cancel_label, null)
            .setPositiveButton(R.string.proxy_test_label, null)
            .setNeutralButton(R.string.reset, null)
            .show()
        // To prevent cancelling the dialog on button click
        dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { view: View? ->
            if (!testSuccessful) {
                dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                test()
                return@setOnClickListener
            }
            setProxyConfig()
            PodcastHttpClient.reinit()
            dialog!!.dismiss()
        }
        dialog!!.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { view: View? ->
            etHost!!.text.clear()
            etPort!!.text.clear()
            etUsername!!.text.clear()
            etPassword!!.text.clear()
            setProxyConfig()
        }
        val types: MutableList<String> = ArrayList()
        types.add(Proxy.Type.DIRECT.name)
        types.add(Proxy.Type.HTTP.name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            types.add(Proxy.Type.SOCKS.name)
        }
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item, types
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spType!!.setAdapter(adapter)
        val proxyConfig = Prefs.proxyConfig
        spType!!.setSelection(adapter.getPosition(proxyConfig.type.name))
        etHost = content.findViewById(R.id.etHost)
        if (!TextUtils.isEmpty(proxyConfig.host)) {
            etHost!!.setText(proxyConfig.host)
        }
        etHost!!.addTextChangedListener(requireTestOnChange)
        etPort = content.findViewById(R.id.etPort)
        if (proxyConfig.port > 0) {
            etPort!!.setText(proxyConfig.port.toString())
        }
        etPort!!.addTextChangedListener(requireTestOnChange)
        etUsername = content.findViewById(R.id.etUsername)
        if (!TextUtils.isEmpty(proxyConfig.username)) {
            etUsername!!.setText(proxyConfig.username)
        }
        etUsername!!.addTextChangedListener(requireTestOnChange)
        etPassword = content.findViewById(R.id.etPassword)
        if (!TextUtils.isEmpty(proxyConfig.password)) {
            etPassword!!.setText(proxyConfig.password)
        }
        etPassword!!.addTextChangedListener(requireTestOnChange)
        if (proxyConfig.type == Proxy.Type.DIRECT) {
            enableSettings(false)
            setTestRequired(false)
        }
        spType!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                if (position == 0) {
                    dialog!!.getButton(AlertDialog.BUTTON_NEUTRAL).visibility = View.GONE
                } else {
                    dialog!!.getButton(AlertDialog.BUTTON_NEUTRAL).visibility = View.VISIBLE
                }
                enableSettings(position > 0)
                setTestRequired(position > 0)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                enableSettings(false)
            }
        })
        txtvMessage = content.findViewById(R.id.txtvMessage)
        checkValidity()
        return dialog
    }

    private fun setProxyConfig() {
        val type = spType!!.selectedItem as String
        val typeEnum = Proxy.Type.valueOf(type)
        val host = etHost!!.text.toString()
        val port = etPort!!.text.toString()
        var username: String? = etUsername!!.text.toString()
        if (TextUtils.isEmpty(username)) {
            username = null
        }
        var password: String? = etPassword!!.text.toString()
        if (TextUtils.isEmpty(password)) {
            password = null
        }
        var portValue = 0
        if (!TextUtils.isEmpty(port)) {
            portValue = port.toInt()
        }
        val config = ProxyConfig(typeEnum, host, portValue, username, password)
        Prefs.proxyConfig = config
        PodcastHttpClient.setProxyConfig(config)
    }

    private val requireTestOnChange: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            setTestRequired(true)
        }
    }

    private fun enableSettings(enable: Boolean) {
        etHost!!.isEnabled = enable
        etPort!!.isEnabled = enable
        etUsername!!.isEnabled = enable
        etPassword!!.isEnabled = enable
    }

    private fun checkValidity(): Boolean {
        var valid = true
        if (spType!!.selectedItemPosition > 0) {
            valid = checkHost()
        }
        valid = valid and checkPort()
        return valid
    }

    private fun checkHost(): Boolean {
        val host = etHost!!.text.toString()
        if (host.length == 0) {
            etHost!!.error = context.getString(R.string.proxy_host_empty_error)
            return false
        }
        if ("localhost" != host && !Patterns.DOMAIN_NAME.matcher(host).matches()) {
            etHost!!.error = context.getString(R.string.proxy_host_invalid_error)
            return false
        }
        return true
    }

    private fun checkPort(): Boolean {
        val port = port
        if (port < 0 || port > 65535) {
            etPort!!.error = context.getString(R.string.proxy_port_invalid_error)
            return false
        }
        return true
    }

    // ignore
    private val port: Int
        private get() {
            val port = etPort!!.text.toString()
            if (port.length > 0) {
                try {
                    return port.toInt()
                } catch (e: NumberFormatException) {
                    // ignore
                }
            }
            return 0
        }

    private fun setTestRequired(required: Boolean) {
        if (required) {
            testSuccessful = false
            dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.proxy_test_label)
        } else {
            testSuccessful = true
            dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).setText(android.R.string.ok)
        }
        dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
    }

    private fun test() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        if (!checkValidity()) {
            setTestRequired(true)
            return
        }
        val res = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        val textColorPrimary = res.getColor(0, 0)
        res.recycle()
        val checking = context.getString(R.string.proxy_checking)
        txtvMessage!!.setTextColor(textColorPrimary)
        txtvMessage!!.text = "{fa-circle-o-notch spin} $checking"
        txtvMessage!!.visibility = View.VISIBLE
        disposable = Completable.create { emitter: CompletableEmitter ->
            val type = spType!!.selectedItem as String
            val host = etHost!!.text.toString()
            val port = etPort!!.text.toString()
            val username = etUsername!!.text.toString()
            val password = etPassword!!.text.toString()
            var portValue = 8080
            if (!TextUtils.isEmpty(port)) {
                portValue = port.toInt()
            }
            val address: SocketAddress = InetSocketAddress.createUnresolved(host, portValue)
            val proxyType = Proxy.Type.valueOf(type.uppercase())
            val builder = PodcastHttpClient.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .proxy(Proxy(proxyType, address))
            if (!TextUtils.isEmpty(username)) {
                builder.proxyAuthenticator { route: Route?, response: Response ->
                    val credentials = Credentials.basic(username, password)
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credentials)
                        .build()
                }
            }
            val client = builder.build()
            val request = Request.Builder().url("https://www.example.com").head().build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        emitter.onComplete()
                    } else {
                        emitter.onError(IOException(response.message))
                    }
                }
            } catch (e: IOException) {
                emitter.onError(e)
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    txtvMessage!!.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.download_success_green
                        )
                    )
                    val message = String.format(
                        "%s %s", "{fa-check}",
                        context.getString(R.string.proxy_test_successful)
                    )
                    txtvMessage!!.text = message
                    setTestRequired(false)
                }
            ) { error: Throwable ->
                error.printStackTrace()
                txtvMessage!!.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.download_failed_red
                    )
                )
                val message = String.format(
                    "%s %s: %s", "{fa-close}",
                    context.getString(R.string.proxy_test_failed), error.message
                )
                txtvMessage!!.text = message
                setTestRequired(true)
            }
    }
}