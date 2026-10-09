package sc.fawanews.app.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import sc.fawanews.app.BuildConfig

enum class Stall { NONE, FROZEN_PICTURE, ENDLESS_BUFFERING }

/**
 * Some TV decoders stop putting out frames without raising an error, so the picture freezes
 * while the player still says it is playing. Buffering that never ends is caught here too.
 */
class StallDetector(
    private val frozenAfterMs: Long = 8_000L,
    private val endlessBufferingAfterMs: Long = 25_000L,
) {
    private var lastFrames: Int? = null
    private var lastProgressMs = -1L
    private var bufferingSinceMs = -1L

    fun reset() {
        lastFrames = null
        lastProgressMs = -1L
        bufferingSinceMs = -1L
    }

    /** [renderedFrames] is null when no video is being decoded (audio-only or not started). */
    fun observe(nowMs: Long, playing: Boolean, state: Int, renderedFrames: Int?): Stall {
        if (!playing || (state != Player.STATE_READY && state != Player.STATE_BUFFERING)) {
            reset()
            return Stall.NONE
        }
        if (state == Player.STATE_BUFFERING) {
            lastProgressMs = nowMs
            if (bufferingSinceMs < 0) bufferingSinceMs = nowMs
            if (nowMs - bufferingSinceMs < endlessBufferingAfterMs) return Stall.NONE
            reset()
            return Stall.ENDLESS_BUFFERING
        }
        bufferingSinceMs = -1L
        if (renderedFrames == null || renderedFrames != lastFrames || lastProgressMs < 0) {
            lastFrames = renderedFrames
            lastProgressMs = nowMs
            return Stall.NONE
        }
        if (nowMs - lastProgressMs < frozenAfterMs) return Stall.NONE
        reset()
        return Stall.FROZEN_PICTURE
    }
}

@UnstableApi
class FreezeWatchdog(
    private val player: ExoPlayer,
    private val onStall: (Stall) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val detector = StallDetector()
    private val tick = object : Runnable {
        override fun run() {
            val frames = player.videoDecoderCounters?.let {
                it.ensureUpdated()
                it.renderedOutputBufferCount
            }
            val stall = detector.observe(
                SystemClock.elapsedRealtime(),
                player.playWhenReady,
                player.playbackState,
                frames,
            )
            if (stall != Stall.NONE) onStall(stall)
            handler.postDelayed(this, CHECK_EVERY_MS)
        }
    }

    fun start() {
        handler.removeCallbacks(tick)
        detector.reset()
        handler.postDelayed(tick, CHECK_EVERY_MS)
    }

    fun stop() {
        handler.removeCallbacks(tick)
    }

    private companion object {
        const val CHECK_EVERY_MS = 2_000L
    }
}

/**
 * Remembers that this device's hardware video decoder froze, so later games start on the
 * software decoder. A new app version tries hardware again.
 */
object SafeDecoding {
    private const val PREFS = "player"
    private const val KEY = "safe_decoding_version"

    fun isOn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, -1) == BuildConfig.VERSION_CODE

    fun turnOn(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY, BuildConfig.VERSION_CODE)
            .apply()
    }
}
