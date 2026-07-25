# 📄 Wear OS PDF Reader

**Developer & Author**: Aju George  
**Target Device**: Samsung Galaxy Watch 6 (Wear OS 4 / Android 13+)

A lightweight, high-performance, hardware-accelerated PDF Reader engineered specifically for Wear OS 4 smartwatches. Built to overcome smartwatch hardware limitations, this app bypasses Jetpack Compose recomposition overhead by leveraging a native Android View with C++ Skia hardware matrix transformations to deliver fluid 60 FPS panning, pinch-to-zoom, and crystal-clear document rendering on circular AMOLED displays.

---

### ✨ Key Features

- **⚡ 60 FPS Fluid Hardware Acceleration**: Uses `LAYER_TYPE_HARDWARE` Skia matrix transformations for ultra-smooth panning and pinch-to-zoom gestures without UI lag.
- **🔍 1080px Super-Sampled Anti-Aliasing (2.25x SSAA)**: Rasterizes PDF vector text at 1080px super-sampled resolution with bitmap anti-aliasing and dithering flags, ensuring vector text remains sharp on smartwatch screens.
- **🚀 Native File Explorer**: Scans internal watch storage (`/sdcard/Download`, `getExternalFilesDir`) automatically—no phone or internet required.
- **🔄 Intent Data Integration**: Opens PDFs directly when tapped inside Watch File Manager or third-party file explorers via `ACTION_VIEW` intent streams.
- **🚫 Anti-Gesture Swipe Protection**: Custom Wear OS theme with `android:windowSwipeToDismiss = false` prevents rightward pan swipes from accidentally triggering system back-gestures or closing the app during document reading.
- **🔋 AMOLED Dark Theme**: Pure black background designed for AMOLED smartwatch displays to save battery power during extended reading sessions.

---

### 👨‍💻 Author & Maintainer

Created with ❤️ by **Aju George**.

---

### 🛠️ Built With

- **Target OS**: Wear OS 4 / Android 13+ (API 33+)
- **Target Hardware**: Samsung Galaxy Watch 6 44mm (`SM-R940`), 480×480 px circular display
- **Language**: Kotlin 1.9 & Java 21
- **UI Framework**: Android Jetpack Compose for Wear OS & Native C++ Skia View (`AndroidView`)
- **Optimization**: R8 Bytecode Shrinking & LRU Memory Caching
