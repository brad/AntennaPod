package de.danoeh.antennapod.wearos

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.wearos.sync.WearDataRepository
import de.danoeh.antennapod.wearos.sync.WearMessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VolumeControlViewModel(application: Application) : AndroidViewModel(application) {
    val volumeState: StateFlow<VolumeUiState> = WearDataRepository.nowPlaying.map { nowPlaying ->
        VolumeUiState(
            volume = nowPlaying?.volume ?: 0,
            maxVolume = nowPlaying?.maxVolume ?: 15,
            outputDevice = nowPlaying?.outputDevice ?: ""
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VolumeUiState()
    )

    fun volumeUp() {
        viewModelScope.launch(Dispatchers.IO) {
            WearMessageSender.send(getApplication(), WearDataPaths.VOLUME_UP)
        }
    }

    fun volumeDown() {
        viewModelScope.launch(Dispatchers.IO) {
            WearMessageSender.send(getApplication(), WearDataPaths.VOLUME_DOWN)
        }
    }

    fun switchOutput() {
        val intent = Intent("com.google.android.wearable.action.LAUNCH_OUTPUT_SWITCHER")
        val packageName = getApplication<Application>().packageName
        intent.putExtra("com.google.android.wearable.extra.EXTRA_PACKAGE_NAME", packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }
}

data class VolumeUiState(
    val volume: Int = 0,
    val maxVolume: Int = 15,
    val outputDevice: String = ""
)
