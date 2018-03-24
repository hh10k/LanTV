package au.id.blackwell.kurt.lantv

import au.id.blackwell.kurt.lantv.player.TvPlayerFactory

internal object TvChannelConfiguration {

    val DEFAULT_PLAYER = TvPlayerFactory.IJK
    val TV_CHANNELS = arrayOf(
            TvChannel("cctv1", "CCTV-1 综合", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv1/hd/"),
            TvChannel("cctv2", "CCTV-2 财经", null /* unavailable online */, "cctv://tv.cctv.com/live/cctv2/hd/"),
            TvChannel("cctv3", "CCTV-3 综艺", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv3/hd/"),
            TvChannel("cctv4", "CCTV-4 亚洲", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv4/hd/"),
            TvChannel("cctveurope", "CCTV-4 欧洲", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctveurope/hd/"),
            TvChannel("cctvamerica", "CCTV-4 美洲", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctvamerica/hd/"),
            TvChannel("cctv5", "CCTV-5 体育", null /* unavailable in my region */, "cctv://tv.cctv.com/live/cctv5/hd/"),
            TvChannel("cctv5plus", "CCTV-5+ 体育赛事", null /* unavailable in my region */, "cctv://tv.cctv.com/live/cctv5plus/hd/"),
            TvChannel("cctv6", "CCTV-6 电影", null /* unavailable online */, "cctv://tv.cctv.com/live/cctv6/hd/"),
            TvChannel("cctv7", "CCTV-7 军事农业", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv7/hd/"),
            TvChannel("cctv8", "CCTV-8 电视剧", null /* unavailable online */, "cctv://tv.cctv.com/live/cctv8/hd/"),
            TvChannel("cctvjilu", "CCTV-9 纪录", null /* unavailable online */, "cctv://tv.cctv.com/live/cctvjilu/hd/"),
            TvChannel("cctv10", "CCTV-10 科教", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv10/hd/"),
            TvChannel("cctv11", "CCTV-11 戏曲", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv11/hd/"),
            TvChannel("cctv12", "CCTV-12 社会与法", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv12/hd/"),
            TvChannel("cctv13", "CCTV-13 新闻", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv13/hd/"),
            TvChannel("cctvchild", "CCTV-14 少儿", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctvchild/hd/"),
            TvChannel("cctv15", "CCTV-15 音乐", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv15/hd/"))
    val DEFAULT_TV_CHANNEL_ID = "cctv13"
    val DEFAULT_TV_CHANNEL_INDEX = getTvChannelIndexById(DEFAULT_TV_CHANNEL_ID)

    fun getTvChannelIndexById(id: String): Int {
        var channelIndex = 0
        for (i in TV_CHANNELS.indices) {
            if (TV_CHANNELS[i].id == DEFAULT_TV_CHANNEL_ID) {
                channelIndex = i
                break
            }
        }
        return channelIndex
    }
}