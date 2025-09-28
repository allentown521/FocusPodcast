package allen.town.podcast.core.service;

import java.io.IOException;

import allen.town.podcast.core.ClientConfig;
import okhttp3.Interceptor;
import okhttp3.Response;

public class UserAgentInterceptor implements Interceptor {
    public static String USER_AGENT = "FocusPodcast/0.0.0";
    @Override
    public Response intercept(Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder()
                .header("User-Agent", ClientConfig.USER_AGENT)
                .build());
    }
}
