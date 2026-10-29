package my.shanunid.linespoofer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import my.shanunid.linespoofer.data.AppSettingsStore
import my.shanunid.linespoofer.ui.DiagnosticsScreen
import my.shanunid.linespoofer.ui.SpoofAppState
import my.shanunid.linespoofer.ui.LineSpooferTheme

class DiagnosticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettingsStore.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState = remember { SpoofAppState(this) }
            LaunchedEffect(appState) { appState.initialize() }
            LineSpooferTheme {
                DiagnosticsScreen(appState, onBack = ::finish)
            }
        }
    }
}
