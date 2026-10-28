package com.buxi.music.dsp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class DemoDSPActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DSPDemoScreen()
                }
            }
        }
    }
}

@Composable
private fun DSPDemoScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // val dsp = remember { UniversalDSPManager.getInstance() }
    var sessionId by remember { mutableStateOf<Int?>(null) }
    var diagnostics by remember { mutableStateOf("DSP Disabled - Using Session-Independent DSP from mussifly") }

    /*
    LaunchedEffect(Unit) {
        dsp.init(ctx, PlayerType.EXOPLAYER, sessionId)
    }
    */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Universal DSP Demo (DEPRECATED)", style = MaterialTheme.typography.titleLarge)
        Text(text = "This demo used the old Android AudioEffect API. Please use the new AudioEffectsScreen.")

        /*
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { dsp.enableEffect("equalizer") }) { Text("Enable EQ") }
            Button(onClick = { dsp.disableEffect("equalizer") }) { Text("Disable EQ") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { dsp.setPreset("EDM") }) { Text("Preset EDM") }
            Button(onClick = { dsp.setPreset("LoFi") }) { Text("Preset LoFi") }
        }

        Button(onClick = {
            diagnostics = dsp.getDiagnostics(ctx).toString(2)
        }) { Text("Get Diagnostics") }
        */

        Text(text = diagnostics, modifier = Modifier.weight(1f))
    }
}


