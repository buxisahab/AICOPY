package buximusic

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "BuxiMusic (Desktop Preview)") {
        MaterialTheme {
            Surface(tonalElevation = 2.dp) {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
        Text("Hello from Compose Desktop!")
        Spacer(Modifier.height(8.dp))
        Text("This is a minimal Windows entry point for the BuxiMusic desktop module.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = { /* TODO: Wire up to shared logic or features */ }) {
            Text("Play")
        }
    }
}
