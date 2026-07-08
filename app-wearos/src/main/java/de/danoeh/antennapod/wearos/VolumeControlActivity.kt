package de.danoeh.antennapod.wearos

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.Text
import de.danoeh.antennapod.ui.common.R as CommonR

class VolumeControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AntennaPodTheme {
                VolumeControlScreen(
                    onLaunchOutputSwitcher = {
                        val intent = Intent("com.google.android.wearable.action.LAUNCH_OUTPUT_SWITCHER")
                        intent.putExtra("com.google.android.wearable.extra.EXTRA_PACKAGE_NAME", packageName)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun VolumeControlScreen(onLaunchOutputSwitcher: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    var volume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent {
                val delta = if (it.verticalScrollPixels > 0) 1 else -1
                val newVolume = (volume + delta).coerceIn(0, maxVolume)
                if (newVolume != volume) {
                    volume = newVolume
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                }
                true
            }
            .focusRequester(focusRequester),
        contentAlignment = Alignment.Center
    ) {
        Stepper(
            value = volume,
            onValueChange = { newValue ->
                volume = newValue
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            },
            valueProgression = 0..maxVolume,
            increaseIcon = { Icon(painterResource(CommonR.drawable.ic_add), contentDescription = "Increase") },
            decreaseIcon = { Icon(painterResource(CommonR.drawable.ic_minus), contentDescription = "Decrease") }
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.ic_volume_adaption),
                contentDescription = stringResource(CommonR.string.volume_label),
                modifier = Modifier.size(24.dp)
            )
        }

        Button(
            onClick = onLaunchOutputSwitcher,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Text(stringResource(CommonR.string.output_switcher_label))
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
