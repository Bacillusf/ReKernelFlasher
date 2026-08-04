# Changelog

## v2.7 (20700) - 2026-08-04

- 迁移 KernelSU 3.2.5 Manager 的 MIUIx 风格、浮动液态玻璃底栏和主页面 Pager 切换逻辑。
- 重做启动检测页，统一首次启动和后续启动流程，移除旧向导页面。
- 首次启动支持 RKF 后端模块安装，并提供 MagicMountRS 重定向方案和 Meta-OverlayFS 挂载方案选择。
- 优化启动检测项和后端工具准备，覆盖 Root、Root 管理器、模块包、工作目录、RootService、分区工具、刷写工具、BusyBox、脚本和解包工具。
- 修复 boot/init_boot/vendor_boot 检测 fallback，修正 magiskboot 工作目录依赖和 vendor_ramdisk.cpio 兼容。
- 更新内置 arm64 magiskboot。
- 整理百宝箱解包入口和解包记录页面。
- 清理旧向导和不再调用的启动代码，避免冗余源码和缓存占用。

详见：docs/releases/v2.7.md

