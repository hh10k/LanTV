package au.id.blackwell.kurt.lantv;

import android.content.Context;

public class TvPlayerFactory {
    public static final String VLC = "vlc";
    public static final String VITAMIO = "vitamio";

    public TvPlayer create(String playerType, Context context) {
        switch (playerType) {
            case VLC:
                return new VlcTvPlayerView(context);
            case VITAMIO:
                return new VitamioTvPlayerView(context);
            default:
                return null;
        }
    }
}
