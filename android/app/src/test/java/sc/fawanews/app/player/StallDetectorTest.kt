package sc.fawanews.app.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class StallDetectorTest {
    private val ready = Player.STATE_READY
    private val buffering = Player.STATE_BUFFERING

    @Test
    fun picturesThatKeepChangingAreFine() {
        val detector = StallDetector()
        for (second in 0..30 step 2) {
            assertEquals(Stall.NONE, detector.observe(second * 1_000L, true, ready, second * 25))
        }
    }

    @Test
    fun frameCountThatStopsMovingIsAFrozenPicture() {
        val detector = StallDetector()
        assertEquals(Stall.NONE, detector.observe(0, true, ready, 50))
        assertEquals(Stall.NONE, detector.observe(2_000, true, ready, 50))
        assertEquals(Stall.NONE, detector.observe(6_000, true, ready, 50))
        assertEquals(Stall.FROZEN_PICTURE, detector.observe(8_000, true, ready, 50))
        assertEquals(Stall.NONE, detector.observe(10_000, true, ready, 50))
    }

    @Test
    fun pausedOrAudioOnlyNeverCountsAsFrozen() {
        val detector = StallDetector()
        for (second in 0..60 step 2) {
            assertEquals(Stall.NONE, detector.observe(second * 1_000L, false, ready, 50))
        }
        for (second in 0..60 step 2) {
            assertEquals(Stall.NONE, detector.observe(second * 1_000L, true, ready, null))
        }
    }

    @Test
    fun bufferingThatNeverEndsIsReported() {
        val detector = StallDetector()
        assertEquals(Stall.NONE, detector.observe(0, true, buffering, null))
        assertEquals(Stall.NONE, detector.observe(20_000, true, buffering, null))
        assertEquals(Stall.ENDLESS_BUFFERING, detector.observe(25_000, true, buffering, null))
    }

    @Test
    fun shortBufferingResetsTheClock() {
        val detector = StallDetector()
        assertEquals(Stall.NONE, detector.observe(0, true, buffering, null))
        assertEquals(Stall.NONE, detector.observe(20_000, true, ready, 10))
        assertEquals(Stall.NONE, detector.observe(22_000, true, buffering, 10))
        assertEquals(Stall.NONE, detector.observe(40_000, true, buffering, 10))
    }
}
