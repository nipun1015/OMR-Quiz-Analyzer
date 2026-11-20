# OMR-Quiz-Analyzer (OMRScanner)

An Android-based OMR (Optical Mark Recognition) application built with OpenCV to scan and evaluate quiz answer sheets in real-time. This project handles bubble detection, alignment correction, and automated grading.

## 🚀 Features
- **Real-time Scanning**: Uses CameraX for smooth camera integration.
- **Computer Vision**: Powered by OpenCV for robust bubble detection and image processing.
- **Exam Management**: Create exams, define answer keys, and manage scan sessions.
- **Automated Grading**: Instant calculation of scores based on scanned OMR sheets.
- **Export Results**: Save and export results for further analysis.
- **Performance Fixes**: Includes custom fixes for camera freeze and multiple detection issues.

## 🛠️ Project Structure
- `app/src/main/java`: Core logic including OMR processing and UI activities.
- `app/src/main/assets`: Project assets (OMR formats, app icons).
- `app/src/main/res`: Layouts, strings, and theme resources.
- `gradle/`: Dependency management using Version Catalogs (`libs.versions.toml`).

## 📋 Prerequisites
- **JDK**: Version 11 or higher.
- **Android SDK**: API Level 26 (Android 8.0) or higher.
- **Environment**: `ANDROID_HOME` variable set in your system.

## 🏃 Getting Started

### 🅰️ Using Android Studio
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/OMR-Quiz-Analyzer.git
   ```
2. **Open Project**: Select "Open an Existing Project" and choose the `OMR-Quiz-Analyzer` folder.
3. **Sync & Run**: Wait for Gradle sync to finish, connect a device, and click the **Run** button.

### 🟦 Using VS Code
1. **Extensions**: Install "Extension Pack for Java", "Gradle for Java", and "Android iOS Support".
2. **Open Folder**: Open the `OMR-Quiz-Analyzer` folder.
3. **Build via Terminal**:
   ```bash
   ./gradlew assembleDebug
   ```
4. **Install APK**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 📲 Direct APK Download
If you want to test the app directly on your device:

1. **Locate the APK**:
   If you built the project locally, the APK is located at: `app/build/outputs/apk/debug/app-debug.apk`
2. **Transfer and Install**:
   - Transfer the `.apk` file to your Android device.
   - Open the file on your device.
   - If prompted, allow "Install from Unknown Sources" in your settings.
   - Follow the installation prompts.

## 📦 Dependencies
Key libraries used in this project:
- **OpenCV (4.12.0)**: Image processing and contour detection.
- **CameraX**: Camera lifecycle management and image analysis.
- **Material Components**: UI/UX design.

## 📄 Documentation
For specific fixes regarding camera performance, see:
[CAMERA_FREEZE_AND_MULTIPLE_DETECTION_FIX.md](./CAMERA_FREEZE_AND_MULTIPLE_DETECTION_FIX.md)

---
