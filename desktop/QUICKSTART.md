# 🚀 Quick Start Guide - AI Music Desktop

## Prerequisites

Before you begin, make sure you have:
- ✅ **Node.js** (v16 or higher) installed
- ✅ **npm** (comes with Node.js)

## Step 1: Install Dependencies

Open PowerShell or Command Prompt and navigate to the desktop folder:

```powershell
cd d:\Wondershare\Download\Ai-master\desktop
```

Install all required packages:

```powershell
# If you get execution policy error, use:
powershell -ExecutionPolicy Bypass -Command "npm install"

# Or simply:
npm install
```

This will install:
- Electron
- React & React Router
- Vite (build tool)
- Howler.js (audio playback)
- All other dependencies

## Step 2: Start Development Server

Once installation is complete, start the app:

```powershell
npm start
```

This command will:
1. Start the React development server on `http://localhost:3000`
2. Launch the Electron app automatically
3. Enable hot reload for instant updates

## Step 3: Explore the App

The app will open with these features:
- 🏠 **Home**: Quick picks and trending music
- 🔍 **Search**: Find songs, artists, and albums
- 📚 **Library**: Your music collection
- ⚙️ **Settings**: Customize your experience

## Building for Production

### Build React App

```powershell
npm run build
```

### Build Desktop App

```powershell
# For Windows
npm run build:win

# For macOS (requires macOS)
npm run build:mac

# For Linux
npm run build:linux
```

The built app will be in the `dist/` folder.

## Troubleshooting

### PowerShell Execution Policy Error

If you see "running scripts is disabled", use:

```powershell
powershell -ExecutionPolicy Bypass -Command "npm install"
```

Or set the execution policy (requires admin):

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Port Already in Use

If port 3000 is already in use:
1. Close any other apps using port 3000
2. Or modify `vite.config.js` to use a different port

### Node.js Not Found

Make sure Node.js is installed:
```powershell
node --version
npm --version
```

If not installed, download from: https://nodejs.org/

## Next Steps

1. **Customize the UI**: Edit files in `src/components/` and `src/pages/`
2. **Add Features**: Implement YouTube Music API integration
3. **Style Changes**: Modify CSS files in `src/styles/`
4. **Build Settings**: Update `electron/main.js` for app behavior

## Useful Commands

```powershell
# Start development
npm start

# Build React app
npm run build

# Build for Windows
npm run build:win

# Clean install (if issues)
rm -r node_modules
npm install
```

## File Structure

```
desktop/
├── electron/          # Electron main process
├── src/
│   ├── components/   # UI components
│   ├── pages/        # Page views
│   ├── styles/       # CSS files
│   └── App.jsx       # Main app
├── package.json      # Dependencies
└── vite.config.js    # Build config
```

## Support

For issues or questions:
- Check the main [README.md](README.md)
- Review the code comments
- Check console for errors (F12 in app)

---

**Happy Coding! 🎵**
