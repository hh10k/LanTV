package au.id.blackwell.kurt.lantv

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView

import au.id.blackwell.kurt.lantv.player.TvPlayerStatusListener
import au.id.blackwell.kurt.lantv.player.TvPlayerState

class TvPlayerStatusView : ConstraintLayout, TvPlayerStatusListener {
    private var mSlider: View? = null
    private var mTitle: TextView? = null
    private var mText: TextView? = null
    private var mVisible = false

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context)
    }

    private fun init(context: Context) {
        if (isInEditMode) {
            return
        }

        // Populate this view based on the layout resource
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_tv_player_status, this, true)

        mSlider = findViewById<View>(R.id.slider) as View
        mTitle = findViewById<View>(R.id.title) as TextView
        mText = findViewById<View>(R.id.text) as TextView
    }

    fun setTitle(title: String) {
        mTitle!!.text = title
    }

    private fun setVisible(visible: Boolean) {
        if (mVisible == visible) {
            return
        }

        mVisible = visible

        if (!visible) {
            val anim = AnimationUtils.loadAnimation(context, R.anim.anim_slide_status)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}

                override fun onAnimationRepeat(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    mSlider!!.clearAnimation()
                    mSlider!!.visibility = View.GONE
                }
            })
            mSlider!!.startAnimation(anim)
        } else {
            mSlider!!.clearAnimation()
            mSlider!!.visibility = View.VISIBLE
        }
    }

    override fun onTvPlayerStateChanged(state: TvPlayerState, progress: Float) {
        when (state) {
            TvPlayerState.RESOLVING -> {
                mText!!.text = context.getString(R.string.status_resolving_progress, (progress * 100).toInt())
                setVisible(true)
            }
            TvPlayerState.CONNECTING -> {
                mText!!.text = context.getString(R.string.status_connecting)
                setVisible(true)
            }
            TvPlayerState.BUFFERING -> {
                mText!!.text = context.getString(R.string.status_buffering_progress, (progress * 100).toInt())
                setVisible(true)
            }
            TvPlayerState.PLAYING -> {
                mText!!.text = context.getString(R.string.status_playing)
                setVisible(false)
            }
            TvPlayerState.STOPPED -> {
                mText!!.text = context.getString(R.string.status_stopped)
                setVisible(true)
            }
            TvPlayerState.FAILED -> {
                mText!!.text = context.getString(R.string.status_failed_unknown)
                setVisible(true)
            }
            else -> {
                mText!!.text = ""
                setVisible(false)
            }
        }
    }

    override fun onTvPlayerFailed(reason: String) {
        mText!!.text = context.getString(R.string.status_failed_reason, reason)
        setVisible(true)
    }
}
