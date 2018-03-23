package au.id.blackwell.kurt.lantv

import android.net.Uri

internal data class TvChannel(val id: String, val title: String, val playerType: String?, val mediaResolveUri: Uri) {

    val isPlayable: Boolean
        get() = playerType != null

    constructor(id: String, title: String, player: String?, mediaResolveUri: String) : this(id, title, player, Uri.parse(mediaResolveUri)) {}
}
