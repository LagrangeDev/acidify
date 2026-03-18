# 签名 API 标准

> [!important]
> 
> Yogurt **不提供**内置签名 API，请自行寻找或实现相关 API。本章节仅介绍 Yogurt 遵循的签名 API 标准规范，供开发者参考或使用者检查签名是否符合规范之用。

## PC 签名

设配置文件中的 `protocol.signApiUrl` 为 `$BASE`。

### POST `$BASE`

提供签名功能。

#### 请求示例

```http request
POST $BASE HTTP/1.1
Content-Type: application/json

{
  "cmd": "wtlogin.login",
  "src": "0123456789abcdef",
  "seq": 100
}
```

#### 响应示例

```json
{
  "platform": "Linux",
  "version": "3.2.19-39038",
  "value": {
    "sign": "0123456789abcdef",
    "token": "0123456789abcdef",
    "extra": "0123456789abcdef"
  }
}
```

其中：

- `platform` 的值必须与初始化所用的 `AppInfo` 的 `Os` 相同；
- `version` 的值必须与初始化所用的 `AppInfo` 的 `CurrentVersion` 相同；
- `value` 中的 `sign`、`token` 和 `extra` 为签名所需字段，必须是 HEX 字符串或空字符串，下同。

以上所列为响应中必须包含的字段，开发者可以根据实际情况添加其他字段，下同，不再赘述。

### GET `$BASE/appinfo`

提供符合 Lagrange.Core (V1) 标准的 AppInfo JSON，表示签名的版本信息。

此 API 为可选 API，如果未提供，用户可以选择使用内置或自定义的 AppInfo 登录。

#### 响应示例

```json
{
  "Os": "Linux",
  "Kernel": "Linux",
  "VendorOs": "linux",
  "CurrentVersion": "3.2.19-39038",
  "PtVersion": "2.0.0",
  "MiscBitmap": 32764,
  "SsoVersion": 19,
  "PackageName": "com.tencent.qq",
  "WtLoginSdk": "nt.wtlogin.0.0.1",
  "AppId": 1600001615,
  "SubAppId": 537313942,
  "AppIdQrCode": 13697054,
  "AppClientVersion": 39038,
  "MainSigMap": 169742560,
  "SubSigMap": 0,
  "NTLoginType": 1
}
```

## Android 签名

设配置文件中的 `protocol.signApiUrl` 为 `$BASE`。

### POST `$BASE/sign`

提供签名功能。

#### 请求示例

```http request
POST $BASE/sign HTTP/1.1
Content-Type: application/json

{
  "uin": 1234567890,
  "cmd": "wtlogin.login",
  "buffer": "0123456789abcdef",
  "guid": "0123456789abcdef0123456789abcdef",
  "seq": 100,
  "version": "9.2.20",
  "qua": "V1_AND_SQ_9.2.20_11650_YYB_D"
}
```

#### 响应示例

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "sign": "0123456789abcdef",
    "token": "0123456789abcdef",
    "extra": "0123456789abcdef"
  }
}
```

其中：

- `code` 的值为 `0` 表示成功，非 `0` 表示失败，下同；
- `msg` 的值为错误信息，成功时可以为空字符串，下同；
- `data` 在 `code` 为 `0` 时必须包含；在 `code` 非 `0` 时不应包含，下同。

### POST `$BASE/energy`

用于获取 TLV 0x544 所需的字节序列。

#### 请求示例

```http request
POST $BASE/energy HTTP/1.1
Content-Type: application/json

{
  "uin": 1234567890,
  "data": "810_9",
  "guid": "0123456789abcdef0123456789abcdef",
  "ver": "6.0.0.2589",
  "version": "9.2.20",
  "qua": "V1_AND_SQ_9.2.20_11650_YYB_D"
}
```

#### 响应示例

```json
{
  "code": 0,
  "msg": "",
  "data": "0123456789abcdef"
}
```

其中 `data` 必须是 HEX 字符串。

### POST `$BASE/get_tlv553`

用于获取 TLV 0x553 所需的字节序列（Debug XWID）。

#### 请求示例

```http request
POST $BASE/get_tlv553 HTTP/1.1
Content-Type: application/json

{
  "uin": 1234567890,
  "data": "810_9",
  "guid": "0123456789abcdef0123456789abcdef",
  "version": "9.2.20",
  "qua": "V1_AND_SQ_9.2.20_11650_YYB_D"
}
```

#### 响应示例

```json
{
  "code": 0,
  "msg": "",
  "data": "0123456789abcdef"
}
```

其中 `data` 必须是 HEX 字符串。

## Android 签名（Legacy）

> [!note]
> 
> 此节介绍的是已成为另一种既定标准的 Android 签名 API 规范，仍有部分用户在使用，且 Yogurt 仍然支持此种规范的签名 API，因此在此保留相关说明。在使用 Legacy 签名 API 时，请将 `config.json` 中的 `protocol.androidUseLegacySign` 设为 `true`。

设配置文件中的 `protocol.signApiUrl` 为 `$BASE`。

### POST `$BASE/sign`

提供签名功能。

#### 请求示例

```http request
POST $BASE/sign HTTP/1.1
Content-Type: application/json

{
  "uin": 1234567890,
  "cmd": "wtlogin.login",
  "buffer": "0123456789abcdef",
  "guid": "0123456789abcdef0123456789abcdef",
  "seq": 100,
  "version": "9.2.20",
  "qua": "V1_AND_SQ_9.2.20_11650_YYB_D",
  "android_id": "d4573bde6663bb55",
  "qimei36": "0123456789abcdef0123456789abcdef"
}
```

#### 响应示例

```json
{
  "code": 0,
  "msg": "",
  "data": {
    "sign": "0123456789abcdef",
    "token": "0123456789abcdef",
    "extra": "0123456789abcdef"
  }
}
```

### POST `$BASE/energy`

用于获取 TLV 0x544 所需的字节序列。

#### 请求示例

```http request
POST $BASE/energy HTTP/1.1
Content-Type: application/json

{
  "uin": 1234567890,
  "data": "810_9",
  "guid": "0123456789abcdef0123456789abcdef",
  "ver": "6.0.0.2589",
  "version": "9.2.20",
  "qua": "V1_AND_SQ_9.2.20_11650_YYB_D"
}
```

#### 响应示例

```json
{
  "code": 0,
  "msg": "",
  "data": "0123456789abcdef"
}
```
