package my.shanunid.linespoofer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import my.shanunid.linespoofer.data.AppSettingsStore
import my.shanunid.linespoofer.ui.LineSpooferApp
import my.shanunid.linespoofer.ui.LineSpooferTheme
import my.shanunid.linespoofer.ui.SpoofAppState

class MainActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        @Volatile
        var isHookActivated: Boolean = false

        @JvmStatic
        @Volatile
        var isServiceBound: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettingsStore.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: XposedService) {
                    isServiceBound = true
                }

                override fun onServiceDied(service: XposedService) {
                    isServiceBound = false
                }
            })
        } catch (_: Throwable) {
        }

        try {
            my.shanunid.linespoofer.webui.WebUiServer.start(this)
        } catch (_: Throwable) {
        }

        try {
            val store = my.shanunid.linespoofer.data.AppProfileStore(my.shanunid.linespoofer.data.ConfigFileManager())
            val state = my.shanunid.linespoofer.data.AppProfileSession.getOrLoad(this, store)
            my.shanunid.linespoofer.utils.RootScopeManager.syncInBackground(this, state.assignments.keys)
        } catch (_: Throwable) {
        }

        setContent {
            val appState = remember { SpoofAppState(this) }
            LaunchedEffect(appState) {
                appState.initialize()
            }
            LineSpooferTheme(
                themeMode = appState.themeMode,
                languageMode = appState.languageMode,
                useSystemColor = appState.systemColorsEnabled,
                colorStyle = appState.colorStyle,
            ) {
                LineSpooferApp(appState)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            my.shanunid.linespoofer.webui.WebUiServer.stop()
        } catch (_: Throwable) {
        }
    }

    fun isModuleActivated(): Boolean = isHookActivated || isServiceBound
}
