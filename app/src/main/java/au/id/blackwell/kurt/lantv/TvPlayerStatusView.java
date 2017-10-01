package au.id.blackwell.kurt.lantv;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class TvPlayerStatusView extends ConstraintLayout implements TvPlayerListener {
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

        mText = (TextView) findViewById(R.id.text);
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
                    mText.clearAnimation();
                    mText.setVisibility(GONE);
                }
            });
            mText.startAnimation(anim);
        } else {
            mText.clearAnimation();
            mText.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onTvPlayerStateChanged(TvPlayerState state, float progress) {
        switch (state) {
            case RESOLVING:
                mText.setText("Resolving " + Integer.toString((int)(progress * 100)) + "%");
                setVisible(true);
                break;
            case CONNECTING:
                mText.setText("Connecting");
                setVisible(true);
                break;
            case BUFFERING:
                mText.setText("Buffering " + Integer.toString((int)(progress * 100)) + "%");
                setVisible(true);
                break;
            case PLAYING:
                mText.setText("Playing");
                setVisible(false);
                break;
            case PAUSED:
                mText.setText("Paused");
                setVisible(true);
                break;
            case FAILED:
                mText.setText("Failed");
                setVisible(true);
                break;
        }
    }
}
