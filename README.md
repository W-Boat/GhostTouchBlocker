# Ghost Touch Blocker

An Android application that blocks touch events in selected screen regions using multiple methods.

## Features

- 🛡️ Block touch events in custom screen regions
- 🎨 Material 3 design with Monet dynamic theming
- 📱 Multiple blocking methods:
  - Accessibility Service (recommended)
  - System Alert Window overlay
  - Root support (future)
- 🎯 Region selection with visual feedback
- ⚙️ Full-screen or custom region blocking
- ✅ Permission check and setup guide

## Requirements

- Android 12 (API 31) or higher
- Overlay permission
- Accessibility service permission (recommended)
- Notification permission (Android 13+)

## Installation

Download the latest APK from [Releases](../../releases) or build from source.

## Usage

1. Grant required permissions through the setup guide
2. Select a region or choose full-screen blocking
3. Tap "Start Blocking" to activate
4. Tap "Stop Blocking" to deactivate

## Building

```bash
./gradlew assembleRelease
```

## License

MIT License
