# 喵喵助手魔改版（MiaoAssistantX）

本项目是基于「喵喵助手」的深度重构与扩展版本，由「夕兮 / SKYRAIN」创作发布。

## 协议说明

本项目按照 **GNU Affero General Public License v3.0（AGPLv3）** 协议开放源代码。

- 完整协议文本见同目录下的 [LICENSE](./LICENSE)。
- 原始「喵喵助手」项目遵循 **AGPLv3** 协议开放源代码，其官方源码仓库为：
  https://github.com/QiCaiJie114514/QQMiaoAssistant
- 本项目作为其在 AGPLv3 下的二次开发与扩展版本，全部新增及重构内容由「夕兮 / SKYRAIN」创作并发布，同样按 AGPLv3 许可。

依据 AGPLv3，您可以自由使用、复制、修改与再分发本项目，但任何基于本项目的修改、衍生或通过网络提供服务的版本，都必须以相同的 AGPLv3 协议公开其完整源代码，并保留版权与许可声明。

## 唯一官方获取渠道

请仅通过 **QQ：792413184**或本仓库获取本项目源码，并自行构建安装。

⚠️ 任何在本项目以外获取的二进制、安装包、网盘链接、第三方 fork 版本均非官方发布，若其中被植入恶意代码，由对应分发者自行承担全部法律责任。

## 主要变更

- 对核心架构进行了全面重构，优化了执行逻辑与代码质量。
- 修复了多个已知漏洞，提升了稳定性与安全性。
- 新增了 AI 和应用选择等功能，显著增强了通用性。
- 项目体积由约 KB 级增长至MB 级，反映了功能的丰富。

注：特此感谢「喵喵助手」提供的源代码。

## 构建

本项目使用 Android Gradle Plugin 构建，无需第三方依赖：

```bash
export ANDROID_HOME=/usr/lib/android-sdk
./gradlew assembleDebug
```

产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

© 夕兮 / SKYRAIN  ·  渠道 QQ：792413184
