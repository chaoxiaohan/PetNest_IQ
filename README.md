# PetNestIQ 智能宠物窝

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**PetNestIQ** 是一款专为爱宠人士打造的智能宠物窝监控应用。无论您身在何处，都能通过手机实时了解宠物的生活环境，确保它们时刻处于最舒适、安全的状态。

<p align="center">
  <img src="cat.png" width="250">
</p>

## ✨ 主要功能

*   **📊 实时数据监控**:
    *   即时查看宠物窝内的 **温度**、**湿度** 和 **宠物重量**。
    *   数据面板清晰直观，关键信息一目了然。

*   **📈 24小时趋势分析**:
    *   以图表形式展示过去24小时内各项数据的变化趋势。
    *   帮助您深入了解宠物的活动规律和环境的周期性变化。

*   **🔔 智能异常提醒**:
    *   当监测数据（如温度过高/过低）超出您预设的安全范围时，应用将立即推送通知。
    *   让您能第一时间采取行动，保障爱宠健康。

*   **📱 现代化UI设计**:
    *   采用 `Jetpack Compose` 构建，界面现代、流畅。
    *   遵循 Material Design 3 设计规范，提供卓越的用户体验。

## 🛠️ 技术栈

本项目采用现代化的 Android 开发技术栈，确保应用的稳定性与可维护性。

*   **开发语言**: [Kotlin](https://kotlinlang.org/)
*   **UI框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose) - 用于构建声明式、响应式的用户界面。
*   **核心组件**:
    *   **Lifecycle**: 管理 Activity 和 Fragment 的生命周期。
    *   **ViewModel**: 以生命周期感知的方式存储和管理UI相关数据。
    *   **Coroutines**: 用于处理异步任务和数据流。
*   **图表库**: [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) (或同类图表库) - 用于展示数据趋势图。
*   **架构模式**: MVVM (Model-View-ViewModel) - 实现了良好的关注点分离。

## 🚀 如何开始

请按照以下步骤在本地设置和运行项目。

### 环境要求

*   Android Studio Iguana | 2023.2.1 或更高版本
*   JDK 17 或更高版本

### 安装与运行

1.  **克隆项目**
    ```bash
    git clone https://github.com/your-username/PetNestIQ.git
    ```

2.  **在 Android Studio 中打开**
    *   启动 Android Studio。
    *   选择 `File` > `Open`，然后导航到您克隆的项目目录。

3.  **构建并运行**
    *   等待 Gradle 同步完成。
    *   选择一个安卓设备或模拟器，点击 `Run` 按钮。

## 🏗️ 项目架构

本项目遵循经典的 **MVVM (Model-View-ViewModel)** 架构模式，旨在实现UI逻辑与业务逻辑的解耦。

*   **Model**: 负责数据层，包括从传感器（或API）获取数据、数据处理和存储。
*   **View**: 负责UI展示，由 Composable 函数构成，响应 ViewModel 中的状态变化。
*   **ViewModel**: 作为 View 和 Model 之间的桥梁，处理业务逻辑，并将数据转换为UI状态供 View 使用。

## 🤝 欢迎贡献

我们欢迎任何形式的贡献！如果您有好的想法或发现了问题，请随时提交 [Issues](https://github.com/your-username/PetNestIQ/issues) 或 [Pull Requests](https://github.com/your-username/PetNestIQ/pulls)。

## 📄 许可证

本项目采用 [MIT License](https://opensource.org/licenses/MIT) 授权。

---

*该文档最后更新于 2025年8月14日*
