package sc.fawanews.app

import android.app.Application
import sc.fawanews.app.data.FawaRepository
import sc.fawanews.app.data.ScoreRepository

class FawaNewsApp : Application() {
    val repository: FawaRepository by lazy { FawaRepository() }
    val scoreRepository: ScoreRepository by lazy { ScoreRepository() }

    override fun onCreate() {
        super.onCreate()
        applyLauncherVisibilityForDevice()
    }
}
