# 数独（Android 原生复刻版）

用 **Kotlin + Jetpack Compose** 对 Flutter 版数独应用（`Desktop/application/sudoku`）的完整原生重写，界面与逻辑保持一致。

## 功能
- 三种模式：3×3 经典数独、4×4 数独、杀手数独（虚线笼）
- 难度：极简 / 简单 / 中等 / 困难（3×3 挖空后唯一解校验；4×4 与原版一致）
- 笔记模式、撤销 / 重做、错误计数、计时、暂停自动存档
- 账户系统：注册 / 登录 / 个人资料（头像 base64）/ 修改资料 / 注销
- 云端存档（自动保存、手动保存 / 加载）
- 排行榜（提交积分、总榜、个人排名），积分公式与 Flutter 版一致
- 音效（SoundPool）+ 震动反馈

## 技术栈
- Kotlin 2.2.10 + Jetpack Compose（BOM 2026.02.01，UI 1.10.4）
- AGP 9.2.1，compileSdk 36 / targetSdk 35 / minSdk 24
- 无第三方导航 / 网络库：状态栈导航、HttpURLConnection、SharedPreferences

## 构建
```bash
# Windows
gradlew.bat assembleDebug
# macOS / Linux
./gradlew assembleDebug
```
需要 Android SDK，路径见 `local.properties`（本机已配置好）。
Debug APK 输出：`app/build/outputs/apk/debug/app-debug.apk`
安装：`adb install -r app/build/outputs/apk/debug/app-debug.apk`

## 连接后端
后端沿用原 Flutter 项目的 Dart shelf 服务（`sudoku/server`，端口 8080，依赖 MySQL）。
- 模拟器：App 自动使用 `10.0.2.2:8080`
- 真机：先执行 `adb reverse tcp:8080 tcp:8080`，App 自动使用 `localhost:8080`

## 与 Flutter 版差异
- 界面与交互 1:1 移植；棋盘用 Compose Canvas 绘制（含杀手笼虚线框）
- 图标为自绘 ImageVector（未引入 icons 扩展库）
- 无第三方依赖库；依赖已下载到本机 Gradle 缓存，可加 `--offline` 构建