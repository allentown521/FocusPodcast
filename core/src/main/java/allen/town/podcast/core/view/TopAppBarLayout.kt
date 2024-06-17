package allen.town.podcast.core.view

import allen.town.focus_common.databinding.CollapsingAppbarLayoutBinding
import allen.town.focus_common.databinding.SimpleAppbarLayoutBinding
import allen.town.focus_common.databinding.SimpleFixedAppbarLayoutBinding
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
import com.google.android.material.shape.MaterialShapeDrawable
import allen.town.podcast.core.pref.Prefs

class TopAppBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1,
) : AppBarLayout(context, attrs, defStyleAttr) {
    private var simpleAppbarBinding: SimpleAppbarLayoutBinding? = null
    private var collapsingAppbarBinding: CollapsingAppbarLayoutBinding? = null
    private var fixedAppbarBinding: SimpleFixedAppbarLayoutBinding? = null

    val mode: AppBarMode = Prefs.appBarMode

    init {
        if (mode == AppBarMode.COLLAPSING) {
            collapsingAppbarBinding =
                CollapsingAppbarLayoutBinding.inflate(LayoutInflater.from(context), this, true)
            val isLandscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape) {
                fitsSystemWindows = false
            }

        } else if(mode == AppBarMode.FIXED){
            fixedAppbarBinding =
                SimpleFixedAppbarLayoutBinding.inflate(LayoutInflater.from(context), this, true)
            /*fixedAppbarBinding?.root?.applyInsetter {
                type(navigationBars = true) {
                    padding(horizontal = true)
                }
            }*/
        } else {
            simpleAppbarBinding =
                SimpleAppbarLayoutBinding.inflate(LayoutInflater.from(context), this, true)
            /*simpleAppbarBinding?.root?.applyInsetter {
                type(navigationBars = true) {
                    padding(horizontal = true)
                }
            }*/
            statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(context)
        }
    }

    fun pinWhenScrolled() {
        simpleAppbarBinding?.root?.updateLayoutParams<LayoutParams> {
            scrollFlags = SCROLL_FLAG_NO_SCROLL
        }
    }

    val toolbar: Toolbar
        get() = if (mode == AppBarMode.COLLAPSING) {
            collapsingAppbarBinding?.toolbar!!
        } else if(mode == AppBarMode.FIXED){
            fixedAppbarBinding?.toolbar!!
        } else {
            simpleAppbarBinding?.toolbar!!
        }

    var title: String
        get() = if (mode == AppBarMode.COLLAPSING) {
            collapsingAppbarBinding?.collapsingToolbarLayout?.title.toString()
        } else if(mode == AppBarMode.FIXED){
            fixedAppbarBinding?.appNameText?.text.toString()
        } else {
            simpleAppbarBinding?.appNameText?.text.toString()
        }
        set(value) {
            if (mode == AppBarMode.COLLAPSING) {
                collapsingAppbarBinding?.collapsingToolbarLayout?.title = value
            }else if(mode == AppBarMode.FIXED){
                fixedAppbarBinding?.appNameText?.text = value
            }  else {
                simpleAppbarBinding?.appNameText?.text = value
            }
        }

    enum class AppBarMode {
        COLLAPSING,
        SIMPLE,
        FIXED
    }
}
