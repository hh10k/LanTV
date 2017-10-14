package au.id.blackwell.kurt.lantv.player;

import android.view.SurfaceHolder;

import java.io.IOException;

import au.id.blackwell.kurt.lantv.MediaDetails;

interface TvMediaPlayer {
    boolean isPlaying();
    void setListener(TvMediaPlayerListener listener);
    void setMedia(MediaDetails details) throws IOException;
    void setScreenOnWhilePlaying(boolean onWhilePlaying);
    void setDisplay(SurfaceHolder holder);
    void prepareAsync();
    void release();
    void start();
    void pause();
}
