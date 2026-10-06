package sc.fawanews.app.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import sc.fawanews.app.data.StreamRequestHeaders

@UnstableApi
object FawaPlayerFactory {

    /** QHD / 2K target for TV (2560×1440). Phone uses adaptive defaults. */
    private const val TV_MAX_WIDTH = 2560
    private const val TV_MAX_HEIGHT = 1440
    private const val TV_MAX_BITRATE = 20_000_000

    fun create(context: Context, preferTvQuality: Boolean = false): ExoPlayer {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(30_000)
            .setDefaultRequestProperties(StreamRequestHeaders.properties)

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .apply {
                    if (preferTvQuality) {
                        setViewportSize(TV_MAX_WIDTH, TV_MAX_HEIGHT, true)
                        setMaxVideoSize(TV_MAX_WIDTH, TV_MAX_HEIGHT)
                        setMaxVideoBitrate(TV_MAX_BITRATE)
                        setForceHighestSupportedBitrate(true)
                    }
                }
                .setExceedVideoConstraintsIfNecessary(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .build()
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                if (preferTvQuality) 20_000 else 15_000,
                if (preferTvQuality) 60_000 else 50_000,
                2_500,
                if (preferTvQuality) 7_500 else 5_000,
            )
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
    }
}
