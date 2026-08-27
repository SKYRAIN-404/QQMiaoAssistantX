# 喵喵助手魔改版（MiaoAssistantX）

本项目是基于「喵喵助手」的深度重构与扩展版本，由「夕兮 / SKYRAIN」创作发布。

## 协议说明

本项目按照 **GNU Affero General Public License v3.0（AGPLv3）** 协议开放源代码。

- 完整协议文本见同目录下的 [LICENSE](./LICENSE)。
- 本项目基于以下开源仓库二次开发：
  - 原作者（初版）：**QiCaiJie114514** — https://github.com/QiCaiJie114514/QQMiaoAssistant
  - 上游仓库（共同贡献者，本二改版所基于的源码）：**Duckling520-QWQ** — https://github.com/Duckling520-QWQ/QQMiaoAssistant
  - 两位上游作者（QiCaiJie114514 与 Duckling520-QWQ）系该项目的共同贡献者，并已互相取得联系确认协作关系。
- 本项目（二改版）官方源码仓库：**SKYRAIN-404** — https://github.com/SKYRAIN-404/QQMiaoAssistantX
- 全部新增及重构内容由「夕兮 / SKYRAIN」创作并发布，同样按 AGPLv3 许可。

依据 AGPLv3，您可以自由使用、复制、修改与再分发本项目，但任何基于本项目的修改、衍生或通过网络提供服务的版本，都必须以相同的 AGPLv3 协议公开其完整源代码，并保留版权与许可声明。

## 唯一官方获取渠道

请仅通过 **QQ：792413814** 或当前仓库获取本项目源码，并自行构建安装。

⚠️ 任何在本项目以外获取的二进制、安装包、网盘链接、第三方 fork 版本均非官方发布，若其中被植入恶意代码，由对应分发者自行承担全部法律责任。

## 主要变更

- 对核心架构进行了全面重构，优化了执行逻辑与代码质量。
- 修复了多个已知漏洞，提升了稳定性与安全性。
- 新增了 AI 和应用选择等功能，显著增强了通用性。
- 项目体积由约 KB 级增长至 MB 级，反映了功能的丰富。

## 新增功能

- 加入了AI修改功能，通过用户预设的提示词，api，或者离线的AI模型，自动对用户的文本进行修改
- 加入了应用选择功能，可以选择软件功能作用于选定的软件

## v1.1更新
- 修复了部分反复消耗token的恶性bug
- 对开屏通告进行了规范

注：特此感谢「QQ喵喵助手」原作者 QiCaiJie114514，以及上游仓库 Duckling520-QWQ（https://github.com/Duckling520-QWQ/QQMiaoAssistant）提供的源代码。
本二改版官方仓库：https://github.com/SKYRAIN-404/QQMiaoAssistantX

## 构建

本项目使用 Android Gradle Plugin 构建，无需第三方依赖：

```bash
export ANDROID_HOME=/usr/lib/android-sdk
./gradlew assembleDebug
```

产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

© 夕兮 / SKYRAIN  ·  渠道 QQ：792413814
```

产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

© 夕兮 / SKYRAIN  ·  渠道 QQ：792413814
