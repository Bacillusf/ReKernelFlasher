# RKPConfig（百宝箱）

从独立应用 **True RKP** 移植进本项目的页面，入口在 **设置 → 百宝箱 → RKPConfig**。
功能与独立版保持一致：读取/应用 RKP 配置、续期签发、查看状态、生成 CSR、校验证书、
工程模式、清空数据、清空/复制日志、设置 su 路径、日志落盘。

---

## 一、入口与路由

| 位置 | 文件 | 说明 |
|---|---|---|
| 百宝箱列表项 | `ui/screens/toolbox/ToolboxContent.kt` | 新增 `RKPConfig` 条目（在「修复 RKP」下方） |
| 路由注册 | `MainActivity.kt` | 新增 `composable("toolbox/rkp_config")` |
| 页面 | `ui/screens/toolbox/RkpConfigContent.kt` | Compose 页面（Material3，沿用项目组件风格） |

---

## 二、新增的核心类（`toolbox/rkp/`）

| 文件 | 对应独立版 | 职责 |
|---|---|---|
| `RkpCommands.kt` | `RkpCommands.java` | 所有 shell 命令的唯一出处（getprop/setprop/csr/certify/clear/engineer/dump） |
| `RkpShell.kt` | `Sh.java` | root shell 执行层：`ProcessBuilder(su,"-c",script)` + 哨兵切分逐条输出 + 超时 + 流式回调。**保留可自定义 su 路径**，因此不用 libsu |
| `RkpLauncher.kt` | `RkpdLauncher.java` | 释放 `assets/rkpdapp.apk`、查找系统 rkpdapp、选择 setns 助手、拼启动命令 |
| `RkpMain.kt` | `RkpMain.java` | `app_process` 入口（`systemMain()` + `IPackageManager` 代理 + Application 挂载 + PathClassLoader） |
| `RkpProvisioning.kt` | `Provisioning.java` | 反射调用 AOSP rkpdapp 完成 RKP 分配与 dump |
| `RkpWidevine.kt` | `Widevine.java` | Widevine 证书分配（**优先 China URL**） |
| `RkpHiddenApi.kt` | `HiddenApi.java` | 双反射解除隐藏 API 限制 |
| `RkpPrefs.kt` | `Prefs.java` | 配置存取 |
| `RkpLogger.kt` | `LogStore.java` | 日志出口抽象（界面进程 / rkpd 子进程各一套实现） |

---

## 三、启动链路（与设备实际行为一致）

「续期签发」实际执行的命令：

```
<nativeLibraryDir>/libnsexec.so  /system/bin/app_process \
    -Djava.class.path='<本项目 APK>' / \
    --nice-name=com.android.rkpdapp \
    safe.kernel.flash.toolbox.rkp.RkpMain \
    '<cacheDir>/rkpdapp.apk'
```

1. **`libnsexec.so`**（`jniLibs/arm64-v8a/`）：自研 setns 助手，进入 init 的 mount namespace 后 `execve` 后面的命令。
   找不到时依次退回 `nsenter -t 1 -m --`、直连（日志会说明用了哪种）。
2. **`--nice-name=com.android.rkpdapp`**：伪装进程名。
3. **`RkpMain`**：在 `app_process` 里执行 RKP 分配 + Widevine，然后 chown 数据目录并退出。

`RkpMain` 内部有两个关键点（都是踩过的坑）：

* **`IPackageManager` 代理**：`hasSystemFeature("cn.google.services")` 恒返回 `false`（国内无 GMS 机型兼容）。
* **必须自建 `PathClassLoader`**：`createPackageContext("com.android.rkpdapp").classLoader`
  对 APEX 安装的包返回**空 DexPathList**，会 `ClassNotFoundException`；必须
  `PathClassLoader(rkpdapp.apk, pkgCtx.classLoader)`。

---

## 四、新增的二进制资源

