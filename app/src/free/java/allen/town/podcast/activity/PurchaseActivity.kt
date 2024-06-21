package allen.town.podcast.activity
/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */

import allen.town.core.service.AliPayService
import allen.town.core.service.GooglePayService
import allen.town.core.service.PayService
import allen.town.focus_common.util.Timber
import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.focus_common.util.Util
import allen.town.focus_purchase.iap.SupporterManager
import allen.town.focus_purchase.iap.SupporterManagerWrap.getSupporterManger
import allen.town.focus_purchase.ui.RestoreAlipayFragment
import allen.town.podcast.R
import allen.town.podcast.databinding.ActivityProVersionBinding
import allen.town.podcast.event.PurchaseEvent
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.ViewCollections
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.util.MaterialUtil
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller
import allen.town.focus_common.extensions.setEdgeToEdgeOrImmersive
import allen.town.focus_common.extensions.setLightStatusBar
import allen.town.focus_common.extensions.setStatusBarColor
import allen.town.focus_purchase.data.db.table.GooglePlayInAppTable
import allen.town.focus_purchase.iap.util.GooglePayUtil.ALIPAY_REMOVE_AD
import allen.town.focus_purchase.iap.util.GooglePayUtil.toGoogleSkuDetail
import allen.town.podcast.activity.SimpleToolbarActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import com.google.android.material.snackbar.Snackbar
import com.wyjson.router.GoRouter
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription


class PurchaseActivity : SimpleToolbarActivity() {

    private lateinit var binding: ActivityProVersionBinding
    lateinit var onSwitchPurchase: OnSwitchPurchaseInterface
    var selectedPurchaseSubId = GoRouter.getInstance().getService(GooglePayService::class.java)!!.getYearlyId()
    private var supporterManager: SupporterManager? = null
    private val lifecycleSubscriptions = CompositeSubscription()

    @JvmField
    @BindViews(
        R.id.upgradeImage0,
        R.id.upgradeImage1,
        R.id.upgradeImage2,
        R.id.upgradeImage3,
        R.id.upgradeImage4,
        R.id.upgradeImage5,
        R.id.upgradeImage6,
        R.id.upgradeImage7,
        R.id.upgradeImage8,
        R.id.upgradeImage9,
        R.id.upgradeImage10,
        R.id.upgradeImage11,
        R.id.upgradeImage12,
    )
    var actionsIcons: List<@JvmSuppressWildcards ImageView>? = null

    @JvmField
    @BindViews(
        R.id.upgradeText0,
        R.id.upgradeText1,
        R.id.upgradeText2,
        R.id.upgradeText3,
        R.id.upgradeText4,
        R.id.upgradeText5,
        R.id.upgradeText6,
        R.id.upgradeText7,
        R.id.upgradeText8,
        R.id.upgradeText9,
        R.id.upgradeText10,
        R.id.upgradeText11,
        R.id.upgradeText12,
    )
    var actionsTextViews: List<@JvmSuppressWildcards TextView>? = null
    private var isAlipayRemoveAd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProVersionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ButterKnife.bind(this)
        setEdgeToEdgeOrImmersive(R.id.status_bar, true)
        setStatusBarColor(Color.TRANSPARENT, R.id.status_bar)
        setLightStatusBar(false)
        binding.toolbar.navigationIcon?.setTint(Color.WHITE)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        ThemedFastScroller.create(binding.scrollView)
        isAlipayRemoveAd = intent.getBooleanExtra(ALIPAY_REMOVE_AD, false)
        if (isAlipayRemoveAd) {
            ViewCollections.run(
                actionsIcons!!
            ) { view12: ImageView, i: Int ->
                view12.visibility = View.GONE
            }
            ViewCollections.run(
                actionsTextViews!!
            ) { view12: TextView, i: Int -> view12.visibility = View.GONE }
            binding.upgradeImage0.setVisibility(View.VISIBLE)
            binding.upgradeText0.setVisibility(View.VISIBLE)
        }

        supporterManager = getSupporterManger(this)
        setPurchaseButton()
        val width: Int = (Util.m1210i(this) - Util.m1216c(16)) / 3
        val height = width * 453 / 330
        binding.itemPremiumPro.oneMonthLayout.layoutParams.width = width
        binding.itemPremiumPro.oneMonthLayout.layoutParams.height = height
        binding.itemPremiumPro.oneYearLayout.layoutParams.width = width
        binding.itemPremiumPro.oneYearLayout.layoutParams.height = height
        binding.itemPremiumPro.threeMonthLayout.layoutParams.width = width
        binding.itemPremiumPro.threeMonthLayout.layoutParams.height = height
//        binding.restoreButton.isEnabled = false
//        binding.purchaseButton.isEnabled = false
        onSwitchPurchase = OnSwitchPurchase()

