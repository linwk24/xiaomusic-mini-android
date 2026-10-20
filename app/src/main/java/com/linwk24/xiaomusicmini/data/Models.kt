package com.linwk24.xiaomusicmini.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusDto(
    val ok: Boolean = false,
    val error: String? = null,
    val device: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("cur_name") val curName: String? = null,
    val playing: Boolean = false,
    @SerialName("cur_list") val curList: String? = null,
    val mode: String? = null,
    @SerialName("mode_key") val modeKey: String? = null,
    @SerialName("play_type") val playType: Int? = null,
    val volume: Int? = null,
    val total: Int = 0,
    val lists: List<String> = emptyList(),
)

@Serializable
data class SongDto(
    val name: String = "",
    val url: String = "",
)

@Serializable
data class SongsDto(
    val ok: Boolean = false,
    val error: String? = null,
    val list: String? = null,
    val items: List<SongDto> = emptyList(),
    val songs: List<String> = emptyList(),
)

@Serializable
data class DeviceDto(
    val name: String = "",
    val did: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    val hardware: String? = null,
    val current: Boolean = false,
)

@Serializable
data class DevicesDto(
    val ok: Boolean = false,
    val error: String? = null,
    val devices: List<DeviceDto> = emptyList(),
)

@Serializable
data class CmdRequest(
    val cmd: String,
    val arg: String = "",
    val list: String = "",
)

@Serializable
data class CmdResponse(
    val ok: Boolean = false,
    val cmd: String? = null,
    val error: String? = null,
)

enum class OutputMode { PHONE, SPEAKER }
