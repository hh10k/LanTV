package au.id.blackwell.kurt.lantv;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

public class TvPlayerStatusView extends ConstraintLayout implements TvPlayerListener {
    private TextView mText;

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

    @Override
    public void onTvPlayerStateChanged(TvPlayerState state, float progress) {
        switch (state) {
            case RESOLVING:
                mText.setText("Resolving " + Integer.toString((int)(progress * 100)) + "%");
                break;
            case CONNECTING:
                mText.setText("Connecting");
                break;
            case BUFFERING:
                mText.setText("Buffering " + Integer.toString((int)(progress * 100)) + "%");
                break;
            case PLAYING:
                mText.setText("");
                break;
            case PAUSED:
                mText.setText("Paused");
                break;
            case FAILED:
                mText.setText("Failed");
                break;
        }
    }
}
