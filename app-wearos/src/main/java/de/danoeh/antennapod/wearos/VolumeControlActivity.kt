package de.danoeh.antennapod.wearos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.Text
import de.danoeh.antennapod.ui.common.R as CommonR

class VolumeControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[VolumeControlViewModel::class.java]
        setContent {
            AntennaPodTheme {
                VolumeControlScreen(viewModel)
            }
        }
    }
}

@Composable
fun VolumeControlScreen(viewModel: VolumeControlViewModel) {
    val uiState by viewModel.volumeState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent {
                if (it.verticalScrollPixels > 0) {
                    viewModel.volumeUp()
                } else {
                    viewModel.volumeDown()
                }
                true
            }
            .focusRequester(focusRequester),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = {
                if (uiState.maxVolume > 0) uiState.volume.toFloat() / uiState.maxVolume else 0f
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            strokeWidth = 4.dp
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = { viewModel.volumeUp() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(CommonR.drawable.ic_add),
                    contentDescription = stringResource(CommonR.string.volume_louder_label),
                    modifier = Modifier.size(24.dp)
                )
            }

            Button(
                onClick = { viewModel.switchOutput() }
            ) {
                Text(uiState.outputDevice.ifEmpty { stringResource(CommonR.string.output_switcher_label) })
            }

            IconButton(
                onClick = { viewModel.volumeDown() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(CommonR.drawable.ic_minus),
                    contentDescription = stringResource(CommonR.string.volume_quieter_label),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
