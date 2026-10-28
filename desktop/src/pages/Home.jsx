import React, { useState, useEffect } from 'react';
import YouTubeMusicAPI from '../services/YouTubeMusicAPI';
import { PlayIcon, PauseIcon } from '../components/Icons';
import './Home.css';

const Home = ({ playTrack }) => {
    const [homeData, setHomeData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [currentPlayingId, setCurrentPlayingId] = useState(null);

    useEffect(() => {
        loadHomeData();
    }, []);

    const loadHomeData = async () => {
        setLoading(true);
        try {
            const data = await YouTubeMusicAPI.getHomePage();
            setHomeData(data);
        } catch (error) {
            console.error('Error loading home data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handlePlayTrack = (track) => {
        setCurrentPlayingId(track.id);
        playTrack(track);
    };

    if (loading) {
        return (
            <div className="home-page">
                <div className="loading-container">
                    <div className="spinner"></div>
                    <p>Loading your music...</p>
                </div>
            </div>
        );
    }

    if (!homeData) {
        return (
            <div className="home-page">
                <div className="error-container">
                    <p>Failed to load music. Please try again.</p>
                    <button onClick={loadHomeData} className="retry-button">Retry</button>
                </div>
            </div>
        );
    }

    return (
        <div className="home-page">
            {/* Quick Picks Section */}
            {homeData.quickPicks && homeData.quickPicks.length > 0 && (
                <section className="music-section">
                    <div className="section-header">
                        <h2 className="section-title">Quick Picks</h2>
                        <p className="section-subtitle">Songs we think you'll love</p>
                    </div>
                    <div className="music-grid">
                        {homeData.quickPicks.map((track) => (
                            <div
                                key={track.id}
                                className="music-card"
                                onClick={() => handlePlayTrack(track)}
                            >
                                <div className="card-artwork">
                                    <img
                                        src={track.thumbnail}
                                        alt={track.title}
                                        onError={(e) => { e.target.src = '/placeholder-album.png'; }}
                                    />
                                    <div className="play-overlay">
                                        <button className="play-button">
                                            <PlayIcon />
                                        </button>
                                    </div>
                                </div>
                                <div className="card-info">
                                    <h3 className="card-title truncate">{track.title}</h3>
                                    <p className="card-artist truncate">{track.artist}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>
            )}

            {/* Trending Now Section */}
            {homeData.trendingNow && homeData.trendingNow.length > 0 && (
                <section className="music-section">
                    <div className="section-header">
                        <h2 className="section-title">Trending Now</h2>
                        <p className="section-subtitle">What's hot right now</p>
                    </div>
                    <div className="music-list">
                        {homeData.trendingNow.map((track, index) => (
                            <div
                                key={track.id}
                                className="list-item"
                                onClick={() => handlePlayTrack(track)}
                            >
                                <div className="list-index">{index + 1}</div>
                                <div className="list-artwork">
                                    <img
                                        src={track.thumbnail}
                                        alt={track.title}
                                        onError={(e) => { e.target.src = '/placeholder-album.png'; }}
                                    />
                                </div>
                                <div className="list-info">
                                    <h3 className="list-title truncate">{track.title}</h3>
                                    <p className="list-artist truncate">{track.artist}</p>
                                </div>
                                <div className="list-duration">
                                    {Math.floor(track.duration / 60)}:{(track.duration % 60).toString().padStart(2, '0')}
                                </div>
                                <button className="list-play-button">
                                    <PlayIcon />
                                </button>
                            </div>
                        ))}
                    </div>
                </section>
            )}

            {/* Forgotten Favorites Section */}
            {homeData.forgottenFavorites && homeData.forgottenFavorites.length > 0 && (
                <section className="music-section">
                    <div className="section-header">
                        <h2 className="section-title">Forgotten Favorites</h2>
                        <p className="section-subtitle">Rediscover songs you loved</p>
                    </div>
                    <div className="music-list">
                        {homeData.forgottenFavorites.map((track, index) => (
                            <div
                                key={track.id}
                                className="list-item"
                                onClick={() => handlePlayTrack(track)}
                            >
                                <div className="list-index">{index + 1}</div>
                                <div className="list-artwork">
                                    <img
                                        src={track.thumbnail}
                                        alt={track.title}
                                        onError={(e) => { e.target.src = '/placeholder-album.png'; }}
                                    />
                                </div>
                                <div className="list-info">
                                    <h3 className="list-title truncate">{track.title}</h3>
                                    <p className="list-artist truncate">{track.artist}</p>
                                </div>
                                <div className="list-duration">
                                    {Math.floor(track.duration / 60)}:{(track.duration % 60).toString().padStart(2, '0')}
                                </div>
                                <button className="list-play-button">
                                    <PlayIcon />
                                </button>
                            </div>
                        ))}
                    </div>
                </section>
            )}

            {/* New Release Albums Section */}
            {homeData.newReleaseAlbums && homeData.newReleaseAlbums.length > 0 && (
                <section className="music-section">
                    <div className="section-header">
                        <h2 className="section-title">New Release Albums</h2>
                        <p className="section-subtitle">Fresh music for you</p>
                    </div>
                    <div className="album-grid">
                        {homeData.newReleaseAlbums.map((album) => (
                            <div
                                key={album.id}
                                className="album-card"
                            >
                                <div className="album-artwork">
                                    <img
                                        src={album.thumbnail}
                                        alt={album.title}
                                        onError={(e) => { e.target.src = '/placeholder-album.png'; }}
                                    />
                                </div>
                                <div className="album-info">
                                    <h3 className="album-title truncate">{album.title}</h3>
                                    <p className="album-artist truncate">{album.artist}</p>
                                    <p className="album-year">{album.year}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>
            )}
        </div>
    );
};

export default Home;
