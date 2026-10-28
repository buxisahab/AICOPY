import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import Player from './components/Player';
import Home from './pages/Home';
import Search from './pages/Search';
import Library from './pages/Library';
import Settings from './pages/Settings';
import './styles/App.css';

function App() {
    const [currentTrack, setCurrentTrack] = useState(null);
    const [isPlaying, setIsPlaying] = useState(false);
    const [playlist, setPlaylist] = useState([]);
    const [currentIndex, setCurrentIndex] = useState(0);

    const playTrack = (track, trackList = null) => {
        setCurrentTrack(track);
        setIsPlaying(true);

        // If a playlist is provided, set it
        if (trackList && trackList.length > 0) {
            setPlaylist(trackList);
            const index = trackList.findIndex(t => t.id === track.id);
            setCurrentIndex(index >= 0 ? index : 0);
        } else if (playlist.length === 0) {
            // If no playlist exists, create one with just this track
            setPlaylist([track]);
            setCurrentIndex(0);
        }
    };

    const handleNext = () => {
        if (playlist.length === 0) return;

        const nextIndex = (currentIndex + 1) % playlist.length;
        setCurrentIndex(nextIndex);
        setCurrentTrack(playlist[nextIndex]);
        setIsPlaying(true);
    };

    const handlePrevious = () => {
        if (playlist.length === 0) return;

        const prevIndex = currentIndex === 0 ? playlist.length - 1 : currentIndex - 1;
        setCurrentIndex(prevIndex);
        setCurrentTrack(playlist[prevIndex]);
        setIsPlaying(true);
    };

    return (
        <Router>
            <div className="app">
                <Sidebar />
                <main className="main-content">
                    <Routes>
                        <Route path="/" element={<Home playTrack={playTrack} />} />
                        <Route path="/search" element={<Search playTrack={playTrack} />} />
                        <Route path="/library" element={<Library playTrack={playTrack} />} />
                        <Route path="/settings" element={<Settings />} />
                    </Routes>
                </main>
                <Player
                    track={currentTrack}
                    isPlaying={isPlaying}
                    setIsPlaying={setIsPlaying}
                    onNext={handleNext}
                    onPrevious={handlePrevious}
                />
            </div>
        </Router>
    );
}

export default App;
