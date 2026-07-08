package de.danoeh.antennapod.wearos
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import coil.compose.AsyncImage
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.ui.common.R as CommonR
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
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

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
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            key(uiState.item.id, lifecycleState == Lifecycle.State.RESUMED) {
                Text(
                    text = uiState.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Text(
                    text = uiState.feedTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSkipBackward,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(CommonR.drawable.ic_fast_rewind),
                        contentDescription = stringResource(CommonR.string.rewind_label),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = {
                            if (uiState.duration > 0) uiState.position.toFloat() / uiState.duration else 0f
                        },
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 2.dp,
                        colors = ProgressIndicatorDefaults.colors(indicatorColor = Color(0xFF00BFFF))
                    )
                    IconButton(
                        onClick = if (uiState.isCurrentlyPlaying) onPause else onPlay,
                        modifier = Modifier.size(48.dp)
                    ) {
                        val iconRes = if (uiState.isCurrentlyPlaying) {
                            CommonR.drawable.ic_pause_black
                        } else {
                            CommonR.drawable.ic_play_48dp_black
                        }
                        val labelRes = if (uiState.isCurrentlyPlaying) {
                            CommonR.string.pause_label
                        } else {
                            CommonR.string.play_label
                        }
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(iconRes),
                            contentDescription = stringResource(labelRes),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = onSkipForward,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(CommonR.drawable.ic_fast_forward),
                        contentDescription = stringResource(CommonR.string.fast_forward_label),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        context.startActivity(Intent(context, VolumeControlActivity::class.java))
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(CommonR.drawable.ic_volume_adaption),
                        contentDescription = "Volume",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { showBottomSheet = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(CommonR.drawable.dots_vertical),
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (showBottomSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(stringResource(CommonR.string.close_label))
                    }
                }
            }
        }
    }
}
