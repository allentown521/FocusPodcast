/*
 * Source: https://github.com/bumptech/glide/wiki/Custom-targets#palette-example
 */

package allen.town.podcast.core.glide;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import allen.town.podcast.core.pref.Prefs;

public class PaletteBitmapTranscoder implements ResourceTranscoder<Bitmap, PaletteBitmap> {

    @Nullable
    @Override
    public Resource<PaletteBitmap> transcode(@NonNull Resource<Bitmap> toTranscode, @NonNull Options options) {
        Bitmap bitmap = toTranscode.get();
        Palette palette = null;
        if (Prefs.shouldShowSubscriptionTitle()) {
            palette = new Palette.Builder(bitmap).generate();
        }
        PaletteBitmap result = new PaletteBitmap(bitmap, palette);
        return new PaletteBitmapResource(result);
    }
}
