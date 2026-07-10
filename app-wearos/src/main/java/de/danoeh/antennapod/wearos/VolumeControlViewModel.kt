package de.danoeh.antennapod.wearos

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.wearos.sync.WearDataRepository
import de.danoeh.antennapod.wearos.sync.WearMessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VolumeControlViewModel(application: Application) : AndroidViewModel(application) {
    private val _volumeState = MutableStateFlow(VolumeUiState())
    val volumeState: StateFlow<VolumeUiState> = _volumeState.asStateFlow()

    init {
        viewModelScope.launch {
            WearMessageSender.send(getApplication(), WearDataPaths.NOW_PLAYING)
        }

        viewModelScope.launch {
            WearDataRepository.nowPlaying.collect { nowPlaying ->
                if (nowPlaying != null) {
                    _volumeState.update {
                        it.copy(
                            volume = nowPlaying.volume,
                            maxVolume = nowPlaying.maxVolume,
                            outputDevice = nowPlaying.outputDevice
                        )
                    }
                }
            }
        }
    }

    fun volumeUp() {
        _volumeState.update { it.copy(volume = (it.volume + 1).coerceAtMost(it.maxVolume)) }
        viewModelScope.launch(Dispatchers.IO) {
            WearMessageSender.send(getApplication(), WearDataPaths.VOLUME_UP)
        }
    }

    fun volumeDown() {
        _volumeState.update { it.copy(volume = (it.volume - 1).coerceAtLeast(0)) }
        viewModelScope.launch(Dispatchers.IO) {
            WearMessageSender.send(getApplication(), WearDataPaths.VOLUME_DOWN)
        }
    }

    fun switchOutput(context: Context) {
        try {
            val intent = Intent("com.google.android.wearable.action.LAUNCH_OUTPUT_SWITCHER")
            val packageName = context.packageName
            intent.putExtra("com.google.android.wearable.extra.EXTRA_PACKAGE_NAME", packageName)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

data class VolumeUiState(
    val volume: Int = 0,
    val maxVolume: Int = 15,
    val outputDevice: String = ""
)
