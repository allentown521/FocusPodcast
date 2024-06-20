package allen.town.podcast.core.service.download;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import allen.town.podcast.core.service.BasicAuthorizationInterceptor;
import allen.town.podcast.core.service.UserAgentInterceptor;
import allen.town.podcast.model.download.ProxyConfig;
import allen.town.podcast.net.ssl.SslClientSetup;
import okhttp3.Cache;
import okhttp3.Credentials;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import java.io.File;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Provides access to a HttpClient singleton.
 */
public class PodcastHttpClient {
    private static final String TAG = "PodcastHttpClient";
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;
    private static final int MAX_CONNECTIONS = 8;
    private static File cacheDirectory;
    private static ProxyConfig proxyConfig;

    private static volatile OkHttpClient httpClient = null;

    private PodcastHttpClient() {

    }

    /**
     * Returns the HttpClient singleton.
     */
    public static synchronized OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = newBuilder().build();
        }
        return httpClient;
    }

    public static synchronized void reinit() {
        httpClient = newBuilder().build();
    }

    /**
     * Creates a new HTTP client.  Most users should just use
     * getHttpClient() to get the standard client,
     * but sometimes it's necessary for others to have their own
     * copy so that the clients don't share state.
     * @return http client
     */
    @NonNull
    public static OkHttpClient.Builder newBuilder() {

        System.setProperty("http.maxConnections", String.valueOf(MAX_CONNECTIONS));

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.interceptors().add(new BasicAuthorizationInterceptor());
        builder.networkInterceptors().add(new UserAgentInterceptor());

        // set cookie handler
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        builder.cookieJar(new JavaNetCookieJar(cm));

        // set timeouts
        builder.connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.cache(new Cache(cacheDirectory, 20L * 1000000)); // 20MB

        // configure redirects
        builder.followRedirects(true);
        builder.followSslRedirects(true);

        if (proxyConfig != null && proxyConfig.type != Proxy.Type.DIRECT && !TextUtils.isEmpty(proxyConfig.host)) {
            int port = proxyConfig.port > 0 ? proxyConfig.port : ProxyConfig.DEFAULT_PORT;
            SocketAddress address = InetSocketAddress.createUnresolved(proxyConfig.host, port);
            builder.proxy(new Proxy(proxyConfig.type, address));
            if (!TextUtils.isEmpty(proxyConfig.username) && proxyConfig.password != null) {
                builder.proxyAuthenticator((route, response) -> {
                    String credentials = Credentials.basic(proxyConfig.username, proxyConfig.password);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credentials)
                            .build();
                });
            }
        }

        SslClientSetup.installCertificates(builder);
        return builder;
    }

    public static void setCacheDirectory(File cacheDirectory) {
        PodcastHttpClient.cacheDirectory = cacheDirectory;
    }

    public static void setProxyConfig(ProxyConfig proxyConfig) {
        PodcastHttpClient.proxyConfig = proxyConfig;
    }
}
