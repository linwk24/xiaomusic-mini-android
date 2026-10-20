package com.linwk24.xiaomusicmini.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linwk24.xiaomusicmini.MainViewModel
import com.linwk24.xiaomusicmini.UiState
import com.linwk24.xiaomusicmini.data.DeviceDto
import com.linwk24.xiaomusicmini.data.MODE_LABELS
import com.linwk24.xiaomusicmini.data.OutputMode
import com.linwk24.xiaomusicmini.data.SongDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(vm: MainViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.startPolling()
                Lifecycle.Event.ON_STOP -> vm.stopPolling()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("小爱音乐 Mini", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (ui.connected) "已连接 ${ui.serverUrl}" else "未连接 · 点右上角设置",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ui.connected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "设置")
                    }
                },
            )
        },
    ) { padding ->
        if (ui.loading && ui.songs.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val filtered = remember(ui.songs, ui.search) {
                val query = ui.search.trim()
                if (query.isEmpty()) ui.songs
                else ui.songs.filter { it.name.contains(query, ignoreCase = true) }
            }
            val title = if (ui.output == OutputMode.PHONE) ui.phoneTitle else ui.speakerTitle
            val playing = if (ui.output == OutputMode.PHONE) ui.phonePlaying else ui.speakerPlaying

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (ui.busy) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                }
                ui.error?.let { message ->
                    item { ErrorCard(message) { vm.clearError() } }
                }
                item {
                    PlayerCard(
                        ui = ui,
                        title = title,
                        playing = playing,
                        onOutput = vm::setOutput,
                    )
                }
                item { TransportRow(playing, vm) }
                item { VolumeRow(ui, vm) }
                item { ModeRow(ui, vm) }
                item { DeviceRow(ui, vm) }
                item { ListsRow(ui, vm) }
                item { SearchRow(ui, vm) }
                if (filtered.isEmpty()) {
                    item { EmptySongs(ui) }
                } else {
                    items(filtered) { song ->
                        SongRow(
                            song = song,
                            current = song.name == title && playing,
                            onPlay = vm::playSong,
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            initial = ui.serverUrl,
            onDismiss = { showSettings = false },
            onSave = { url -> vm.saveServer(url); showSettings = false },
            onTest = vm::testServer,
        )
    }
}

@Composable
private fun PlayerCard(
    ui: UiState,
    title: String?,
    playing: Boolean,
    onOutput: (OutputMode) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(playing)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title ?: "未在播放",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag("歌单 ${ui.curList ?: "-"}")
                        Tag("模式 ${ui.mode ?: "-"}")
                        Tag("共 ${ui.total} 首")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${ui.deviceName ?: "未知设备"} · ${if (playing) "播放中" else "已暂停/停止"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = ui.output == OutputMode.PHONE,
                    onClick = { onOutput(OutputMode.PHONE) },
                    label = { Text("手机播放") },
                    leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, null, Modifier.size(18.dp)) },
                )
                FilterChip(
                    selected = ui.output == OutputMode.SPEAKER,
                    onClick = { onOutput(OutputMode.SPEAKER) },
                    label = { Text("音箱播放") },
                    leadingIcon = { Icon(Icons.Rounded.Speaker, null, Modifier.size(18.dp)) },
                )
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Artwork(playing: Boolean) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF7C8CFF), Color(0xFFB06CFF)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (playing) {
            Equalizer()
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun Equalizer() {
    val transition = rememberInfiniteTransition(label = "equalizer")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(4) { index ->
            val barHeight by transition.animateFloat(
                initialValue = 10f,
                targetValue = 30f,
                animationSpec = infiniteRepeatable(
                    animation = tween(420 + index * 90, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar$index",
            )
            Box(
                Modifier
                    .width(6.dp)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun TransportRow(playing: Boolean, vm: MainViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = vm::skipPrevious) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(18.dp))
        FilledIconButton(onClick = vm::toggle, modifier = Modifier.size(68.dp)) {
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "暂停" else "播放",
                modifier = Modifier.size(38.dp),
            )
        }
        Spacer(Modifier.width(18.dp))
        IconButton(onClick = vm::skipNext) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "下一首", modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(10.dp))
        IconButton(onClick = vm::stop) {
            Icon(Icons.Rounded.Stop, contentDescription = "停止", modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun VolumeRow(ui: UiState, vm: MainViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(22.dp))
        Slider(
            value = ui.volume.toFloat(),
            onValueChange = { vm.setVolume(it.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
        Text("${ui.volume}%", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ModeRow(ui: UiState, vm: MainViewModel) {
    val current = if (ui.output == OutputMode.PHONE) ui.phoneMode else ui.modeKey
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "播放模式",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MODE_LABELS.forEach { (key, label) ->
                FilterChip(
                    selected = key == current,
                    onClick = { vm.setMode(key) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(ui: UiState, vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Devices, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = ui.deviceName ?: "选择音箱",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ui.devices.forEach { device: DeviceDto ->
                    DropdownMenuItem(
                        text = { Text(device.name + if (device.current) "（当前）" else "") },
                        onClick = {
                            vm.selectDevice(device.did ?: device.deviceId ?: "")
                            expanded = false
                        },
                    )
                }
            }
        }
        IconButton(onClick = vm::rescan) {
            Icon(Icons.Rounded.Refresh, contentDescription = "刷新歌单")
        }
    }
}

@Composable
private fun ListsRow(ui: UiState, vm: MainViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ui.lists.forEach { name ->
            FilterChip(
                selected = name == ui.selectedList,
                onClick = { vm.selectList(name) },
                label = { Text(name) },
            )
        }
    }
}

@Composable
private fun SearchRow(ui: UiState, vm: MainViewModel) {
    OutlinedTextField(
        value = ui.search,
        onValueChange = vm::setSearch,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        placeholder = { Text("搜索歌名，点歌曲播放") },
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun SongRow(song: SongDto, current: Boolean, onPlay: (SongDto) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPlay(song) },
        color = if (current) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (current) Icons.Rounded.VolumeUp else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (current) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = song.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (current) {
                Text(
                    text = "正在播放",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EmptySongs(ui: UiState) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (ui.search.isNotBlank()) "没有匹配的歌曲" else "当前歌单为空",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorCard(message: String, onClose: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭")
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onTest: (String, (Boolean, String) -> Unit) -> Unit,
) {
    var url by remember { mutableStateOf(initial) }
    var message by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
        title = { Text("服务器设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; message = null },
                    label = { Text("服务地址") },
                    placeholder = { Text("http://192.168.5.1:8091") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "手机与路由器需在同一局域网；后端默认端口 8091。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                message?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (text.startsWith("连接成功")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                }
                if (testing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(url) }) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        testing = true
                        onTest(url) { _, result -> message = result; testing = false }
                    },
                    enabled = !testing,
                ) {
                    Text("测试")
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        },
    )
}
