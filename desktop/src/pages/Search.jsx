import React, { useState } from 'react';
import YouTubeMusicAPI from '../services/YouTubeMusicAPI';
import { SearchIcon, CloseIcon, PlayIcon } from '../components/Icons';
import './Search.css';

const Search = ({ playTrack }) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleSearch = async (searchQuery) => {
        if (!searchQuery.trim()) {
            setResults(null);
            return;
        }

        setLoading(true);
        try {
            const data = await YouTubeMusicAPI.search(searchQuery);
            setResults(data);
        } catch (error) {
            console.error('Search error:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (e) => {
        const value = e.target.value;
        setQuery(value);

        // Debounce search
        if (value.trim()) {
            setTimeout(() => handleSearch(value), 500);
        } else {
            setResults(null);
        }
    };

    const handleClear = () => {
        setQuery('');
        setResults(null);
    };

    const handlePlayTrack = (track) => {
        playTrack(track);
    };

    return (
        <div className="search-page">
            {/* Search Header */}
            <div className="search-header">
                <div className="search-input-container">
                    <SearchIcon />
                    <input
                        type="text"
                        className="search-input"
                        placeholder="Search for songs, artists, albums..."
                        value={query}
                        onChange={handleInputChange}
                        autoFocus
                    />
                    {query && (
                        <button className="clear-button" onClick={handleClear}>
                            <CloseIcon />
                        </button>
                    )}
                </div>
            </div>

            {/* Search Results */}
            <div className="search-content">
                {loading && (
                    <div className="search-loading">
                        <div className="spinner"></div>
                        <p>Searching...</p>
                    </div>
                )}

                {!loading && !results && !query && (
                    <div className="search-empty">
                        <SearchIcon />
                        <h3>Search for music</h3>
                        <p>Find your favorite songs, artists, and albums</p>
                    </div>
                )}

                {!loading && results && (
                    <>
                        {/* Songs Results */}
                        {results.songs && results.songs.length > 0 && (
                            <section className="results-section">
                                <h2 className="results-title">Songs</h2>
                                <div className="results-list">
                                    {results.songs.map((track) => (
                                        <div
                                            key={track.id}
                                            className="result-item"
                                            onClick={() => handlePlayTrack(track)}
                                        >
                                            <div className="result-artwork">
                                                <img
                                                    src={track.thumbnail}
                                                    alt={track.title}
                                                    onError={(e) => { e.target.src = '/placeholder-album.png'; }}
                                                />
                                                <div className="result-play-overlay">
                                                    <PlayIcon />
                                                </div>
                                            </div>
                                            <div className="result-info">
                                                <h3 className="result-title truncate">{track.title}</h3>
                                                <p className="result-artist truncate">{track.artist}</p>
                                            </div>
                                            <div className="result-duration">
                                                {Math.floor(track.duration / 60)}:{(track.duration % 60).toString().padStart(2, '0')}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </section>
                        )}

                        {/* Albums Results */}
                        {results.albums && results.albums.length > 0 && (
                            <section className="results-section">
                                <h2 className="results-title">Albums</h2>
                                <div className="results-grid">
                                    {results.albums.map((album) => (
                                        <div key={album.id} className="result-card">
                                            <div className="result-card-artwork">
                                                <img
                                                    src={album.thumbnail}
                                                    alt={album.title}
                                                    onError={(e) => { e.target.src = '/placeholder-album.png'; }}
                                                />
                                            </div>
                                            <h3 className="result-card-title truncate">{album.title}</h3>
                                            <p className="result-card-artist truncate">{album.artist}</p>
                                        </div>
                                    ))}
                                </div>
                            </section>
                        )}

                        {/* No Results */}
                        {results.songs?.length === 0 && results.albums?.length === 0 && (
                            <div className="no-results">
                                <h3>No results found</h3>
                                <p>Try searching with different keywords</p>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default Search;
