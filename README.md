# That Sky Switch

一个用于切换 **Sky: Children of the Light Android version** 服务器和跳过证书校验的 Xposed 模块

## 功能

- 🔄 **服务器切换**：将光遇的服务器地址从官方默认地址替换为自定义 hostname
- ⚙️ **跳过证书校验**：去除SSL证书校验

## 环境要求

| 要求 | 版本 |
|------|------|
| Android | 8.0+（API 26+） |
| Xposed 框架 | LSPosed（API 101+） |
| 目标应用 | `com.tgc.sky.android` |

## 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose + Material 3
- **Java Hook**：[libxposed](https://github.com/libxposed/api) v101
- **Native Hook**：[ShadowHook](https://github.com/bytedance/shadowhook) v2.0
## 构建

```bash
# 克隆项目
git clone https://github.com/that-sky-project/that-sky-switch.git
cd that-sky-switch

# 构建 debug APK
./gradlew :app:assembleDebug

# 构建 release APK
./gradlew :app:assembleRelease
```

生成的 APK 位于 `app/build/outputs/apk/` 目录。

## 安装使用

1. 确保设备已安装 LSPosed 框架
2. 构建并安装本模块 APK
3. 在 LSPosed 管理器中启用本模块，并勾选作用域 `com.tgc.sky.android`
4. 重启光遇或重启设备
5. 在模块配置界面中设置自定义服务器地址

## License
