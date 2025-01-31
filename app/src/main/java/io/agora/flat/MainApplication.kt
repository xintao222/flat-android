package io.agora.flat

import android.app.Application
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp
import io.agora.flat.common.android.DarkModeManager
import io.agora.flat.common.android.LanguageManager
import io.agora.flat.common.upload.UploadManager
import io.agora.flat.di.interfaces.StartupInitializer
import io.agora.flat.util.isApkInDebug
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {
    @Inject
    lateinit var initializerSet: Set<@JvmSuppressWildcards StartupInitializer>

    override fun onCreate() {
        super.onCreate()
        initializerSet.forEach {
            it.init(this)
        }
        UploadManager.init(this)
        LanguageManager.init(this)
        DarkModeManager.init(this)
        WebView.setWebContentsDebuggingEnabled(isApkInDebug())
    }
}