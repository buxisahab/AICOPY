import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
    HomeIcon,
    SearchIcon,
    LibraryMusicIcon,
    SettingsIcon,
    MusicNoteIcon
} from './Icons';
import './Sidebar.css';

const Sidebar = () => {
    const location = useLocation();

    const isActive = (path) => {
        return location.pathname === path;
    };

    return (
        <div className="sidebar">
            <div className="sidebar-content">
                {/* App Header */}
                <div className="sidebar-header">
                    <div className="app-logo">
                        <div className="logo-icon">
                            <MusicNoteIcon />
                        </div>
                        <h1 className="app-title">AI Music</h1>
                    </div>
                </div>

                {/* Navigation */}
                <nav className="sidebar-nav">
                    <Link
                        to="/"
                        className={`nav-item ${isActive('/') ? 'active' : ''}`}
                    >
                        <HomeIcon />
                        <span>For You</span>
                    </Link>

                    <Link
                        to="/search"
                        className={`nav-item ${isActive('/search') ? 'active' : ''}`}
                    >
                        <SearchIcon />
                        <span>Search</span>
                    </Link>

                    <Link
                        to="/library"
                        className={`nav-item ${isActive('/library') ? 'active' : ''}`}
                    >
                        <LibraryMusicIcon />
                        <span>Library</span>
                    </Link>

                    <Link
                        to="/settings"
                        className={`nav-item ${isActive('/settings') ? 'active' : ''}`}
                    >
                        <SettingsIcon />
                        <span>Settings</span>
                    </Link>
                </nav>

                {/* Footer */}
                <div className="sidebar-footer">
                    <p className="footer-text">AI Music Desktop</p>
                    <p className="footer-version">v1.0.0</p>
                </div>
            </div>
        </div>
    );
};

export default Sidebar;
