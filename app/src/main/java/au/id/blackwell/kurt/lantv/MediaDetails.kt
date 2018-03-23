package au.id.blackwell.kurt.lantv

import android.net.Uri

class MediaDetails @JvmOverloads constructor(val uri: Uri, // TODO: What does VLC do with this?
                                             val userAgentName: String = DEFAULT_USER_AGENT_NAME, val httpUserAgent: String = DEFAULT_HTTP_USER_AGENT) {
    companion object {
        val DEFAULT_USER_AGENT_NAME = "LanTV"
        val DEFAULT_HTTP_USER_AGENT = "LanTV"
    }
}
