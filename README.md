# DSH Agents Android

一个应用,两种配对:

- **DSH 远程访问** — 扫描 [dsh-remote-web-ui](https://github.com/zhu1090093659/dsh-web/tree/main/packages/dsh-remote-web-ui) 插件「远程访问」面板中的二维码,在内置 WebView 中完成配对并使用官方 DSH Web GUI(含移动端适配层)。设备凭证由 WebView 的 Cookie 与 DOM Storage 持久化保存,已配对主机一键免重扫进入。
- **Agents Anywhere** — 完整的 Agents Anywhere 原生客户端:Cloud / 自托管登录、扫码登录、设备配对与管理、会话时间线与对话、工具审批、远程 shell/终端、文件浏览与预览(以 [Agents-Anywhere](https://github.com/anywhere-labs/Agents-Anywhere) 官方 Android 客户端源码为蓝本)。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- compileSdk 36 / minSdk 26 (Android 8.0+) / targetSdk 36,JDK 17
- CameraX + MLKit(扫码)、Android WebView(DSH 容器)、OkHttp(v2 API)

## 构建

1. 安装 Android Studio(内含 JDK 17 与 Android SDK)。
2. 打开本目录(`dsh-agents-android/`),让 Android Studio 完成 Gradle 同步。
3. 连接开启 USB 调试的手机,运行 `app` 配置。

命令行构建(需 JDK 17、Android SDK,并设置 `ANDROID_HOME` 或在 `local.properties` 中写入 `sdk.dir`):

```bash
# Windows
gradlew.bat testDebugUnitTest assembleDebug

# macOS / Linux
./gradlew testDebugUnitTest assembleDebug
```

产物位于 `app/build/outputs/apk/`。release 构建未配置签名;请使用自己的 keystore 并保持签名一致。

## 使用

### DSH 远程访问

1. 在电脑的 DSH Web 侧边栏底部打开手机图标,进入「远程访问」面板(局域网访问需先开启,或使用公网隧道)。
2. 在 App 首页选择「DSH 远程访问」,对准面板中的二维码;也可以复制配对链接手动粘贴。
3. 配对成功后自动进入官方 Web GUI。之后可在「已配对主机」列表一键进入,免重扫。

实现要点:

- 配对链路(`/pair-accept → /pair-app → /`)完全由 WebView 完成,设备 Cookie 与 DOM Storage 由系统 WebView 持久化。
- 主框架收到 403(插件门禁:已取消配对/会话过期)时显示「配对已失效」覆盖层,可一键回到配对页。
- 主框架收到 401(harness 浏览器认证页,纯 HTTP 局域网重开裸 `/` 的场景)自动改走插件托管的 `/pair-app` 落地页一次;https origin 由插件的 reopen service worker 处理。
- 支持 Web GUI 内的文件选择器上传。

### Agents Anywhere

在 App 首页选择「Agents Anywhere」,登录 Cloud 或自托管服务,即可使用设备、会话、审批、终端与文件功能。详细能力见上游仓库文档。

## 与官方应用的差异

- 独立 `applicationId`(`com.dshagents.app`),可与官方 Agents Anywhere 应用共存安装。
- 移除了应用内更新占位模块(其下载地址为 `.invalid` 占位)。
- OAuth 回调 scheme(`agents-anywhere://oauth/callback`)与官方应用一致:若两者同时安装,网页登录回跳时系统可能弹出应用选择框。

## 源码说明

`app/src/main/java/com/dshagents/app/` 下:

- `api/`、`feature/`(除 `dshremote/`)、`ui/`(除 `modepicker/`、`screens/dshremote/`)、`navigation/`、`model/` — 移植自 Agents Anywhere 官方 Android 客户端
- `feature/dshremote/` — DSH 配对链接解析与已配对主机持久化(新增)
- `ui/screens/modepicker/` — 双模式入口页(新增)
- `ui/screens/dshremote/` — DSH 扫码配对页与 WebView 容器页(新增)

## 遥测与隐私

App 自身不收集任何数据。DSH Web GUI 由 dsh-remote-web-ui 插件提供服务,其浏览器侧每日发送一次匿名安装心跳(见上游说明)。
