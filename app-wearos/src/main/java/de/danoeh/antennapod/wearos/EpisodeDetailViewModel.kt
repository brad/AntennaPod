package de.danoeh.antennapod.wearos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.wearos.sync.WearDataRepository
import de.danoeh.antennapod.wearos.sync.WearMessageSender
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EpisodeDetailUiState(
    val item: FeedItem,
    val title: String = "",
    val feedTitle: String = "",
    val position: Int = 0,
    val duration: Int = 0,
    val isCurrentlyPlaying: Boolean = false
)

class EpisodeDetailViewModel(
    application: Application,
    private val episodeId: Long,
    private val initialEpisode: FeedItem?
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(
        EpisodeDetailUiState(
            item = initialEpisode ?: FeedItem().apply { id = episodeId },
            title = initialEpisode?.title ?: "",
            feedTitle = initialEpisode?.feed?.title ?: "",
            position = initialEpisode?.media?.position ?: 0,
            duration = initialEpisode?.media?.duration ?: 0
        )
    )
    val uiState: StateFlow<EpisodeDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            while (isActive) {
                WearMessageSender.send(getApplication(), WearDataPaths.NOW_PLAYING)
                delay(1.seconds)
            }
        }

        viewModelScope.launch {
            WearDataRepository.nowPlaying.collect { nowPlaying ->
                if (nowPlaying == null) return@collect
                val isMatchingEpisode = episodeId == -1L || nowPlaying.item.id == episodeId

                _uiState.update { currentState ->
                    if (isMatchingEpisode) {
                        val newItem = nowPlaying.item
                        val position = newItem.media?.position ?: currentState.position
                        val duration = newItem.media?.duration?.takeIf { it > 0 } ?: currentState.duration
                        currentState.copy(
                            item = newItem,
                            title = newItem.title ?: currentState.title,
                            feedTitle = newItem.feed?.title ?: currentState.feedTitle,
                            position = position,
                            duration = duration,
                            isCurrentlyPlaying = nowPlaying.isPlaying
                        )
                    } else {
                        currentState.copy(isCurrentlyPlaying = false)
                    }
                }
            }
        }

        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isCurrentlyPlaying) {
                    _uiState.update {
                        if (it.position < it.duration || it.duration == 0) {
                            it.copy(position = it.position + 100)
                        } else {
                            it
                        }
                    }
                }
                delay(100.milliseconds)
            }
        }
    }

    fun play() {
        val targetId = if (episodeId != -1L) episodeId else _uiState.value.item.id
        if (targetId != -1L) {
            viewModelScope.launch(Dispatchers.IO) {
                WearMessageSender.send(getApplication(), WearDataPaths.playPath(targetId))
            }
        }
    }

    fun pause() {
        viewModelScope.launch(Dispatchers.IO) { WearMessageSender.send(getApplication(), WearDataPaths.PAUSE) }
    }

    fun skipForward() {
        _uiState.update { it.copy(position = it.position + 10000) }
        viewModelScope.launch(Dispatchers.IO) { WearMessageSender.send(getApplication(), WearDataPaths.SKIP_FORWARD) }
    }

    fun skipBackward() {
        _uiState.update { it.copy(position = Math.max(0, it.position - 10000)) }
        viewModelScope.launch(Dispatchers.IO) { WearMessageSender.send(getApplication(), WearDataPaths.SKIP_BACKWARD) }
    }

    fun openOnPhone() {
        val targetId = if (episodeId != -1L) episodeId else _uiState.value.item.id
        if (targetId != -1L) {
            viewModelScope.launch(Dispatchers.IO) {
                WearMessageSender.send(getApplication(), WearDataPaths.openOnPhonePath(targetId))
            }
        }
    }

    companion object {
        fun factory(episodeId: Long, initialEpisode: FeedItem?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EpisodeDetailViewModel(
                    checkNotNull(get(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY)),
                    episodeId,
                    initialEpisode
                )
            }
        }
    }
}
