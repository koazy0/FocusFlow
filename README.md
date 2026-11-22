# FocusFlow

FocusFlow 是一款 Android 专注计时器。轻点台灯色的圆形按钮开始计时，结束后可以查看今日、近七天和历史专注记录。记录保存在设备本地的 SQLite 数据库中。

## 安装

在 [GitHub Releases](https://github.com/koazy0/FocusFlow/releases) 下载带版本号的 APK。支持 Android 8.0 及以上。已有用户可以直接安装升级，卸载会删除本地专注记录。

首次开启防切出保护时，请按系统提示授权。系统来电及通话期间暂停拉回，锁屏期间也会放行。

## 从源码构建

使用 JDK 21，安装 Android SDK Platform 36 和 Build-Tools 36.0.0。设置 `ANDROID_HOME` 指向本机 SDK，或在项目根目录的 `local.properties` 中填写 `sdk.dir`。

在 Windows 运行 `.\gradlew.bat :app:assembleDebug`，在 macOS 或 Linux 运行 `./gradlew :app:assembleDebug`。测试安装包位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 发布新版本

1. 在 `gradle.properties` 中更新 `focusflow.versionName`（如 `1.1.1`），并递增 `focusflow.versionCode`。设置页和安装包会使用相同的版本名。每个正式发布的安装包使用新的内部版本号。
2. 提交改动，从干净检出运行 `:app:assembleRelease`。未签名包位于 `app/build/outputs/apk/release/app-release-unsigned.apk`。
3. 使用 Android Build-Tools 的 `apksigner sign` 和维护者保管的原签名密钥生成 `FocusFlow-<版本>.apk`。使用 `apksigner verify --verbose --print-certs` 检查签名，与上一版证书指纹比对，用 `aapt dump badging` 核对版本。
4. 为构建对应的提交创建 `v<版本>` 标签，推送提交和标签，在 GitHub Release 附上已签名 APK、更新说明和 SHA-256 校验值。

早期安装包使用项目维护者本机的 Android 调试签名。为了让原用户保留记录升级，1.1.0 的发布构建沿用该证书。维护者应单独备份原密钥；新电脑自动生成的调试密钥无法用于覆盖升级。