| 路径 | 大小 | SHA-256 | 说明 |
|---|---|---|---|
| `app/src/main/assets/rkpdapp.apk` | 1,164,970 | `4a52e05b…6dd7ac65` | AOSP `com.android.rkpdapp`（RemoteProvisioner），Apache-2.0，未修改 |
| `app/src/main/jniLibs/arm64-v8a/libnsexec.so` | 417,136 | `08604b58…43057091` | 自研 setns 助手（NDK r29 / clang 21 静态编译，语义与原 `librkp.so` 一致） |

* `rkpdapp.apk` **缺失也能工作**：会退回用 `PackageManager` 查询设备上已安装的那份。
* `.gitignore` 原本会忽略 `*.apk`，已加一行 `!app/src/main/assets/rkpdapp.apk` 例外。
* `libnsexec.so` 缺失也能工作：退回 `nsenter` 或直连。
* `app/build.gradle.kts` 的 `packaging.jniLibs` 增加了 `keepDebugSymbols += "**/libnsexec.so"`，
  避免 AGP 的 strip 任务处理这个预编译产物。

---

## 五、Manifest 变更

新增 `<queries>`（Android 11+ 才能用 `PackageManager` 查询这些包）：

```xml
<queries>
    <package android:name="com.android.rkpdapp" />
    <package android:name="com.google.android.rkpdapp" />
    <package android:name="com.android.remoteprovisioner" />
    <package android:name="com.oplus.engineermode" />
    <package android:name="io.github.vvb2060.keyattestation" />
</queries>
```

---

## 六、界面

沿用项目的 Material3 风格（`surfaceVariant` 卡片 + 项目排版），四块：

1. **状态**：ROOT / StrongBox / 命名空间助手
2. **配置**：启用 RKP、服务器地址（含 Google / GrapheneOS 快捷 chip）、仅 StrongBox、仅 TEE、连接超时滑杆
3. **操作**：读取配置 / 应用配置 / 续期签发 / 查看状态 / 生成 CSR / 校验证书 / 工程模式 / 清空数据
   （**长按「应用配置」设置 su 路径**）
4. **日志**：带级别着色的等宽日志，可复制、清空，同时落盘 `filesDir/rkp.log`

---

## 七、构建注意

* 本项目 `compileSdk = 37`。当前 SDK 的标准渠道里没有 `platforms;android-37`，
  预览渠道提供 `platforms;android-37.0/37.1/37.2`：
  ```bash
  sdkmanager --channel=3 "platforms;android-37.0"
  ```
  若 AGP 找不到 `android-37`，可临时把 `compileSdk` 调到 36 验证本功能（本功能不依赖 API 37）。
* 首次构建需要下载 AGP 8.13.2 / Compose / Room / KSP / libsu 等依赖。
* `minSdk = 29`，本功能用到的 API（`java.time`、`android.system.Os`、`PathClassLoader`）都满足。

---

## 八、验证状态

### ✅ 已在本机完整构建通过（2026-09-26）

```
BUILD SUCCESSFUL in 29s
产物：app/build/outputs/apk/debug/app-debug.apk
      31,110,621 B
      SHA-256 8e323777f210f3d650669c07a1c6f953382545c044fcc01e9fafc889ddc4bf0c
警告数：0
```

产物内容核对（`aapt2` + `dexdump`）：

| 检查项 | 结果 |
|---|---|
| 包名 / 版本 | `safe.kernel.flash`，versionCode 20800 / versionName 2.8 |
| `native-code` | `arm64-v8a`（含 `lib/arm64-v8a/libnsexec.so`，417,136 B） |
| 内嵌资源 | `assets/rkpdapp.apk`（1,164,970 B）✓ |
| 全部新增类 | `RkpMain` / `RkpProvisioning` / `RkpLauncher` / `RkpConfigContent` 均在 dex 中 |
| **app_process 入口** | `RkpMain.main` = `PUBLIC STATIC FINAL ([Ljava/lang/String;)V` ✓ |
| 关键字符串 | `setHiddenApiExemptions`、`cn.google.services`、`remote_provisioning.hostname` 均在 |

