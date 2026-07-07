package de.danoeh.antennapod.wearos

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil.compose.AsyncImage
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.ui.common.R as CommonR
import de.danoeh.antennapod.ui.notifications.R as NotificationsR
import de.danoeh.antennapod.wearos.composable.ListItem

class EpisodeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val episode = IntentCompat.getSerializableExtra(intent, EXTRA_EPISODE, FeedItem::class.java) ?: run {
            finish()
            return
        }
        val viewModel = ViewModelProvider(this, EpisodeDetailViewModel.factory(episode))
            .get(EpisodeDetailViewModel::class.java)

        setContent {
            AntennaPodTheme {
                val uiState by viewModel.uiState.collectAsState()
                EpisodeDetailScreen(
                    uiState = uiState,
                    onPlay = { viewModel.play() },
                    onPause = { viewModel.pause() },
                    onSkipForward = { viewModel.skipForward() },
                    onSkipBackward = { viewModel.skipBackward() },
                    onOpenOnPhone = { viewModel.openOnPhone() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_EPISODE = "episode"
    }
}

@Composable
fun EpisodeDetailScreen(
    uiState: EpisodeDetailUiState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onOpenOnPhone: () -> Unit
) {
    val item = uiState.item
    val context = androidx.compose.ui.platform.LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (item.imageUrl != null) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.2f),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = item.feed?.title ?: "",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSkipBackward,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(CommonR.drawable.ic_replay),
                        contentDescription = stringResource(CommonR.string.rewind_label),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = if (uiState.isCurrentlyPlaying) onPause else onPlay,
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors()
                ) {
                    Icon(
                        painter = painterResource(
                            if (uiState.isCurrentlyPlaying) {
                                CommonR.drawable.ic_pause_black
                            } else {
                                CommonR.drawable.ic_play_48dp_black
                            }
                        ),
                        contentDescription = stringResource(
                            if (uiState.isCurrentlyPlaying) {
                                CommonR.string.pause_label
                            } else {
                                CommonR.string.play_label
                            }
                        ),
                        modifier = Modifier.fillMaxSize(0.6f),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(
                    onClick = onSkipForward,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(CommonR.drawable.ic_fast_forward),
                        contentDescription = stringResource(CommonR.string.fast_forward_label),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent("com.google.android.wearable.action.LAUNCH_OUTPUT_SWITCHER")
                                .putExtra(
                                    "com.google.android.wearable.extra.PACKAGE_NAME",
                                    context.packageName
                                )
                        )
                    },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors()
                ) {
                    Icon(
                        painter = painterResource(NotificationsR.drawable.ic_notification_stream),
                        contentDescription = "Audio Output",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_SAME,
                            AudioManager.FLAG_SHOW_UI
                        )
                    },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors()
                ) {
                    Icon(
                        painter = painterResource(CommonR.drawable.ic_volume_adaption),
                        contentDescription = "Volume",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        IconButton(
            onClick = { showBottomSheet = true },
            modifier = Modifier.align(Alignment.BottomCenter).size(32.dp)
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.ic_arrow_full_up),
                contentDescription = "Open drawer",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        if (showBottomSheet) {
            Box(
                modifier = Modifier.fillMaxSize().alpha(0.95f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ListItem(
                        text = stringResource(CommonR.string.wearos_open_on_phone),
                        iconRes = CommonR.drawable.ic_phone_black,
                        onClick = {
                            onOpenOnPhone()
                            showBottomSheet = false
                        }
                    )
                    Button(
                        onClick = { showBottomSheet = false },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(stringResource(CommonR.string.close_label))
                    }
                }
            }
        }
    }
}
