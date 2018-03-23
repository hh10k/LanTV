package au.id.blackwell.kurt.lantv.player

enum class TvPlayerState {
    NONE,
    RESOLVING,
    CONNECTING,
    BUFFERING,
    PLAYING,
    STOPPED,
    FAILED
}
