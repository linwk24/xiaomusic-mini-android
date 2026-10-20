package com.linwk24.xiaomusicmini.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiException(message: String) : Exception(message)

class XiaomiMiniApi(rawUrl: String) {

    @Volatile
    var baseUrl: String = normalize(rawUrl)
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun absolute(path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return baseUrl + if (path.startsWith("/")) path else "/$path"
    }

    suspend fun status(): StatusDto {
        val dto = get<StatusDto>("/api/status")
        if (!dto.ok) throw ApiException(dto.error ?: "服务返回异常")
        return dto
    }

    suspend fun songs(list: String): SongsDto {
        val encoded = URLEncoder.encode(list, "UTF-8")
        val dto = get<SongsDto>("/api/songs?list=$encoded")
        if (!dto.ok) throw ApiException(dto.error ?: "加载歌单失败")
        return dto
    }

    suspend fun devices(): DevicesDto {
        val dto = get<DevicesDto>("/api/devices")
        if (!dto.ok) throw ApiException(dto.error ?: "加载设备失败")
        return dto
    }

    suspend fun cmd(cmd: String, arg: String = "", list: String = ""): CmdResponse {
        val body = json.encodeToString(CmdRequest(cmd = cmd, arg = arg, list = list))
            .toRequestBody(jsonType)
        val request = Request.Builder().url(baseUrl + "/api/cmd").post(body).build()
        val text = execute(request)
        val dto = runCatching { json.decodeFromString<CmdResponse>(text) }.getOrElse {
            throw ApiException("响应解析失败：${text.take(200)}")
        }
        if (!dto.ok) throw ApiException(dto.error ?: "操作失败")
        return dto
    }

    private suspend inline fun <reified T> get(path: String): T {
        val text = execute(Request.Builder().url(baseUrl + path).get().build())
        return runCatching { json.decodeFromString<T>(text) }.getOrElse {
            throw ApiException("响应解析失败：${text.take(200)}")
        }
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = runCatching {
                        json.decodeFromString<CmdResponse>(body).error
                    }.getOrNull()
                    throw ApiException(msg ?: "HTTP ${response.code}")
                }
                body
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException(e.message ?: "网络请求失败")
        }
    }

    companion object {
        fun normalize(raw: String): String {
            var value = raw.trim()
            if (value.isEmpty()) return ""
            if (!value.startsWith("http://") && !value.startsWith("https://")) {
                value = "http://$value"
            }
            return value.trimEnd('/')
        }
    }
}
