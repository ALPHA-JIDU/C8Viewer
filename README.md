# Chat Viewer

A private, offline viewer for Facebook Messenger archive exports (ZIP or extracted folders) built with modern Android architecture.

---

## 🚀 How to Download Your APK (No Developer Setup Needed)

1. **Push or Upload this project to your GitHub repository.**
2. Go to the **Actions** tab at the top of your GitHub repository.
3. You will see a workflow running named **"Build Android APK"**.
4. When it finishes (green checkmark, usually ~2 minutes), click on the run.
5. Under the **Artifacts** section at the bottom of the page, click **`ChatViewer-Debug-APK`** to download your ready-to-install `.apk` file!
6. Transfer the `.apk` to your Android phone and tap to install.

---

## ✨ Features (Phase 1 – Phase 8)

- **Archive Support**: Import directly from one or multiple ZIP files or an already extracted folder (`OpenDocumentTree`).
- **Selective Chat Picker**: Pre-scans message counts and allows choosing specific chats to import.
- **Background WorkManager**: Foreground service with notification progress and screen wake lock.
- **Data Preservation**: User customizations (pins, bookmarks, nicknames, themes, categories) persist across re-imports.
- **Media Support**:
  - Fullscreen photo viewer with pinch-to-zoom and swipe between images.
  - In-app ExoPlayer video player and audio player with 1x, 1.5x, and 2x speed controls.
  - Save to device gallery and system sharing.
- **Search & Filter**: Search across all chats or within a single chat with date range, sender, and media type filters.
- **Privacy & Security**: Biometric (Fingerprint/Face) and PIN app lock. Hidden chats are password-protected.
- **Exporting**: Export entire chats or date ranges to PDF or Plain Text (`.txt`).
- **Modern UI**: Full Material 3 with Android 12+ dynamic wallpaper colors and edge-to-edge support.
- **Reading Continuity**: "Continue where you left off" jump banner and jump-to-date calendar picker.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0.21
- **UI Framework**: 100% Jetpack Compose with Material 3 Design System
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 15 (API 35)
- **Database**: Room 2.6.1 with SQLite FTS support
- **Background Tasks**: AndroidX WorkManager 2.10.0 CoroutineWorker
- **Media Engine**: AndroidX Media3 (ExoPlayer 1.5.1) & Coil 2.7.0
- **Security**: AndroidX Biometric 1.2.0 & Jetpack DataStore Preferences