### 构建过程中的三处修正

1. **`app/build.gradle.kts`：新增 `compileSdkMinor = 0`**
   公开 SDK 仓库里 API 37 的平台包名是 `platforms;android-37.0`（不存在 `android-37`），
   原配置会报 `Failed to find target with hash string 'android-37'`。
   `compileSdk = 37` + `compileSdkMinor = 0` 才能匹配到已安装的 `android-37.0`。
2. **`.gitignore`：新增 `!app/src/main/assets/rkpdapp.apk`**
   原有 `*.apk` 规则会把内嵌的 rkpdapp 一起忽略掉。
3. **本机 Maven 镜像配置**（`~/.gradle/init.gradle`，不属于本项目）
   把 `dl.google.com` / `repo1.maven.org` / `plugins.gradle.org` 原地改写为阿里云镜像；
   **jitpack.io 保持原样**（libsu 只在 JitPack 上发布，而 miuix 在 Maven Central，会先被 Central 镜像命中）。

---

## 九、Release 签名构建

### ✅ 已产出签名包（2026-09-26）

```
BUILD SUCCESSFUL in 1m 36s    （含 lintVital，未跳过任何任务）
产物：app/build/outputs/apk/release/app-release.apk
      23,337,231 B
      SHA-256 f554d9d2dc3f3e766b001c47b863272cfff9a967c122b15b29ee3837f286532d
```

`apksigner verify --print-certs` 结果：

```
Verifies
Verified using v2 scheme: true      （minSdk 29，v2 已足够，无需 v1）
Number of signers: 1
certificate DN:                CN=Bacillusf, OU=Unknown, O=Unknown, L=Chengdu, ST=Sichuan, C=CN
certificate SHA-256 digest:    30a1646080234ff50a0c7ead70931e1a8ef2a0d915defde30af8196334ed957d
```

该指纹与 `RKF-release-key.jks` 中的证书**完全一致**，确认用的是项目自带的 release 密钥。

### 签名配置（`keystore.properties`）

`app/build.gradle.kts` 从**项目根目录**的 `keystore.properties` 读取口令（该文件在 `.gitignore` 内，不会被提交）：

```properties
storeFile=../RKF-release-key.jks
storePassword=***
keyAlias=rekernelflasher
keyPassword=***
```

> 注意 `storeFile` 写成 `../RKF-release-key.jks`：AGP 里 `file()` 是相对 **模块目录**（`app/`）解析的，
> 而密钥库放在仓库根目录，所以要回退一层。
> 另外 PKCS12 里的实际别名为小写 `rekernelflasher`（大小写以 `keytool -list` 为准）。

### 复现

```bash
cd items/RekernelFlasher
./gradlew :app:assembleRelease        # 需要 keystore.properties 存在
```

### 唯一一次失败：lint 依赖下载超时（非代码问题）

第一次 `assembleRelease` 失败在 `:app:lintVitalAnalyzeRelease`，原因是
`intellij-core-31.13.2.jar` 与 `kotlin-compiler-31.13.2.jar` 从镜像下载**读超时**（瞬时网络问题）。
重试即通过；若以后再遇到，两种处理：

* 直接重试（本次就是重试后成功的）；
* 或临时跳过：`./gradlew :app:assembleRelease -x lintVitalAnalyzeRelease`；
* 或永久关闭 release lint：在 `android { }` 里加 `lint { checkReleaseBuilds = false }`。

### 真机行为

功能逻辑与**已在真机验证过的独立版 True RKP v1.0** 完全一致
（独立版在 OnePlus PJZ110 / Android 16 / KernelSU 上完整跑通：RKP 分配成功 2/2、Widevine 走 China URL 返回 1844 字节）。
本项目内的版本尚未装机实测，建议安装后先点「读取配置」（纯读，安全）→「查看状态」→ 再用「续期签发」。
