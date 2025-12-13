<div align="center">

**English** | [简体中文](README_ZH_HANS.md)

<img src="logo.webp" style="width: 96px;" alt="logo">

## YumeBox

[![Latest release](https://img.shields.io/github/v/release/YumeLira/YumeBox?label=Release&logo=github)](https://github.com/YumeLira/YumeBox/releases/latest)
[![GitHub License](https://img.shields.io/github/license/YumeLira/YumeBox?logo=gnu)](/LICENSE)
![Downloads](https://img.shields.io/github/downloads/YumeLira/YumeBox/total)

**An open-source Android client based on the [mihomo](https://github.com/MetaCubeX/mihomo) kernel**

</div>

## Features

- Mihomo kernel
- SubStore Optional Support
- Web dashboard
- For more, please explore on your own...

## Compatibility

- Android 7.0 and above
- Supports `armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64` architectures

## Usage

- **Install**: Visit the [Installation](https://yume.mintlify.app/yumebox/guide/install) page
- **Build**: See the [Build section](https://yume.mintlify.app/yumebox/guide/building)

## Discussion

- Telegram group: [@OOM_WG](https://t.me/OOM_Group)

## Contributing Translations

To translate YumeBox into your language or improve existing translations, please fork this project
and create or update translation files in the `lang` directory.


1. **Sync core source code**

   If this repository is configured with the mihomo submodule, update it by running:

   ```bash
   git submodule update --init --remote --depth=1 core/src/foss/golang/mihomo
   ```

   To switch branch (e.g., Alpha):

   ```bash
   git -C core/src/foss/golang/mihomo fetch origin Alpha && git -C core/src/foss/golang/mihomo checkout Alpha
   ```

2. **Install dependencies**
   Ensure that OpenJDK 24, Android SDK, CMake, and Golang are installed.

3. **Create `local.properties` in the project root**

   ```properties
   sdk.dir=/path/to/android-sdk
   ```

4. **(Optional) Customize the package name by editing `gradle.properties`**

   ```properties
   project.namespace.base=com.github.yumelira.yumebox
   project.namespace.core=${project.namespace.base}.core
   project.namespace.extension=${project.namespace.base}.extension
   project.namespace.buildlogic=${project.namespace.base}.buildlogic
   ```

5. **Create `signing.properties` in the project root**

   ```properties
   keystore.path=/path/to/keystore/file
   keystore.password=<key store password>
   key.alias=<key alias>
   key.password=<key password>
   ```

6. **Build the application**

   Ontology:

   ```bash
   ./gradlew app:assembleRelease
   ```

   SubStore Extension:

   ```bash
   ./gradlew extension:assembleRelease
   ```

   Ontology With SubStore Extension:

   ```bash
   ./gradlew assembleReleaseWithExtension
   ```

## Note

> [!IMPORTANT]
> The author has no knowledge of the code in this project. The code is either functional or non-functional; there is no third state.

## Acknowledgements

- [Mihomo](https://github.com/MetaCubeX/mihomo)
- [ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid)
- [SubStore](https://github.com/sub-store-org)
- [SubCase](https://github.com/sion-codin/SubCase)
- [Yacd-meta](https://github.com/MetaCubeX/Yacd-meta)
- [Zashboard](https://github.com/Zephyruso/zashboard)
