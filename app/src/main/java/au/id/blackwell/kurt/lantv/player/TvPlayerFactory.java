package au.id.blackwell.kurt.lantv.player;

import android.content.Context;

public class TvPlayerFactory {
    public static final String ANDROID = "android";
    public static final String IJK = "ijk";

    public TvPlayer create(String playerType, Context context) {
        switch (playerType) {
            case ANDROID:
                return new AndroidMediaPlayerView(context);
            case IJK:
                return new IjkMediaPlayerView(context);
            default:
                return null;
        }
    }
}
