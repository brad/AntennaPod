package de.danoeh.antennapod.wearos

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun AntennaPodTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}
