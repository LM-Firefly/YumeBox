<div align="center">

**简体中文** | [English](README.md)

<img src="logo.webp" style="width: 96px;" alt="logo">

## YumeBox

[![Latest release](https://img.shields.io/github/v/release/YumeLira/YumeBox?label=Release&logo=github)](https://github.com/YumeLira/YumeBox/releases/latest)[![GitHub License](https://img.shields.io/github/license/YumeLira/YumeBox?logo=gnu)](/LICENSE)![Downloads](https://img.shields.io/github/downloads/YumeLira/YumeBox/total)

**一个基于 [mihomo](https://github.com/MetaCubeX/mihomo) 内核的开源 Android 客户端**

</div>

## 特性

- Mihomo 内核
- SubStore 可选支持
- Web 面板
- 更多请自行探索...

## 适配性

- Android 7.0 及以上
- 支持 `armeabi-v7a`、`arm64-v8a`、`x86`、`x86_64` 架构

## 使用方法

- **安装**：前往 [Releases](https://github.com/YumeLira/YumeBox/releases)
- **构建**：[跳转至构建章节](#构建)

### 外部控制 API

YumeBox 支持通过 Android Intent 进行外部控制，使其他应用能够启动或停止代理服务。

- 启动 Clash.Meta 服务

  向活动 `com.github.yumelira.yumebox.MainActivity` 发送带有动作  
  `com.github.yumelira.yumebox.action.START_CLASH` 的 Intent

- 停止 Clash.Meta 服务

  向活动 `com.github.yumelira.yumebox.MainActivity` 发送带有动作  
  `com.github.yumelira.yumebox.action.STOP_CLASH` 的 Intent

- 导入配置文件

  使用 URL Scheme：`clash://install-config?url=<encoded URI>`  
  或 `clashmeta://install-config?url=<encoded URI>`

## 讨论

- Telegram 群组：[@OOM_WG](https://t.me/OOM_Group)

## 参与翻译

如果您希望将 YumeBox 翻译为更多语言，或改进现有翻译，请 Fork 本项目，并在 `lang` 目录下创建或更新对应的翻译文件。

## 构建

1. **同步 core 源码**

   ```bash
   sh scripts/sync-kernel.sh <alpha|meta|smart>
   ```

2. **安装依赖**
   请确保已安装 OpenJDK 24、Android SDK、CMake 与 Golang。

3. **在项目根目录创建 `local.properties`**

   ```properties
   sdk.dir=/path/to/android-sdk
   ```

4. **（可选）自定义包名：修改 `gradle.properties`**

   ```properties
   project.namespace.base=com.github.yumelira.yumebox
   project.namespace.core=${project.namespace.base}.core
   project.namespace.extension=${project.namespace.base}.extension
   project.namespace.buildlogic=${project.namespace.base}.buildlogic
   ```

5. **在项目根目录创建 `signing.properties`**

   ```properties
   keystore.path=/path/to/keystore/file
   keystore.password=<key store password>
   key.alias=<key alias>
   key.password=<key password>
   ```

6. **构建应用**

   本体:

   ```bash
   ./gradlew app:assembleRelease
   ```

   SubStore 拓展:

   ```bash
   ./gradlew extension:assembleRelease
   ```

## 特别

> [!IMPORTANT]
> 作者对这个项目中的代码一无所知。代码处于可用或不可用状态，没有第三种情况。

## 鸣谢

- [Mihomo](https://github.com/MetaCubeX/mihomo)
- [ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid)
- [SubStore](https://github.com/sub-store-org)
- [SubCase](https://github.com/sion-codin/SubCase)
- [Yacd-meta](https://github.com/MetaCubeX/Yacd-meta)
- [Zashboard](https://github.com/Zephyruso/zashboard)
