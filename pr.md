# PR: 添加音乐卡片消息支持

## 功能概述

本 PR 为 Yogurt 添加了音乐卡片消息发送功能，支持发送 QQ音乐、网易云音乐、酷狗音乐等平台的音乐卡片，以及自定义音乐卡片。

## 实现方式

### 1. 外部签名服务

由于音乐卡片消息需要特定格式的 Ark JSON，本实现采用了外部签名服务来生成 Ark 消息体：

- **签名服务地址**: `https://ss.xingzhige.com/music_card/card`
- **参考实现**: [NapCatQQ](https://github.com/NapNeko/NapCatQQ) 的音乐卡片功能

签名服务接收音乐类型和相关参数，返回可直接发送的 Ark JSON。

### 2. 新增文件

#### `yogurt/src/commonMain/kotlin/org/ntqqrev/yogurt/music/MusicSegment.kt`

包含以下类：

- `MusicType` - 音乐平台枚举 (qq/163/kugou/migu/kuwo/custom)
- `MusicSegment` / `MusicData` - 音乐消息段数据结构
- `MusicSignRequest` - 签名请求数据类
- `MusicSignResponse` - 签名响应数据类
- `MusicSignException` - 签名异常类
- `MusicSigner` - 签名器，负责调用外部签名服务

#### `yogurt/src/commonMain/kotlin/org/ntqqrev/yogurt/api/message/SendMusicMessage.kt`

新增两个 HTTP API 端点：

- `POST /api/send_group_music` - 发送群聊音乐卡片
- `POST /api/send_private_music` - 发送私聊音乐卡片

### 3. API 使用方式

#### 平台音乐 (qq/163/kugou/migu/kuwo)

```json
POST /api/send_group_music
{
    "group_id": 123456789,
    "music_type": "163",
    "id": "1999253939"
}
```

#### 自定义音乐卡片

```json
POST /api/send_group_music
{
    "group_id": 123456789,
    "music_type": "custom",
    "url": "https://music.163.com/#/song?id=1999253939",
    "audio": "https://example.com/audio.mp3",
    "title": "歌曲标题",
    "singer": "歌手名称",
    "image": "https://example.com/cover.jpg"
}
```

### 4. 为什么不使用 Milky 的 OutgoingSegment？

Milky 协议的 `OutgoingSegment` 是 sealed class，无法在外部扩展添加新的消息类型。因此选择了添加独立的 HTTP API 端点来实现音乐卡片功能，而不是作为消息段类型。

## 测试结果

| 音乐类型 | 状态 | 备注 |
|---------|------|------|
| 网易云 (163) | ✅ 正常 | |
| 自定义 (custom) | ✅ 正常 | |
| QQ音乐 (qq) | ⚠️ 部分可用 | 签名服务可能返回非 Ark 格式 |
| 酷狗 (kugou) | ⚠️ 依赖签名服务 | |

## 配置

可在 `config.json` 中配置签名服务地址（可选）：

```json
{
    "musicSignUrl": "https://ss.xingzhige.com/music_card/card"
}
```

如不配置，默认使用上述地址。

## 相关参考

- [NapCatQQ 音乐卡片实现](https://github.com/NapNeko/NapCatQQ)
- 签名服务: `https://ss.xingzhige.com/music_card/card`
