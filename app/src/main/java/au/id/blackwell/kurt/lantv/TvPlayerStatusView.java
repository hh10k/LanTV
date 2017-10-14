package au.id.blackwell.kurt.lantv;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class TvPlayerStatusView extends ConstraintLayout implements TvPlayerListener {
    private View mSlider;
    private TextView mTitle;
    private TextView mText;
    private boolean mVisible = false;

    public TvPlayerStatusView(Context context) {
        super(context);
        init(context);
    }

    public TvPlayerStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TvPlayerStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        if (isInEditMode()) {
            return;
        }

        // Populate this view based on the layout resource
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_tv_player_status, this, true);

        mSlider = (View) findViewById(R.id.slider);
        mTitle = (TextView) findViewById(R.id.title);
        mText = (TextView) findViewById(R.id.text);
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    private void setVisible(boolean visible) {
        if (mVisible == visible) {
            return;
        }

        mVisible = visible;

        if (!visible) {
            final Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_slide_status);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mSlider.clearAnimation();
                    mSlider.setVisibility(GONE);
                }
            });
            mSlider.startAnimation(anim);
        } else {
            mSlider.clearAnimation();
            mSlider.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onTvPlayerStateChanged(TvPlayerState state, float progress) {
        switch (state) {
            case RESOLVING:
                mText.setText(getContext().getString(R.string.status_resolving_progress, (int)(progress * 100)));
                setVisible(true);
                break;
            case CONNECTING:
                mText.setText(getContext().getString(R.string.status_connecting));
                setVisible(true);
                break;
            case BUFFERING:
                mText.setText(getContext().getString(R.string.status_buffering_progress, (int)(progress * 100)));
                setVisible(true);
                break;
            case PLAYING:
                mText.setText(getContext().getString(R.string.status_playing));
                setVisible(false);
                break;
            case PAUSED:
                mText.setText(getContext().getString(R.string.status_paused));
                setVisible(true);
                break;
            case FAILED:
                mText.setText(getContext().getString(R.string.status_failed));
                setVisible(true);
                break;
            default:
                mText.setText("");
                setVisible(false);
        }
    }

    public void onTvPlayerFailed(String reason) {
        mText.setText(getContext().getString(R.string.status_failed_reason, reason));
        setVisible(true);
    }
}
