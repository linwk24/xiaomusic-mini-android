# 小爱音乐 Mini · Android

小爱音乐 Mini（OpenWrt / iStoreOS 上的 Python 后端）的 Android 客户端。

两种播放方式：

- **手机播放**：手机通过 ExoPlayer 直接播放后端 `/music/...` 音频，支持后台播放、通知栏/锁屏控制、蓝牙耳机。
- **音箱播放**：手机作为遥控器，控制小爱音箱播放、音量、歌单和设备。

## 功能

- 服务器地址可配置，支持连接测试（默认 `http://192.168.5.1:8091`）
- 歌单切换、歌曲搜索、点歌播放
- 播放模式：全部循环 / 单曲循环 / 随机播放 / 顺序播放 / 单曲播放（手机端单曲播放只播当前曲）
- 播放/暂停、上一首/下一首、停止
- 音量滑杆（手机模式调本地音量，音箱模式调音箱音量）
- 当前曲目/歌单/模式/总曲目展示
- 手机 / 音箱输出切换（切换时自动停止另一路，避免双响）
- Media3 `MediaSessionService`：后台播放、通知栏、锁屏、耳机按键
- Material 3，深色/浅色自适应

## 构建

要求：JDK 17、Android SDK 35（Android Studio Ladybug 或更新）。

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

也可以直接推到 GitHub，Actions 会自动构建并上传 debug APK（见 `.github/workflows/android.yml`）。

## 使用

1. 手机和路由器连接同一局域网（或通过 VPN 回家）。
2. 打开 App → 右上角设置 → 填 `http://192.168.5.1:8091` → 测试 → 保存。
3. 选择歌单/歌曲：
   - 点「手机播放」：手机出声，可锁屏/后台；
   - 点「音箱播放」：由小爱音箱出声，手机只做遥控。
4. 顶部可切换音箱；音量滑杆实时调整。

> 后端接口是明文 HTTP，仅建议在局域网/VPN 内使用；不要直接暴露到公网。

## 后端接口

| 接口 | 说明 |
|---|---|
| `GET /api/status` | 设备、当前曲目、`playing`、音量、歌单、模式 |
| `GET /api/songs?list=` | 歌单歌曲（`/music/...`） |
| `GET /api/devices` | 音箱列表 |
| `POST /api/cmd` | `{cmd,arg,list}`：play/toggle/prev/next/stop/volume/device/rescan |
| `GET /music/<path>` | 音频流（支持 Range） |

## Roadmap

- 可选 API Token（后端 + App 同步）
- mDNS 自动发现服务器
- 离线下载缓存、封面、定时关闭、睡眠定时
- Android Auto / 桌面小组件

## License

MIT
