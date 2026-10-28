# 🎵 AI Music Desktop

<div align="center">

![AI Music Desktop](https://img.shields.io/badge/AI%20Music-Desktop-1DB954?style=for-the-badge&logo=electron)
![Version](https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge)
![License](https://img.shields.io/badge/license-GPL--3.0-green?style=for-the-badge)

**A beautiful, modern desktop music player for YouTube Music**

Built with Electron, React, and Material Design 3

[Features](#-features) • [Installation](#-installation) • [Development](#-development) • [Build](#-build)

</div>

---

## ✨ Features

### 🎨 **Modern UI/UX**
- **Material Design 3** inspired interface
- **Dark theme** with premium aesthetics
- **Smooth animations** and transitions
- **Glassmorphism** effects
- **Responsive design** for all screen sizes

### 🎵 **Music Player**
- **Full playback controls** (play, pause, next, previous)
- **Progress tracking** with seekable timeline
- **Volume control** with mute functionality
- **Repeat & Shuffle** modes
- **Media key support** (keyboard shortcuts)

### 📚 **Library Management**
- **Songs** collection
- **Playlists** management
- **Albums** view
- **Search** functionality
- **Recently played** tracking

### ⚙️ **Customization**
- **Theme selection** (Dark/Light/System)
- **Audio quality** settings
- **Crossfade** between tracks
- **Volume normalization**
- **Persistent settings** storage

---

## 📦 Installation

### Prerequisites

- **Node.js** (v16 or higher)
- **npm** or **yarn**

### Quick Start

```bash
# Navigate to desktop directory
cd desktop

# Install dependencies
npm install

# Start development server
npm start
```

The app will open automatically in development mode.

---

## 🛠️ Development

### Project Structure

```
desktop/
├── electron/           # Electron main process
│   ├── main.js        # Main window & app lifecycle
│   └── preload.js     # Secure IPC bridge
├── src/
│   ├── components/    # React components
│   │   ├── Sidebar.jsx
│   │   └── Player.jsx
│   ├── pages/         # Page components
│   │   ├── Home.jsx
│   │   ├── Search.jsx
│   │   ├── Library.jsx
│   │   └── Settings.jsx
│   ├── styles/        # CSS files
│   │   ├── index.css  # Global styles
│   │   └── App.css    # App layout
│   ├── App.jsx        # Main app component
│   └── main.jsx       # React entry point
├── index.html         # HTML entry point
├── package.json       # Dependencies
└── vite.config.js     # Vite configuration
```

### Available Scripts

```bash
# Start development server
npm start

# Build React app
npm run build

# Build for Windows
npm run build:win

# Build for macOS
npm run build:mac

# Build for Linux
npm run build:linux

# Package for all platforms
npm run package
```

### Development Mode

The app runs in two processes:
1. **React Dev Server** (Vite) on `http://localhost:3000`
2. **Electron Main Process** loads the dev server

Hot reload is enabled for instant updates during development.

---

## 📦 Build

### Build for Production

```bash
# Build React app first
npm run build

# Then build for your platform
npm run build:win    # Windows (NSIS installer)
npm run build:mac    # macOS (DMG)
npm run build:linux  # Linux (AppImage, deb)
```

### Build Output

Built applications will be in the `dist/` directory:
- **Windows**: `AI Music Setup 1.0.0.exe`
- **macOS**: `AI Music-1.0.0.dmg`
- **Linux**: `AI Music-1.0.0.AppImage` or `.deb`

---

## 🎨 Tech Stack

| Category | Technology |
|----------|-----------|
| **Framework** | Electron 28 |
| **UI Library** | React 18 |
| **Build Tool** | Vite 5 |
| **Routing** | React Router 6 |
| **Audio** | Howler.js |
| **HTTP Client** | Axios |
| **Storage** | electron-store |

---

## 🎯 Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Space` | Play/Pause |
| `Media Play/Pause` | Play/Pause |
| `Media Next Track` | Next Song |
| `Media Previous Track` | Previous Song |
| `Ctrl/Cmd + F` | Focus Search |

---

## 🔧 Configuration

Settings are stored locally using `electron-store`:
- **Theme preference**
- **Audio quality**
- **Playback settings**
- **User preferences**

Config location:
- **Windows**: `%APPDATA%\ai-music-desktop\config.json`
- **macOS**: `~/Library/Application Support/ai-music-desktop/config.json`
- **Linux**: `~/.config/ai-music-desktop/config.json`

---

## 🐛 Known Issues

- YouTube Music API integration is mocked (needs real implementation)
- Audio streaming requires actual YouTube Music backend
- Some features are UI-only placeholders

---

## 🚀 Roadmap

- [ ] YouTube Music API integration
- [ ] Lyrics display
- [ ] Download for offline playback
- [ ] Discord Rich Presence
- [ ] Last.fm scrobbling
- [ ] Custom themes
- [ ] Equalizer
- [ ] Mini player mode

---

## 📝 License

This project is licensed under the **GPL-3.0 License** - see the [LICENSE](../LICENSE) file for details.

---

## 👨‍💻 Author

**Based on the Android app by Arturo Cervantes**

Desktop version created with ❤️ using Electron + React

---

## 🙏 Acknowledgments

- Original Android app: [AI Music (OpenTune)](https://github.com/Arturo254/OpenTune)
- Material Design 3 guidelines
- Electron community
- React community

---

<div align="center">

**Made with 🎵 for music lovers**

[Report Bug](https://github.com/yourusername/ai-music/issues) • [Request Feature](https://github.com/yourusername/ai-music/issues)

</div>
