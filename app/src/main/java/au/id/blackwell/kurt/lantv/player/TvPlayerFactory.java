package au.id.blackwell.kurt.lantv.player;

import android.content.Context;

public class TvPlayerFactory {
    public static final String ANDROID = "android";
    public static final String IJK = "ijk";
    public static final String VITAMIO = "vitamio";
    public static final String VLC = "vlc";

    public TvPlayer create(String playerType, Context context) {
        switch (playerType) {
            case ANDROID:
                return new AndroidMediaPlayerView(context);
            case IJK:
                return new IjkMediaPlayerView(context);
            case VITAMIO:
                return new VitamioMediaPlayerView(context);
            case VLC:
                return new VlcTvPlayerView(context);
            default:
                return null;
        }
    }
}
