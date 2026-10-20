package com.linwk24.xiaomusicmini

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.linwk24.xiaomusicmini.data.ApiException
import com.linwk24.xiaomusicmini.data.DeviceDto
import com.linwk24.xiaomusicmini.data.OutputMode
import com.linwk24.xiaomusicmini.data.SettingsRepository
import com.linwk24.xiaomusicmini.data.SongDto
import com.linwk24.xiaomusicmini.data.StatusDto
import com.linwk24.xiaomusicmini.data.XiaomiMiniApi
import com.linwk24.xiaomusicmini.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiState(
    val serverUrl: String = "",
    val output: OutputMode = OutputMode.SPEAKER,
    val connected: Boolean = false,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val deviceName: String? = null,
    val speakerTitle: String? = null,
    val speakerPlaying: Boolean = false,
    val curList: String? = null,
    val mode: String? = null,
    val total: Int = 0,
    val volume: Int = 0,
    val lists: List<String> = listOf("全部"),
    val selectedList: String = "全部",
    val songs: List<SongDto> = emptyList(),
    val devices: List<DeviceDto> = emptyList(),
    val phoneTitle: String? = null,
    val phonePlaying: Boolean = false,
    val search: String = "",
)

@OptIn(UnstableApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)
    private var api = XiaomiMiniApi(SettingsRepository.DEFAULT_SERVER)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pollJob: Job? = null
    private var volumeJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _ui.update { it.copy(phonePlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _ui.update { it.copy(phoneTitle = mediaItem?.mediaMetadata?.title?.toString()) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _ui.update { it.copy(phonePlaying = false) }
            }
        }
    }

    init {
        viewModelScope.launch {
            val url = settings.serverUrl.first()
            val mode = settings.outputMode.first()
            api = XiaomiMiniApi(url)
            _ui.update { it.copy(serverUrl = url, output = mode) }
            connectController()
            refreshAll()
        }
        viewModelScope.launch {
            settings.outputMode.collect { mode ->
                _ui.update { it.copy(output = mode) }
            }
        }
    }

    private fun connectController() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                controller = future.get().also { c ->
                    c.addListener(playerListener)
                    c.repeatMode = Player.REPEAT_MODE_ALL
                }
                _ui.update { it.copy(phonePlaying = controller?.isPlaying == true) }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "播放器初始化失败：${e.message}") }
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                refreshStatus()
                delay(2000)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun refreshAll() {
        _ui.update { it.copy(loading = true, error = null) }
        try {
            val status = api.status()
            applyStatus(status)
            val devices = runCatching { api.devices().devices }.getOrDefault(emptyList())
            val lists = status.lists.ifEmpty { listOf("全部") }
            var selected = _ui.value.selectedList
            if (selected !in lists) {
                selected = status.curList?.takeIf { it in lists } ?: lists.first()
            }
            _ui.update {
                it.copy(
                    devices = devices,
                    lists = lists,
                    selectedList = selected,
                    loading = false,
                    connected = true,
                    error = null,
                )
            }
            loadSongs(selected)
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, connected = false, error = friendly(e)) }
        }
    }

    private suspend fun refreshStatus() {
        try {
            val status = api.status()
            applyStatus(status)
            val lists = _ui.value.lists
            if (lists.isNotEmpty() && _ui.value.selectedList !in lists) {
                val selected = status.curList?.takeIf { it in lists } ?: lists.first()
                _ui.update { it.copy(selectedList = selected) }
                loadSongs(selected)
            }
        } catch (e: Exception) {
            _ui.update { it.copy(connected = false, error = friendly(e)) }
        }
    }

    private fun applyStatus(status: StatusDto) {
        _ui.update { s ->
            s.copy(
                connected = true,
                error = null,
                deviceName = status.deviceName ?: s.deviceName,
                speakerTitle = status.curName,
                speakerPlaying = status.playing,
                curList = status.curList,
                mode = status.mode,
                total = status.total,
                volume = status.volume ?: s.volume,
                lists = if (status.lists.isNotEmpty()) status.lists else s.lists,
            )
        }
    }

    private suspend fun loadSongs(list: String) {
        try {
            val songs = api.songs(list)
            _ui.update { it.copy(songs = songs.items) }
        } catch (e: Exception) {
            _ui.update { it.copy(error = friendly(e)) }
        }
    }

    fun playSong(song: SongDto) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            try {
                if (_ui.value.output == OutputMode.PHONE) {
                    runCatching { api.cmd("stop") }
                    val player = controller ?: error("播放器未就绪，请稍后重试")
                    val songs = _ui.value.songs
                    if (songs.isEmpty()) error("当前歌单为空")
                    val items = songs.map { s ->
                        MediaItem.Builder()
                            .setMediaId(s.name)
                            .setUri(api.absolute(s.url))
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(s.name)
                                    .setArtist("小爱音乐 Mini")
                                    .build()
                            )
                            .build()
                    }
                    val index = songs.indexOfFirst { it.name == song.name }.coerceAtLeast(0)
                    player.setMediaItems(items, index, 0L)
                    player.prepare()
                    player.play()
                    _ui.update { it.copy(phoneTitle = song.name) }
                } else {
                    api.cmd("play", song.name, _ui.value.selectedList)
                }
                refreshStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = friendly(e)) }
            } finally {
                _ui.update { it.copy(busy = false) }
            }
        }
    }

    fun toggle() {
        viewModelScope.launch {
            try {
                if (_ui.value.output == OutputMode.PHONE) {
                    val player = controller ?: error("播放器未就绪")
                    if (player.isPlaying) {
                        player.pause()
                    } else if (player.mediaItemCount == 0) {
                        val first = _ui.value.songs.firstOrNull() ?: error("当前歌单为空")
                        playSong(first)
                    } else {
                        player.play()
                    }
                } else {
                    api.cmd("toggle")
                    refreshStatus()
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = friendly(e)) }
            }
        }
    }

    fun skipNext() {
        viewModelScope.launch {
            try {
                if (_ui.value.output == OutputMode.PHONE) {
                    controller?.let { player ->
                        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                        else player.seekTo(0, 0L)
                    }
                } else {
                    api.cmd("next")
                    refreshStatus()
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = friendly(e)) }
            }
        }
    }

    fun skipPrevious() {
        viewModelScope.launch {
            try {
                if (_ui.value.output == OutputMode.PHONE) {
                    controller?.let { player ->
                        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                        else player.seekTo((player.mediaItemCount - 1).coerceAtLeast(0), 0L)
                    }
                } else {
                    api.cmd("prev")
                    refreshStatus()
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = friendly(e)) }
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            try {
                if (_ui.value.output == OutputMode.PHONE) {
                    controller?.stop()
                    _ui.update { it.copy(phonePlaying = false) }
                } else {
                    api.cmd("stop")
                    refreshStatus()
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = friendly(e)) }
            }
        }
    }

    fun setVolume(value: Int) {
        val volume = value.coerceIn(0, 100)
        _ui.update { it.copy(volume = volume) }
        volumeJob?.cancel()
        volumeJob = viewModelScope.launch {
            delay(250)
            try {
                if (_ui.value.output == OutputMode.PHONE) {
                    controller?.volume = volume / 100f
                } else {
                    api.cmd("volume", volume.toString())
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = friendly(e)) }
            }
        }
    }

    fun selectDevice(id: String) {
        if (id.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            try {
                api.cmd("device", id)
                refreshAll()
            } catch (e: Exception) {
                _ui.update { it.copy(error = friendly(e)) }
            } finally {
                _ui.update { it.copy(busy = false) }
            }
        }
    }

    fun selectList(name: String) {
        if (name == _ui.value.selectedList) return
        _ui.update { it.copy(selectedList = name, songs = emptyList()) }
        viewModelScope.launch { loadSongs(name) }
    }

    fun rescan() {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            try {
                api.cmd("rescan")
                loadSongs(_ui.value.selectedList)
                refreshStatus()
            } catch (e: Exception) {
                _ui.update { it.copy(error = friendly(e)) }
            } finally {
                _ui.update { it.copy(busy = false) }
            }
        }
    }

    fun setOutput(mode: OutputMode) {
        if (mode == _ui.value.output) return
        viewModelScope.launch {
            settings.setOutputMode(mode)
            _ui.update { it.copy(output = mode, error = null) }
            if (mode == OutputMode.SPEAKER) {
                controller?.stop()
                _ui.update { it.copy(phonePlaying = false) }
            } else {
                runCatching { api.cmd("stop") }
            }
        }
    }

    fun saveServer(raw: String) {
        val url = XiaomiMiniApi.normalize(raw)
        if (url.isEmpty()) {
            _ui.update { it.copy(error = "服务地址不能为空") }
            return
        }
        viewModelScope.launch {
            settings.setServerUrl(url)
            api = XiaomiMiniApi(url)
            _ui.update { it.copy(serverUrl = url, connected = false, error = null) }
            refreshAll()
        }
    }

    fun testServer(raw: String, onResult: (Boolean, String) -> Unit) {
        val url = XiaomiMiniApi.normalize(raw)
        viewModelScope.launch {
            try {
                val status = XiaomiMiniApi(url).status()
                onResult(true, "连接成功 · ${status.deviceName ?: "未知设备"} · ${status.total} 首")
            } catch (e: Exception) {
                onResult(false, friendly(e))
            }
        }
    }

    fun setSearch(value: String) {
        _ui.update { it.copy(search = value) }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }

    private fun friendly(e: Exception): String = when (e) {
        is ApiException -> e.message ?: "请求失败"
        else -> e.message ?: "请求失败"
    }

    override fun onCleared() {
        pollJob?.cancel()
        volumeJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }
}
