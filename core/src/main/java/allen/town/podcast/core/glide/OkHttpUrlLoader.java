package allen.town.podcast.core.glide;

import android.content.ContentResolver;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import allen.town.podcast.core.service.download.PodcastHttpClient;
import allen.town.podcast.core.util.NetworkUtils;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@see com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader}.
 */
class OkHttpUrlLoader implements ModelLoader<String, InputStream> {

    /**
     * The default factory for {@link OkHttpUrlLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<String, InputStream> {

        private static volatile OkHttpClient internalClient;
        private final OkHttpClient client;

        private static OkHttpClient getInternalClient() {
            if (internalClient == null) {
                synchronized (Factory.class) {
                    if (internalClient == null) {
                        OkHttpClient.Builder builder = PodcastHttpClient.newBuilder();
                        builder.interceptors().add(new NetworkAllowanceInterceptor());
                        builder.cache(null); // Handled by Glide
                        internalClient = builder.build();
                    }
                }
            }
            return internalClient;
        }

        /**
         * Constructor for a new Factory that runs requests using a static singleton client.
         */
        Factory() {
            this.client = getInternalClient();
        }

        @NonNull
        @Override
        public ModelLoader<String, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new OkHttpUrlLoader(client);
        }

        @Override
        public void teardown() {
            // Do nothing, this instance doesn't own the client.
        }
    }

    private final OkHttpClient client;

    private OkHttpUrlLoader(OkHttpClient client) {
        this.client = client;
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull String model, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model), new ResizingOkHttpStreamFetcher(client, new GlideUrl(model)));
    }

    @Override
    public boolean handles(@NonNull String model) {
        // Leave content URIs to Glide's default loaders
        return !TextUtils.isEmpty(model)
                && !model.startsWith(ContentResolver.SCHEME_CONTENT)
                && !model.startsWith(ContentResolver.SCHEME_ANDROID_RESOURCE);
    }

    private static class NetworkAllowanceInterceptor implements Interceptor {

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            if (NetworkUtils.isImageAllowed()) {
                return chain.proceed(chain.request());
            } else {
                return new Response.Builder()
                        .protocol(Protocol.HTTP_2)
                        .code(420)
                        .message("Policy Not Fulfilled")
                        .body(ResponseBody.create(null, new byte[0]))
                        .request(chain.request())
                        .build();
            }
        }
    }
}