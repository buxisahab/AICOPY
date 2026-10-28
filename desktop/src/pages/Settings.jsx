import React from 'react';
import './Settings.css';

const Settings = ({ theme, setTheme }) => {
    const handleThemeChange = async (newTheme) => {
        setTheme(newTheme);
        if (window.electron) {
            await window.electron.store.set('theme', newTheme);
        }
    };

    return (
        <div className="settings-page fade-in">
            <header className="page-header">
                <h1 className="page-title">Settings</h1>
                <p className="page-subtitle">Customize your experience</p>
            </header>

            <div className="settings-sections">
                <section className="settings-section">
                    <h2 className="section-title">Appearance</h2>
                    <div className="settings-group">
                        <div className="setting-item">
                            <div className="setting-info">
                                <h3 className="setting-label">Theme</h3>
                                <p className="setting-description">Choose your preferred theme</p>
                            </div>
                            <select
                                className="setting-select"
                                value={theme}
                                onChange={(e) => handleThemeChange(e.target.value)}
                            >
                                <option value="dark">Dark</option>
                                <option value="light">Light</option>
                                <option value="system">System</option>
                            </select>
                        </div>
                    </div>
                </section>

                <section className="settings-section">
                    <h2 className="section-title">Playback</h2>
                    <div className="settings-group">
                        <div className="setting-item">
                            <div className="setting-info">
                                <h3 className="setting-label">Audio Quality</h3>
                                <p className="setting-description">Higher quality uses more data</p>
                            </div>
                            <select className="setting-select">
                                <option value="low">Low</option>
                                <option value="medium">Medium</option>
                                <option value="high">High</option>
                                <option value="highest">Highest</option>
                            </select>
                        </div>

                        <div className="setting-item">
                            <div className="setting-info">
                                <h3 className="setting-label">Crossfade</h3>
                                <p className="setting-description">Seamless transition between songs</p>
                            </div>
                            <label className="toggle-switch">
                                <input type="checkbox" />
                                <span className="toggle-slider"></span>
                            </label>
                        </div>

                        <div className="setting-item">
                            <div className="setting-info">
                                <h3 className="setting-label">Normalize Volume</h3>
                                <p className="setting-description">Set the same volume level for all songs</p>
                            </div>
                            <label className="toggle-switch">
                                <input type="checkbox" />
                                <span className="toggle-slider"></span>
                            </label>
                        </div>
                    </div>
                </section>

                <section className="settings-section">
                    <h2 className="section-title">About</h2>
                    <div className="settings-group">
                        <div className="about-info">
                            <div className="about-logo">🎵</div>
                            <h3 className="about-title">AI Music Desktop</h3>
                            <p className="about-version">Version 1.0.0</p>
                            <p className="about-description">
                                A beautiful desktop music player for YouTube Music
                            </p>
                            <div className="about-links">
                                <button className="btn-link">GitHub</button>
                                <button className="btn-link">Report Issue</button>
                                <button className="btn-link">Check for Updates</button>
                            </div>
                        </div>
                    </div>
                </section>
            </div>
        </div>
    );
};

export default Settings;
