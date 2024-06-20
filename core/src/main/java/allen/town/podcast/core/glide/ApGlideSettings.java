package allen.town.podcast.core.glide;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

/**
 * The settings that FocusPodcast will use for various Glide options
 */
public class ApGlideSettings {
    private ApGlideSettings(){}

    public static final DiskCacheStrategy AP_DISK_CACHE_STRATEGY = DiskCacheStrategy.ALL;
}
