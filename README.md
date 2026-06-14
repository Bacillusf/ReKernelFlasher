# Kernel Flasher

Android 内核刷写工具，支持在已 root 设备上刷写、备份、恢复 Android 内核镜像。

基于 [KernelFlasher](https://github.com/fatalcoder524/KernelFlasher) 二次开发，由 [Bacillusf](https://github.com/Bacillusf) 维护。

---
## 灵感
原版KernelFlasher对新手很不友好，没有二次弹窗确认，容易刷错分区，很多新手没有备份意识，刷错了只能无助的等待全量包...而且原仓库已经归档，于是我做了这个分支

## 功能

### 首页
| 功能 | 说明 |
|---|---|
| 设备信息 | 显示型号、构建版本、Android 版本、应用版本、内核版本、Root 管理器（KernelSU/Magisk/APatch）、槽位后缀、SUSFS 版本、Boot HAL 版本 |
| 重启菜单 | 右上角按钮展开，支持重启到系统/Recovery/Bootloader/Download/EDL，每次带确认弹窗 |

### 刷写
| 功能 | 说明 |
|---|---|
| 槽位管理 | 查看 A/B 槽位信息（可启动、已标记成功、Boot SHA1、镜像格式、ramdisk 信息） |
| 刷写 AK3 Zip | 选择 AnyKernel3 兼容 zip 刷入 |
| 刷写 AK3 Zip (mkbootfs) | 以 mkbootfs 模式刷入 AK3 zip |
| 刷写 KernelSU 驱动 | 刷入 KernelSU LKM 内核模块 |
| 刷写分区镜像 | 选择 .img 文件刷入指定分区 |
| 备份分区 | 选择分区备份到本地 |
| 恢复备份 | 从历史备份恢复分区 |
| vendor_dlkm 管理 | 挂载/卸载/映射/取消映射 vendor_dlkm 分区 |
| 检查内核版本 | 提取当前 boot 镜像中的内核版本字符串 |
| 操作历史 | 查看所有刷写/备份/重启等操作记录 |

### 设置
| 功能 | 说明 |
|---|---|
| 备份管理 | 查看和管理所有备份 |
| 更新源 | 添加和管理内核更新 JSON URL |
| 自动备份 | 开关控制，开启后刷写分区或 AK3 前自动备份目标分区到 `/sdcard/KernelFlasher-AutoBackup/` |
| 查看自动备份记录 | 显示自动备份历史，长按可定位到文件 |
| 保存 ramoops | 保存内核崩溃日志 |
| 保存 dmesg | 保存内核日志 |
| 保存 logcat | 保存系统日志 |
| 界面缩放 (DPI) | 拖动滑条调整界面缩放比例（50%-150%），点击应用生效 |

### 二次确认
所有操作均有确认弹窗（带渐入动画），包括：
- 刷写 AK3/镜像/KernelSU 驱动 → 显示源路径 → 目标分区路径
- 备份/恢复/删除分区
- 重启到各模式

### 操作历史
- 持久化存储到 `/sdcard/KernelFlasher/history.json`
- 记录格式：`yyyy年MM月dd日HH时 操作描述`
- 支持清空历史

### 自动备份
- 开启后，刷写前自动 `dd` 提取目标分区镜像
- 保存路径：`/sdcard/KernelFlasher-AutoBackup/{unix时间戳}/分区名.img`
- 同时生成 `backup.yml` 元数据和 `summary.json` 总索引
- 记录页面长按可调用系统文件管理器打开备份文件

---

## 技术栈

| 类别 | 技术 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | MVVM |
| Root | libsu |
| 数据库 | Room |
| 序列化 | kotlinx.serialization |
| HTTP | OkHttp + Retrofit |

## 原生二进制

| 文件 | 用途 |
|---|---|
| magiskboot | Boot 镜像解包/打包 |
| lptools_static | 逻辑分区管理 |
| httools_static | Fstab 导出、AVB、挂载 |
| bootctl | A/B 槽位控制 |
| busybox | 通用 shell 工具集 |

## 构建

```bash
# 前置条件：JDK 21、Android SDK (compileSdk 36)
./gradlew assembleDebug   # 调试版
./gradlew assembleRelease # 发布版
```

---

## 许可证

GPL v3.0 License

---

## 变更记录

### 新增功能（基于原版 KernelFlasher 1.6.0）
- **UI 重构**：底部三栏导航（首页/刷写/设置）
- **DPI 缩放**：设置中可调整界面缩放
- **操作历史**：记录所有刷写/备份/重启操作，持久化存储
- **自动备份**：刷写前自动备份目标分区
- **二次确认弹窗**：所有操作带渐入动画确认弹窗
- **中文界面**：所有弹窗和日志中文化
- **Root 管理器显示**：首页显示当前 Root 方案
- **文件定位**：长按自动备份记录调用系统打开文件
- **包名变更**：`safe.kernel.flash`
- **解决部分手机兼容性问题**: 解决了Invalid ramdisk in boot.img
