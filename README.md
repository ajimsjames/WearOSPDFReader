# 📄 Wear OS PDF Reader (v1.4.0)

**Sleek Offline PDF Document Viewer for Wear OS (Samsung Galaxy Watch 6)**

Developed by **Aju George**.

---

## ✨ Features

- 📄 **Offline Local Document Rendering**: Opens any PDF file stored directly on smartwatch internal memory (`/sdcard/Download`, `/sdcard/Documents`, or System File Picker).
- 🌙 **Dark Mode Document Inversion**: Inverts bright PDF pages into sleek OLED dark mode for comfortable reading and zero battery drain.
- ⚙️ **Rotary Bezel Scrolling & Zoom**: Smooth physical hardware crown/bezel scroll navigation and double-tap pinch zoom.
- ⭕ **Bezel-Aligned Navigation & About Dialog**: Curved top navigation bar (`CurvedLayout`) with About App screen and One UI squircle launcher icon.

---

## 🛠️ Architecture & Tech Stack

- **Framework**: Android Wear OS (Min SDK 30 / Target SDK 33)
- **UI Engine**: Wear Compose + Jetpack Compose + CurvedLayout
- **Rendering Engine**: Native Android PdfRenderer + Skia Color Matrix Inversion Filter.

---

## 📦 Installation

```bash
# Connect to Galaxy Watch 6 via Wireless ADB
adb connect <WATCH_IP>:<PORT>

# Build and Install Release APK
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📄 License & Credits

Created and maintained by **Aju George**. Distributed for Wear OS devices.
