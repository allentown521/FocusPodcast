package allen.town.podcast.event

import android.graphics.Bitmap
import allen.town.podcast.fragment.CoverFragment
import allen.town.podcast.util.MediaNotificationProcessor

class CoverColorChangeEvent(val color: MediaNotificationProcessor, val bitmap: Bitmap?,val coverViewHolder: CoverFragment.CoverViewHolder?)