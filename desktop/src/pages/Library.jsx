import React, { useState, useEffect } from 'react';
import './Library.css';

const Library = ({ playTrack }) => {
    const [activeTab, setActiveTab] = useState('songs');
    const [songs, setSongs] = useState([]);
    const [playlists, setPlaylists] = useState([]);
    const [albums, setAlbums] = useState([]);

    useEffect(() => {
        // Mock data
        setSongs([
            { id: 1, title: 'Favorite Song 1', artist: 'Artist 1', thumbnail: '/placeholder.jpg', url: '' },
            { id: 2, title: 'Favorite Song 2', artist: 'Artist 2', thumbnail: '/placeholder.jpg', url: '' },
        ]);

        setPlaylists([
            { id: 1, name: 'My Playlist 1', count: 25, thumbnail: '/placeholder.jpg' },
            { id: 2, name: 'My Playlist 2', count: 15, thumbnail: '/placeholder.jpg' },
        ]);

        setAlbums([
            { id: 1, name: 'Album 1', artist: 'Artist 1', thumbnail: '/placeholder.jpg' },
            { id: 2, name: 'Album 2', artist: 'Artist 2', thumbnail: '/placeholder.jpg' },
        ]);
    }, []);

    const tabs = [
        { id: 'songs', label: 'Songs', icon: '🎵' },
        { id: 'playlists', label: 'Playlists', icon: '📋' },
        { id: 'albums', label: 'Albums', icon: '💿' },
    ];

    return (
        <div className="library-page fade-in">
            <header className="page-header">
                <h1 className="page-title">Library</h1>
                <p className="page-subtitle">Your music collection</p>
            </header>

            <div className="library-tabs">
                {tabs.map((tab) => (
                    <button
                        key={tab.id}
                        className={`tab-button ${activeTab === tab.id ? 'active' : ''}`}
                        onClick={() => setActiveTab(tab.id)}
                    >
                        <span className="tab-icon">{tab.icon}</span>
                        <span className="tab-label">{tab.label}</span>
                    </button>
                ))}
            </div>

            <div className="library-content">
                {activeTab === 'songs' && (
                    <div className="songs-list">
                        {songs.map((song) => (
                            <div
                                key={song.id}
                                className="library-item"
                                onClick={() => playTrack(song, songs)}
                            >
                                <div className="item-artwork">
                                    <img src={song.thumbnail} alt={song.title} />
                                </div>
                                <div className="item-info">
                                    <h3 className="item-title">{song.title}</h3>
                                    <p className="item-subtitle">{song.artist}</p>
                                </div>
                                <button className="btn-more">⋮</button>
                            </div>
                        ))}
                    </div>
                )}

                {activeTab === 'playlists' && (
                    <div className="playlists-grid">
                        {playlists.map((playlist) => (
                            <div key={playlist.id} className="playlist-card">
                                <div className="playlist-artwork">
                                    <img src={playlist.thumbnail} alt={playlist.name} />
                                </div>
                                <div className="playlist-info">
                                    <h3 className="playlist-name">{playlist.name}</h3>
                                    <p className="playlist-count">{playlist.count} songs</p>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {activeTab === 'albums' && (
                    <div className="albums-grid">
                        {albums.map((album) => (
                            <div key={album.id} className="album-card">
                                <div className="album-artwork">
                                    <img src={album.thumbnail} alt={album.name} />
                                </div>
                                <div className="album-info">
                                    <h3 className="album-name">{album.name}</h3>
                                    <p className="album-artist">{album.artist}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default Library;
