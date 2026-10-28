const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
contextBridge.exposeInMainWorld('electron', {
    // Storage API
    store: {
        get: (key) => ipcRenderer.invoke('get-store-value', key),
        set: (key, value) => ipcRenderer.invoke('set-store-value', key, value),
        delete: (key) => ipcRenderer.invoke('delete-store-value', key)
    },

    // Window controls
    window: {
        minimize: () => ipcRenderer.invoke('minimize-window'),
        maximize: () => ipcRenderer.invoke('maximize-window'),
        close: () => ipcRenderer.invoke('close-window')
    },

    // Media control listeners
    onMediaControl: (callback) => {
        ipcRenderer.on('media-control', (event, action) => callback(action));
    }
});
