package allen.town.podcast.fragment

import android.os.Bundle
import android.view.View
import allen.town.podcast.event.CoverColorChangeEvent

/**
 * 有个bug，如果有章节，点击了菜单的更多，然后切换颜色不会变化，再点击更多恢复正常
 */
class VinylAudioPlayerFragment : BlurAudioPlayerFragment() {
    override fun coverColorUpdate(event: CoverColorChangeEvent) {
        super.coverColorUpdate(event)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        ToolbarContentTintHelper.colorizeToolbar(_binding.toolbar, Color.WHITE, activity)
    }



}