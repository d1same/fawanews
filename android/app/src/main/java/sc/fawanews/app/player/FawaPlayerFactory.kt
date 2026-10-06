package sc.fawanews.app.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.DefaultHlsExtractorFactory
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import sc.fawanews.app.data.StreamRequestHeaders

@UnstableApi
object FawaPlayerFactory {

    fun create(context: Context, preferTvQuality: Boolean = false): ExoPlayer {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(StreamRequestHeaders.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setDefaultRequestProperties(StreamRequestHeaders.properties)

        val loadErrorPolicy = DefaultLoadErrorHandlingPolicy(LOAD_RETRIES)

        // Re-streamed TS feeds often start mid-GOP or omit access unit delimiters.
        val hlsExtractors = DefaultHlsExtractorFactory(
            DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS,
            true,
        )
        val mediaSourceFactory = StreamMediaSourceFactory(
            hls = HlsMediaSource.Factory(dataSourceFactory)
                .setExtractorFactory(hlsExtractors)
                .setLoadErrorHandlingPolicy(loadErrorPolicy)
                .setAllowChunklessPreparation(true),
            other = DefaultMediaSourceFactory(dataSourceFactory)
                .setLoadErrorHandlingPolicy(loadErrorPolicy),
        )

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

        // Live feeds rarely buffer more than a few segments ahead. Cap bytes so a high-bitrate
        // feed cannot hold 100+ MB of video in memory on low-RAM phones.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                if (preferTvQuality) 20_000 else 15_000,
                if (preferTvQuality) 50_000 else 30_000,
                2_500,
                if (preferTvQuality) 7_500 else 5_000,
            )
            .setTargetBufferBytes(if (preferTvQuality) 64 * 1024 * 1024 else 32 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

        // If a device's hardware decoder refuses a stream, try the next decoder instead of failing.
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
    }

    private const val LOAD_RETRIES = 6
}

@UnstableApi
private class StreamMediaSourceFactory(
    private val hls: HlsMediaSource.Factory,
    private val other: DefaultMediaSourceFactory,
) : MediaSource.Factory {

    override fun setDrmSessionManagerProvider(provider: DrmSessionManagerProvider): MediaSource.Factory {
        hls.setDrmSessionManagerProvider(provider)
        other.setDrmSessionManagerProvider(provider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(policy: LoadErrorHandlingPolicy): MediaSource.Factory {
        hls.setLoadErrorHandlingPolicy(policy)
        other.setLoadErrorHandlingPolicy(policy)
        return this
    }

    override fun getSupportedTypes(): IntArray = other.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val config = mediaItem.localConfiguration
        val path = config?.uri?.path.orEmpty()
        val isHls = config?.mimeType == MimeTypes.APPLICATION_M3U8 || path.endsWith(".m3u8", ignoreCase = true)
        return if (isHls) hls.createMediaSource(mediaItem) else other.createMediaSource(mediaItem)
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
