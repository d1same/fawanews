package sc.fawanews.app.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import sc.fawanews.app.data.StreamRequestHeaders

@UnstableApi
object FawaPlayerFactory {

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

/** Pick the sharpest video track the TV can play. Phone playback stays adaptive. */
@UnstableApi
fun ExoPlayer.preferHighestVideo() {
    var bestGroup: TrackGroup? = null
    var bestIndex = -1
    var bestPixels = -1
    var bestBitrate = -1
    for (group in currentTracks.groups) {
        if (group.type != C.TRACK_TYPE_VIDEO) continue
        for (index in 0 until group.length) {
            if (!group.isTrackSupported(index, false)) continue
            val format = group.getTrackFormat(index)
            val pixels = format.width.coerceAtLeast(0) * format.height.coerceAtLeast(0)
            val bitrate = format.bitrate.coerceAtLeast(0)
            val sharper = pixels > bestPixels || (pixels == bestPixels && bitrate > bestBitrate)
            if (sharper && (pixels > 0 || bitrate > 0)) {
                bestPixels = pixels
                bestBitrate = bitrate
                bestGroup = group.mediaTrackGroup
                bestIndex = index
            }
        }
    }
    val group = bestGroup ?: return
    if (bestIndex < 0) return
    val already = trackSelectionParameters.overrides[group]
    if (already != null && already.trackIndices.size == 1 && already.trackIndices[0] == bestIndex) {
        return
    }
    trackSelectionParameters = trackSelectionParameters
        .buildUpon()
        .setForceHighestSupportedBitrate(true)
        .setOverrideForType(TrackSelectionOverride(group, bestIndex))
        .build()
}
