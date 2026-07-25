# Wear OS 4 PDF Reader for Samsung Galaxy Watch 6

A native Wear OS 4 application built with Jetpack Compose for Wear OS and Android's high-performance native `PdfRenderer`.

## Features
- **High Resolution Vector PDF Renderer**: Sharp text rendering tuned for Galaxy Watch 6 AMOLED displays (453 ppi).
- **Gesture Controls**: Pinch-to-zoom (up to 4x) and 2D drag panning.
- **Wear OS Navigation**: Compact page bar with page counter.
- **Rotating Bezel Support**: Smooth scrolling using Galaxy Watch 6 physical or digital rotating bezel.
- **Storage Access Framework (SAF)**: Pick any PDF document on watch storage or run sample PDF.

---

## How to Connect Samsung Galaxy Watch 6 via ADB Wi-Fi

1. **On your Samsung Galaxy Watch 6**:
   - Go to **Settings > About Watch > Software Info**.
   - Tap **Software Version** 7 times until Developer Mode is turned ON.
   - Go back to **Settings > Developer Options**.
   - Turn ON **Wireless Debugging**.
   - Tap **Wireless Debugging > Pair new device**.
   - Note the **Wi-Fi IP Address, Port, and Wi-Fi pairing code** displayed on watch screen.

2. **On your Computer Terminal**:
   ```bash
   # Pair with your watch (replace IP and PORT with watch values):
   adb pair <WATCH_IP>:<PAIR_PORT> <PAIRING_CODE>

   # Connect to your watch:
   adb connect <WATCH_IP>:<DEBUG_PORT>

   # Verify connection:
   adb devices
   ```

---

## Running in Android Studio

1. Launch Android Studio:
   ```bash
   studio
   ```
2. Click **Open** and select `/home/aju/WearOSPDFReader`.
3. Select your connected **Samsung Galaxy Watch 6** device target.
4. Click **Run (Shift + F10)**.
