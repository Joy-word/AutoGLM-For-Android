# AutoGLM For Android

<div align="center">
<img src="screenshots/logo.svg" width="120"/>

**Native Android Phone AI Assistant Based on Open-AutoGLM**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org)

English | [中文](README.md)

</div>

---

## 📸 Screenshots

<div align="center">
<img src="screenshots/main_screen.jpg" width="200"/>
<img src="screenshots/settings.jpg" width="200"/>
<img src="screenshots/settings1.jpg" width="200"/>
</div>

<div align="center">
<img src="screenshots/history.jpg" width="200"/>
<img src="screenshots/history1.jpg" width="200"/>
<img src="screenshots/floating_window.jpg" width="200"/>
</div>

---

## 📖 Introduction

AutoGLM For Android is a native Android application developed based on the [Open-AutoGLM](https://github.com/zai-org/Open-AutoGLM) open-source project. It transforms the original phone automation solution that required a computer + ADB connection into a standalone app running directly on the phone, allowing users to control their phone using natural language.

**Key Features:**
- 🚀 **No Computer Required**: Runs directly on the phone without ADB connection
- 🎯 **Natural Language Control**: Describe tasks in natural language, AI executes automatically
- 🔒 **Shizuku Permissions**: Obtains necessary system permissions through Shizuku
- 🪟 **Floating Window**: Real-time display of task execution progress
- 📱 **Native Experience**: Material Design, smooth native Android experience
- 🔌 **Multi-Model Support**: Compatible with any model API supporting OpenAI format and image understanding

## 🏗️ Architecture Comparison

| Feature | Open-AutoGLM (Original) | AutoGLM For Android (This Project) |
|---------|-------------------------|-------------------------------------|
| Runtime | Computer (Python) | Phone (Android App) |
| Connection | Requires ADB/USB | No connection needed, standalone |
| Permissions | ADB shell commands | Shizuku service |
| Text Input | ADB Keyboard | Built-in AutoGLM Keyboard |
| User Interface | Command line | Native Android UI + Floating Window |
| Screenshot | ADB screencap | Shizuku shell commands |

## 📋 Features

### Core Features
- ✅ **Task Execution**: Input natural language task descriptions, AI automatically plans and executes
- ✅ **Screen Understanding**: Screenshot → Vision model analysis → Output action commands
- ✅ **Multiple Actions**: Click, swipe, long press, double tap, text input, launch apps, etc.
- ✅ **Task Control**: Pause, resume, cancel task execution
- ✅ **History**: Save task execution history, view details and screenshots

### User Interface
- ✅ **Main Screen**: Task input, status display, quick actions
- ✅ **Floating Window**: Real-time display of execution steps, thinking process, action results
- ✅ **Settings Page**: Model configuration, Agent parameters, multi-profile management
- ✅ **History Page**: Task history list, detail view, screenshot annotations

### Advanced Features
- ✅ **Multi-Model Profiles**: Save multiple model configurations, quick switching
- ✅ **Task Templates**: Save frequently used tasks, one-click execution
- ✅ **Custom Prompts**: Support custom system prompts
- ✅ **Quick Tile**: Notification bar quick tile, fast access to floating window

## 📱 Requirements

- **Android Version**: Android 7.0 (API 24) or higher
- **Required App**: [Shizuku](https://shizuku.rikka.app/) (for system permissions)
- **Network**: Connection to model API service (supports any OpenAI-compatible vision model)
- **Permissions**:
  - Overlay permission (for floating window)
  - Network permission (for API communication)
  - Shizuku permission (for system operations)

## 🚀 Quick Start

### 1. Install Shizuku

Shizuku is a tool that allows regular apps to use system APIs. This app relies on it to perform screen operations.

1. Download and install Shizuku from [Google Play](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or [GitHub](https://github.com/RikkaApps/Shizuku/releases)
2. Launch Shizuku and follow the guide to activate the service:
   - **Wireless Debugging** (Recommended): Enable Developer Options → Wireless Debugging → Pair Device
   - **ADB Method**: Connect to computer and run `adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh`
   - **Root Method**: If device is rooted, grant permission directly

### 2. Install AutoGLM For Android

1. Download the latest APK from [Releases](https://github.com/your-repo/releases)
2. Install the APK and open the app
3. Grant Shizuku permission (click "Request Permission" button)
4. Grant overlay permission (click "Grant Permission" button)
5. Enable AutoGLM Keyboard (click "Enable Keyboard" button)

### 3. Configure Model Service

Go to Settings and configure the model API. This app uses the standard **OpenAI API format** and supports any model that is compatible with this format and has image understanding capabilities.

**Model Requirements**:
- ✅ Compatible with OpenAI `/chat/completions` API format
- ✅ Supports multi-modal input (text + image)
- ✅ Can understand screenshots and output action commands

**Recommended Model Configurations**:

| Service | Base URL | Model | Get API Key |
|---------|----------|-------|-------------|
| Zhipu BigModel (Recommended) | `https://open.bigmodel.cn/api/paas/v4` | `autoglm-phone` | [Zhipu Open Platform](https://open.bigmodel.cn/) |
| ModelScope | `https://api-inference.modelscope.cn/v1` | `ZhipuAI/AutoGLM-Phone-9B` | [ModelScope](https://modelscope.cn/) |

**Using Other Third-Party Models**:

Any model service can be used as long as it meets these requirements:

1. **API Format Compatible**: Provides OpenAI-compatible `/chat/completions` endpoint
2. **Multi-modal Support**: Supports `image_url` format for image input
3. **Image Understanding**: Can analyze screenshots and understand UI elements

Examples of compatible services:
- OpenAI GPT-4V / GPT-4o (may need prompt adaptation)
- Claude 3 series (via compatibility layer)
- Other vision model APIs supporting OpenAI format

> ⚠️ **Note**: Non-AutoGLM models may require custom system prompts to output the correct action format. You can customize the system prompt in Settings → Advanced Settings.

### 4. Start Using

1. Enter a task description on the main screen, e.g., "Open WeChat and send a message to File Transfer: test"
2. Click "Start Task" button
3. The floating window will automatically appear, showing execution progress
4. Watch the AI's thinking process and execution actions

## 📖 User Guide

### Basic Operations

**Start a Task**:
1. Enter task description on the main screen or floating window
2. Click "Start" button
3. The app will automatically screenshot, analyze, and execute actions

**Control Tasks**:
- **Pause**: Click the pause button on the floating window, task will pause after current step
- **Resume**: Click resume button to continue execution
- **Stop**: Click stop button to cancel task

**View History**:
1. Click the history button on the main screen
2. View all executed tasks
3. Click a task to view detailed steps and screenshots

### Task Examples

```
# Social Communication
Open WeChat, search for John and send message: Hello

# Shopping Search
Open Taobao, search for wireless earphones, sort by sales

# Food Delivery
Open Meituan, search for nearby hotpot restaurants

# Navigation
Open Amap, navigate to the nearest subway station

# Video Entertainment
Open TikTok, browse 5 videos
```

### Advanced Features

**Save Model Configuration**:
1. Go to Settings → Model Configuration
2. After configuring parameters, click "Save Configuration"
3. Enter configuration name to save
4. You can quickly switch between different configurations later

**Create Task Templates**:
1. Go to Settings → Task Templates
2. Click "Add Template"
3. Enter template name and task description
4. Click template button on main screen for quick selection

**Custom System Prompts**:
1. Go to Settings → Advanced Settings
2. Edit system prompts
3. Add domain-specific instructions for enhancement

## 🛠️ Development Guide

### Environment Setup

**Development Tools**:
- Android Studio Hedgehog (2023.1.1) or higher
- JDK 11 or higher
- Kotlin 1.9.x

**Clone Project**:
```bash
git clone https://github.com/your-repo/AutoGLM-For-Android.git
cd AutoGLM-For-Android
```

**Open Project**:
1. Launch Android Studio
2. Select "Open an existing project"
3. Select project root directory
4. Wait for Gradle sync to complete

### Project Structure

```
app/src/main/java/com/kevinluo/autoglm/
├── action/                 # Action handling module
│   ├── ActionHandler.kt    # Action executor
│   ├── ActionParser.kt     # Action parser
│   └── AgentAction.kt      # Action data classes
├── agent/                  # Agent core module
│   ├── PhoneAgent.kt       # Phone Agent main class
│   └── AgentContext.kt     # Conversation context management
├── app/                    # App base module
│   ├── AppResolver.kt      # App name resolver
│   └── AutoGLMApplication.kt
├── config/                 # Configuration module
│   ├── I18n.kt             # Internationalization
│   └── SystemPrompts.kt    # System prompts
├── device/                 # Device operation module
│   └── DeviceExecutor.kt   # Device command executor
├── history/                # History module
│   ├── HistoryManager.kt   # History manager
│   └── HistoryActivity.kt  # History UI
├── input/                  # Input module
│   ├── TextInputManager.kt # Text input manager
│   └── AutoGLMKeyboardService.kt  # Built-in keyboard
├── model/                  # Model communication module
│   └── ModelClient.kt      # API client
├── screenshot/             # Screenshot module
│   └── ScreenshotService.kt # Screenshot service
├── settings/               # Settings module
│   ├── SettingsManager.kt  # Settings manager
│   └── SettingsActivity.kt # Settings UI
├── ui/                     # UI module
│   ├── FloatingWindowService.kt  # Floating window service
│   └── MainViewModel.kt    # Main screen ViewModel
├── util/                   # Utility module
│   ├── CoordinateConverter.kt    # Coordinate converter
│   ├── HumanizedSwipeGenerator.kt # Humanized swipe generator
│   └── Logger.kt           # Logger utility
├── ComponentManager.kt     # Component manager
├── MainActivity.kt         # Main activity
└── UserService.kt          # Shizuku user service
```

### Core Module Description

**PhoneAgent (agent/PhoneAgent.kt)**
- Core Agent class, responsible for task execution flow
- Manages screenshot → model request → action execution loop
- Supports pause, resume, cancel operations

**ModelClient (model/ModelClient.kt)**
- Communicates with model API
- Supports SSE streaming responses
- Parses thinking process and action commands

**ActionHandler (action/ActionHandler.kt)**
- Executes various device operations
- Coordinates DeviceExecutor, TextInputManager and other components
- Manages floating window show/hide

**DeviceExecutor (device/DeviceExecutor.kt)**
- Executes shell commands via Shizuku
- Implements click, swipe, key press and other operations
- Supports humanized swipe trajectories

**ScreenshotService (screenshot/ScreenshotService.kt)**
- Captures screen and compresses to WebP
- Automatically hides floating window to avoid interference
- Supports sensitive page detection

### Build and Debug

**Debug Build**:
```bash
./gradlew assembleDebug
```

**Release Build**:
```bash
./gradlew assembleRelease
```

**Run Tests**:
```bash
./gradlew test
```

**Install to Device**:
```bash
./gradlew installDebug
```

### Adding New Features

**Add New Action Type**:

1. Add new action class in `AgentAction.kt`:
```kotlin
data class NewAction(val param: String) : AgentAction() {
    override fun formatForDisplay(): String = "New Action: $param"
}
```

2. Add parsing logic in `ActionParser.kt`:
```kotlin
"NewAction" -> parseNewAction(response)
```

3. Add execution logic in `ActionHandler.kt`:
```kotlin
is AgentAction.NewAction -> executeNewAction(action)
```

**Add New Setting**:

1. Add key and methods in `SettingsManager.kt`:
```kotlin
private const val KEY_NEW_SETTING = "new_setting"

fun getNewSetting(): String = prefs.getString(KEY_NEW_SETTING, "") ?: ""
fun saveNewSetting(value: String) = prefs.edit().putString(KEY_NEW_SETTING, value).apply()
```

2. Add corresponding UI in settings screen

## 🔧 FAQ

### Shizuku Related

**Q: Shizuku shows not running?**
A: 
1. Make sure Shizuku app is installed and opened
2. Follow the guide in Shizuku to activate the service
3. Wireless debugging method is recommended, no Root required

**Q: Shizuku becomes invalid after phone restart?**
A: 
- Wireless debugging method requires re-pairing
- Consider using Root method for permanent activation
- Or set up auto-start script

### Permission Related

**Q: Cannot grant overlay permission?**
A: 
1. Go to System Settings → Apps → AutoGLM → Permissions
2. Find "Display over other apps" and enable it
3. Some systems require setting in "Special permissions"

**Q: Cannot enable keyboard?**
A: 
1. Go to System Settings → Language & Input → Manage Keyboards
2. Find "AutoGLM Keyboard" and enable it
3. No need to set as default keyboard, app will switch automatically

### Execution Related

**Q: Click action not working?**
A: 
1. Check if Shizuku is running properly
2. Some systems require enabling "USB debugging (Security settings)"
3. Try restarting Shizuku service

**Q: Text input failed?**
A: 
1. Make sure AutoGLM Keyboard is enabled
2. Check if target input field has focus
3. Check logs to confirm keyboard switching succeeded

**Q: Screenshot shows black screen?**
A: 
- This is usually normal protection for sensitive pages (payment, password, etc.)
- App will automatically detect and mark as sensitive screenshot

## 📄 License

This project is licensed under [Apache License 2.0](LICENSE).

## 🙏 Acknowledgments

- [Open-AutoGLM](https://github.com/zai-org/Open-AutoGLM) - Original open-source project
- [Shizuku](https://github.com/RikkaApps/Shizuku) - System permission framework
- [Zhipu AI](https://www.zhipuai.cn/) - AutoGLM model provider

## 📞 Contact

- Issues: [GitHub Issues](https://github.com/your-repo/issues)
- Email: luokavin@foxmail.com

---

<div align="center">

**If this project helps you, please give it a ⭐ Star!**

</div>
