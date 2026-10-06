package sc.fawanews.app.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Many site streams keep only ~12 seconds of video (3 short segments). A short stall drops the
 * player behind the live window, or a segment is deleted before it loads. Jump back to the live
 * edge a few times before treating the link as dead.
 */
@UnstableApi
class LiveRecovery(private val player: ExoPlayer) {
    private val handler = Handler(Looper.getMainLooper())
    private var attempts = 0
    private var windowStartMs = 0L

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        attempts = 0
        windowStartMs = 0L
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
    }

    fun tryRecover(error: PlaybackException): Boolean {
        Log.w(TAG, "Playback error ${error.errorCodeName}", error)
        if (error.errorCode !in recoverableCodes) return false
        val now = SystemClock.elapsedRealtime()
        if (now - windowStartMs > WINDOW_MS) {
            attempts = 0
            windowStartMs = now
        }
        attempts += 1
        if (attempts > MAX_ATTEMPTS) return false
        val delayMs = if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            0L
        } else {
            attempts * 1_000L
        }
        handler.postDelayed({
            player.seekToDefaultPosition()
            player.prepare()
            player.playWhenReady = true
        }, delayMs)
        return true
    }

    private companion object {
        const val TAG = "FawaPlayer"
        const val WINDOW_MS = 45_000L
        const val MAX_ATTEMPTS = 4

        val recoverableCodes = setOf(
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
        )
    }
}
