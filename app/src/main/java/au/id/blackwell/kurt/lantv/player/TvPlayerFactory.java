package au.id.blackwell.kurt.lantv.player;

import android.content.Context;

public class TvPlayerFactory {
    public static final String ANDROID = "android";
    public static final String VLC = "vlc";
    public static final String VITAMIO = "vitamio";

    public TvPlayer create(String playerType, Context context) {
        switch (playerType) {
            case ANDROID:
                return new AndroidMediaPlayerView(context);
            case VLC:
                return new VlcTvPlayerView(context);
            case VITAMIO:
                return new VitamioMediaPlayerView(context);
            default:
                return null;
        }
    }
}