        binding.itemPremiumPro.oneMonthLayout.setOnClickListener {
            SwitchPurchaseOnClickListener(
                binding.itemPremiumPro.oneYearLayout,
                binding.itemPremiumPro.threeMonthLayout,
                GoRouter.getInstance().getService(GooglePayService::class.java)!!.getMonthId()
            ).onClick(it)
        }

        binding.itemPremiumPro.oneYearLayout.setOnClickListener {
            SwitchPurchaseOnClickListener(
                binding.itemPremiumPro.oneMonthLayout,
                binding.itemPremiumPro.threeMonthLayout,
                GoRouter.getInstance().getService(GooglePayService::class.java)!!.getYearlyId()
            ).onClick(it)
        }

        binding.itemPremiumPro.threeMonthLayout.setOnClickListener {
            SwitchPurchaseOnClickListener(
                binding.itemPremiumPro.oneMonthLayout,
                binding.itemPremiumPro.oneYearLayout,
                GoRouter.getInstance().getService(GooglePayService::class.java)!!.getQuarterlyId()
            ).onClick(it)
        }

        MaterialUtil.setTint(binding.purchaseButton, true)

        binding.purchaseButton.setOnClickListener {
            if (!isAlipayRemoveAd) {
                lifecycleSubscriptions.add(
                    supporterManager!!.becomeSubSupporter(
                        this,
                        selectedPurchaseSubId
                    ).subscribeOn(Schedulers.io()).observeOn(
                        AndroidSchedulers.mainThread()
                    )
                        .subscribe({ aBoolean: Boolean ->
                            if (aBoolean) {
                                doOnPurchaseSuccess()
                            }
                        }) { throwable: Throwable? ->
                            Timber.d(
                                throwable,
                                "There was an error while purchasing supporter item"
                            )
                        })
            } else {
                try {
                    lifecycleSubscriptions.add(
                        supporterManager!!.becomeInAppSubSupporter(
                            this,
                            SkuDetails(
                                toGoogleSkuDetail(
                                    GoRouter.getInstance().getService(GooglePayService::class.java)!!.getRemoveAdsId()[0],
                                    BillingClient.SkuType.SUBS,
                                    GoRouter.getInstance().getService(AliPayService::class.java)!!.getRemoveAdPrice()
                                )
                            ),
                            GooglePlayInAppTable.TYPE_REMOVE_ADS
                        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ aBoolean: Boolean ->
                                if (aBoolean) {
                                    doOnPurchaseSuccess()
                                }
                            }) { throwable: Throwable? ->
                                Timber.d(
                                    throwable,
                                    "There was an error while purchasing supporter item"
                                )
                            })
                } catch (e: JSONException) {
                    Timber.e("alipay remove ad failed ", e)
                }
            }
        }
        binding.bannerContainer.backgroundTintList =
            ColorStateList.valueOf(ThemeStore.accentColor(this))
        if (!isAlipayRemoveAd) {
            lifecycleSubscriptions.add(
                supporterManager!!.isSupporter.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ aBoolean: Boolean ->
                        updateSubScribeContainer(aBoolean)

                        //没有付费显示付费按钮和价格
                        if (!aBoolean) {
                            lifecycleSubscriptions.add(
                                supporterManager!!.supporterSubItem.subscribeOn(Schedulers.io())
                                    .observeOn(
                                        AndroidSchedulers.mainThread()
                                    ).subscribe(
                                        { skuDetails: List<SkuDetails> ->
                                            for (detail in skuDetails) {
                                                if (GoRouter.getInstance().getService(GooglePayService::class.java)!!.getQuarterlyId() == detail.sku) {
                                                    binding.itemPremiumPro.threeMonthPrice.text =
                                                        detail.price

                                                } else if (GoRouter.getInstance().getService(GooglePayService::class.java)!!.getYearlyId() == detail.sku) {
                                                    binding.itemPremiumPro.oneYearIntroductoryPrice.text =
                                                        detail.price
                                                } else if (GoRouter.getInstance().getService(GooglePayService::class.java)!!.getMonthId() == detail.sku) {
                                                    binding.itemPremiumPro.oneMonthPrice.text =
                                                        detail.price
                                                } else if (GoRouter.getInstance().getService(GooglePayService::class.java)!!.getWeeklyId() == detail.sku) {
                                                    binding.itemPremiumPro.oneMonthPrice.text =
                                                        detail.price
                                                } else {
                                                    Timber.e("unknown sku %s", detail.sku)
                                                }
                                            }
                                            setPurchaseButton()
                                        }) { throwable: Throwable? ->
                                        Timber.e(
                                            throwable, "There was an error while retrieving " +
                                                    "supporter sub item"
                                        )
                                    }
                            )
                        }
                    }) { throwable: Throwable? ->
                        //抛异常说明没有安装google服务
                        showSnack(
                            this,
                            R.string.google_service_not_install,
                            Snackbar.LENGTH_LONG
                        )
                        updateSubScribeContainer(false)
                        Timber.d(throwable, "There was an error while retrieving supporter status")
                    })
        } else {
            updateSubScribeContainer(false)
            binding.itemPremiumPro.threeMonthLayout.visibility = View.GONE
            binding.itemPremiumPro.oneMonthLayout.visibility = View.GONE
            binding.itemPremiumPro.oneYearLayout.visibility = View.VISIBLE
            binding.itemPremiumPro.oneYearTipTv.visibility = View.GONE
            binding.itemPremiumPro.dayTrialFreeTv.text = "终生"
            binding.itemPremiumPro.oneYearIntroductoryPrice.text =
                GoRouter.getInstance().getService(AliPayService::class.java)!!.getRemoveAdPrice()
        }
    }

    var freeDays = 7
    private fun setPurchaseButton() {
        if (!GoRouter.getInstance().getService(PayService::class.java)!!.isAliPay()) {
            val monthPrice = binding.itemPremiumPro.oneMonthPrice.text
            val threeMonthPrice = binding.itemPremiumPro.threeMonthPrice.text
            val yealyPrice = binding.itemPremiumPro.oneYearIntroductoryPrice.text
            binding.itemPremiumPro.dayTrialTv.text = getString(
                R.string.day_trial,
                freeDays
            )

            if (selectedPurchaseSubId == GoRouter.getInstance().getService(GooglePayService::class.java)!!.getYearlyId()) {
                binding.purchaseButton.text = getString(R.string.try_it_free)
                val yearlyStr = getString(
                    R.string.cancel_any_time,
                    "${
                        getString(
                            R.string.day_trial,
                            freeDays
                        )
                    } ${getString(R.string.then_price_year, yealyPrice)}"
                )
                binding.cancelAnyTimeTv.text = yearlyStr
            } else {
                if (selectedPurchaseSubId == GoRouter.getInstance().getService(GooglePayService::class.java)!!.getMonthId()) {
                    val monthStr = getString(
                        R.string.cancel_any_time,
                        getString(R.string.payment_price_month, monthPrice)
                    )
                    binding.cancelAnyTimeTv.text = monthStr
                } else if (selectedPurchaseSubId == GoRouter.getInstance().getService(GooglePayService::class.java)!!.getQuarterlyId()) {
                    val threeMonthsStr = getString(
                        R.string.cancel_any_time,
                        getString(R.string.payment_price_3month, threeMonthPrice)
                    )
                    binding.cancelAnyTimeTv.text = threeMonthsStr
                }
                binding.purchaseButton.text = getString(R.string.purchase)
            }

        } else {
            binding.itemPremiumPro.dayTrialTv.visibility = View.GONE
            if (!isAlipayRemoveAd) {
                binding.itemPremiumPro.dayTrialFreeTv.text = getString(R.string.recommend)
            }

        }
    }

    private fun doOnPurchaseSuccess() {
        showSnack(this, R.string.thanks_purchase, Toast.LENGTH_LONG)
        updateSubScribeContainer(true)
        EventBus.getDefault().post(PurchaseEvent())
    }

    private fun updateSubScribeContainer(isSupporter: Boolean) {
        Timber.d("updateSubScribeContainer %s", isSupporter)
        if (isSupporter) {
            binding.alreadyVipLottie.visibility = View.VISIBLE
            binding.alreadyPurchaseTv.visibility = View.VISIBLE
            binding.alreadyVipLottie.visibility = View.VISIBLE
            binding.itemPremiumPro.container.visibility = View.GONE
            binding.purchaseButton.visibility = View.GONE
            if (!GoRouter.getInstance().getService(PayService::class.java)!!.isAliPay()) {
                binding.alreadyPurchaseTv.text = getString(R.string.premium_pro_tip)
            }
            binding.cancelAnyTimeTv.visibility = View.GONE
        } else {
            binding.alreadyVipLottie.visibility = View.GONE
            binding.alreadyPurchaseTv.visibility = View.GONE
            binding.alreadyVipLottie.visibility = View.GONE
            binding.itemPremiumPro.container.visibility = View.VISIBLE
            binding.purchaseButton.visibility = View.VISIBLE
        }
        updateAlipayLayout()
    }

    private fun updateAlipayLayout() {
        if (GoRouter.getInstance().getService(PayService::class.java)!!.isAliPay()) {
            binding.cancelAnyTimeTv.visibility = View.GONE
            val expiredTimeStr = supporterManager!!.expriedDate
            if (expiredTimeStr != null) {
                binding.alreadyPurchaseTv.text =
                    String.format(
                        "%s%s",
                        getString(R.string.expired_time),
                        expiredTimeStr
                    )
            }
            if (supporterManager!!.restoreTip() > 0) {
                binding.additionTv.setText(R.string.restore_tip)
                binding.additionTv.setOnClickListener {
                    Observable.create { subscriber: Subscriber<in Boolean?>? ->
                        RestoreAlipayFragment(
                            subscriber!!
                        ).showIt(supportFragmentManager)
                    }.subscribeOn(Schedulers.io()).observeOn(
                        AndroidSchedulers.mainThread()
                    )
                        .subscribe({ aBoolean: Boolean? ->
                            if (aBoolean!!) {
                                doOnPurchaseSuccess()
                            } else {
                                showSnack(
                                    this@PurchaseActivity,
                                    R.string.query_alipay_order_no,
                                    Snackbar.LENGTH_LONG
                                )
                            }
                        }) { throwable: Throwable? ->
                            showSnack(
                                this@PurchaseActivity,
                                R.string.query_alipay_order_failed,
                                Snackbar.LENGTH_LONG
                            )
                            Timber.d(
                                throwable,
                                "There was an error while purchasing supporter item"
                            )
                        }
                }
            }
        }
    }

    inner class SwitchPurchaseOnClickListener(
        val view2: View, val view3: View, val subId: String
    ) :
        View.OnClickListener {
        override fun onClick(view: View) {
            val aVar: OnSwitchPurchaseInterface = onSwitchPurchase
            aVar.onSwitch(view, view2, view3, subId)
        }
    }

    private fun restorePurchase() {
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleSubscriptions.unsubscribe()
        supporterManager!!.dispose()
    }

    companion object {
        private const val TAG: String = "PurchaseActivity"
    }

    interface OnSwitchPurchaseInterface {
        fun onSwitch(view: View, view2: View, view3: View, subId: String)
    }

    fun animatorSwitchPurchase(view: View, z: Boolean) {
        view.clearAnimation()
        var f = 1.0f
        var f2 = 0.8f
        if (!z) {
            f = 0.8f
            f2 = 1.0f
        }
        val ofFloat = ObjectAnimator.ofFloat(view, "scaleX", f, f2)
        val ofFloat2 = ObjectAnimator.ofFloat(view, "scaleY", f, f2)
        val animatorSet = AnimatorSet()
        animatorSet.play(ofFloat).with(ofFloat2)
        animatorSet.duration = 300L
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }


    inner class OnSwitchPurchase : OnSwitchPurchaseInterface {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onSwitch(view: View, view2: View, view3: View, subId: String) {
            selectedPurchaseSubId = subId
            setPurchaseButton()
            if (view.scaleX < 1.0f) {
                animatorSwitchPurchase(view, false)
            }
            val i2 = Build.VERSION.SDK_INT
            if (i2 >= 23) {
                view.foreground =
                    ContextCompat.getDrawable(view.context, R.drawable.transparent)
            }
            if (view2.scaleX > 0.9f) {
                animatorSwitchPurchase(view2, true)
            }
            if (i2 >= 23) {
                view2.foreground =
                    ContextCompat.getDrawable(view2.context, R.drawable.alpha54black)
            }
            if (view3.scaleX > 0.9f) {
                animatorSwitchPurchase(view3, true)
            }
            if (i2 >= 23) {
                view3.foreground =
                    ContextCompat.getDrawable(view3.context, R.drawable.alpha54black)
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.anim_activity_stay, R.anim.retro_fragment_close_exit)
    }
}
