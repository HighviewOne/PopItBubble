# 🫧 PopItBubble

[![Build](https://github.com/HighviewOne/PopItBubble/actions/workflows/android.yml/badge.svg)](https://github.com/HighviewOne/PopItBubble/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-pink.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org)

A satisfying **Pop-It fidget sensory app** for Android. Tap or drag across the silicone-style bubbles to pop them — complete with 3D animations, haptic feedback, and satisfying pop sounds.

**[🌐 Live Demo & Landing Page →](https://highviewone.github.io/PopItBubble/)**

---

## Screenshots

| Inflated bubbles | Popped state | All popped! |
|---|---|---|
| *(Rainbow 5×5 grid)* | *(Concave popped bubbles)* | *(Celebration overlay)* |

> Real screenshots coming soon. Try the [interactive browser demo](https://highviewone.github.io/PopItBubble/#demo) in the meantime!

---

## Features

| Feature | Details |
|---|---|
| 🎨 **4 Color Themes** | Rainbow, Pink, Blue, Pastel |
| 📐 **4 Grid Sizes** | 4×4, 5×5, 6×6, 7×7 |
| 🔊 **Pop Sounds** | 4 programmatically-generated WAV variations with pitch randomization |
| 📳 **Haptic Feedback** | Crisp 25 ms vibration pulse on every pop |
| 👆 **Multi-Touch** | Drag multiple fingers to pop bubbles in one sweep |
| ✨ **3D Bubble Rendering** | RadialGradient dome with specular highlight using Canvas |
| 🎉 **Celebration** | Animated overlay + auto-reset when all bubbles are popped |
| 📊 **Pop Counter** | Live `X / Total` count in the header bar |

---

## How to Build

### Requirements
- Android Studio **Giraffe (2022.3.1)** or newer
- JDK 17
- Android SDK with **API level 35** platform

### Steps

```bash
git clone https://github.com/HighviewOne/PopItBubble.git
cd PopItBubble
```

Open the folder in **Android Studio** — it will sync Gradle automatically.

Then press **▶ Run** or build from the terminal:

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

The debug APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Project Structure

```
PopItBubble/
├── app/src/main/
│   ├── java/com/popitbubble/
│   │   ├── MainActivity.kt       # Toolbar, menu, counter, celebration
│   │   ├── BubbleGridView.kt     # Custom View — Canvas drawing, touch, animation
│   │   └── SoundManager.kt       # Programmatic WAV generation + SoundPool
│   └── res/
│       ├── layout/activity_main.xml
│       ├── menu/main_menu.xml    # Grid size + color theme options
│       └── values/               # Colors, strings, themes
└── docs/                         # GitHub Pages landing page
```

### Architecture

- **`BubbleGridView`** — single custom `View` that draws the entire grid on `Canvas`.
  Uses `RadialGradient` for the 3D dome effect and `ValueAnimator` with `OvershootInterpolator` for the pop spring-back.
- **`SoundManager`** — generates pop sounds at runtime using white noise + low-frequency tone + click transient, writes them to cache WAV files, and plays via `SoundPool` for low latency.
- **No third-party libraries** — only AndroidX + Material Components.

---

## Roadmap

- [ ] Record and add real screenshots / GIF
- [ ] Sound toggle (on/off)
- [ ] Haptic strength slider
- [ ] Timed challenge mode (pop all bubbles as fast as you can)
- [ ] High-score leaderboard
- [ ] Hexagonal grid layout
- [ ] Accessibility: screen reader support

---

## Download

Grab the latest debug APK from [Releases](https://github.com/HighviewOne/PopItBubble/releases/latest).

> Enable **Install from unknown sources** in Android Settings → Apps before installing.

---

## License

[MIT](LICENSE) © 2026 HighviewOne
