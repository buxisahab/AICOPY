import React, { useState, useEffect, useRef } from 'react';
import { Howl } from 'howler';
import {
    PlayIcon,
    PauseIcon,
    SkipNextIcon,
    SkipPreviousIcon,
    ShuffleIcon,
    ShuffleOnIcon,
    RepeatIcon,
    RepeatOnIcon,
    RepeatOneIcon,
    FavoriteIcon,
    FavoriteBorderIcon,
    VolumeUpIcon,
    VolumeOffIcon
} from './Icons';
import './Player.css';

const Player = ({ track, isPlaying, setIsPlaying, onNext, onPrevious }) => {
    const [currentTime, setCurrentTime] = useState(0);
    const [duration, setDuration] = useState(0);
    const [volume, setVolume] = useState(0.7);
    const [isMuted, setIsMuted] = useState(false);
    const [previousVolume, setPreviousVolume] = useState(0.7);
    const [shuffle, setShuffle] = useState(false);
    const [repeat, setRepeat] = useState(0); // 0: off, 1: all, 2: one
    const [isLiked, setIsLiked] = useState(false);

    const soundRef = useRef(null);
    const progressInterval = useRef(null);

    // Initialize Howler when track changes
    useEffect(() => {
        if (track && track.url) {
            // Clean up previous sound
            if (soundRef.current) {
                soundRef.current.unload();
            }

            // Create new Howl instance
            soundRef.current = new Howl({
                src: [track.url],
                html5: true,
                volume: isMuted ? 0 : volume,
                onload: function () {
                    setDuration(this.duration());
                },
                onplay: function () {
                    requestAnimationFrame(updateProgress);
                },
                onend: function () {
                    if (repeat === 2) {
                        // Repeat one
                        this.seek(0);
                        this.play();
                    } else if (repeat === 1) {
                        // Repeat all - go to next
                        onNext();
                    } else {
                        // No repeat
                        setIsPlaying(false);
                        setCurrentTime(0);
                    }
                }
            });

            // Auto play if was playing
            if (isPlaying) {
                soundRef.current.play();
            }
        }

        return () => {
            if (soundRef.current) {
                soundRef.current.unload();
            }
        };
    }, [track]);

    // Handle play/pause state
    useEffect(() => {
        if (soundRef.current) {
            if (isPlaying) {
                soundRef.current.play();
            } else {
                soundRef.current.pause();
            }
        }
    }, [isPlaying]);

    // Handle volume changes
    useEffect(() => {
        if (soundRef.current) {
            soundRef.current.volume(isMuted ? 0 : volume);
        }
    }, [volume, isMuted]);

    const updateProgress = () => {
        if (soundRef.current && soundRef.current.playing()) {
            setCurrentTime(soundRef.current.seek());
            requestAnimationFrame(updateProgress);
        }
    };

    const handleSeek = (e) => {
        const newTime = parseFloat(e.target.value);
        setCurrentTime(newTime);
        if (soundRef.current) {
            soundRef.current.seek(newTime);
        }
    };

    const handleVolumeChange = (e) => {
        const newVolume = parseFloat(e.target.value);
        setVolume(newVolume);
        if (newVolume > 0) {
            setIsMuted(false);
        }
    };

    const toggleMute = () => {
        if (isMuted) {
            setIsMuted(false);
            setVolume(previousVolume);
        } else {
            setPreviousVolume(volume);
            setIsMuted(true);
        }
    };

    const toggleShuffle = () => {
        setShuffle(!shuffle);
    };

    const toggleRepeat = () => {
        setRepeat((repeat + 1) % 3);
    };

    const toggleLike = () => {
        setIsLiked(!isLiked);
    };

    const formatTime = (seconds) => {
        if (isNaN(seconds)) return '0:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    if (!track) return null;

    return (
        <div className="player">
            {/* Track Info Section */}
            <div className="player-track-info">
                <div className="track-artwork">
                    <img
                        src={track.thumbnail || '/placeholder-album.png'}
                        alt={track.title}
                        onError={(e) => { e.target.src = '/placeholder-album.png'; }}
                    />
                </div>
                <div className="track-details">
                    <h3 className="track-title truncate">{track.title}</h3>
                    <p className="track-artist truncate">{track.artist || 'Unknown Artist'}</p>
                </div>
                <button
                    className={`btn-icon btn-favorite ${isLiked ? 'liked' : ''}`}
                    onClick={toggleLike}
                    aria-label={isLiked ? 'Unlike' : 'Like'}
                >
                    {isLiked ? <FavoriteIcon /> : <FavoriteBorderIcon />}
                </button>
            </div>

            {/* Playback Controls Section */}
            <div className="player-controls">
                <div className="control-buttons">
                    <button
                        className={`btn-icon btn-shuffle ${shuffle ? 'active' : ''}`}
                        onClick={toggleShuffle}
                        aria-label="Shuffle"
                    >
                        {shuffle ? <ShuffleOnIcon /> : <ShuffleIcon />}
                    </button>

                    <button
                        className="btn-icon btn-previous"
                        onClick={onPrevious}
                        aria-label="Previous"
                    >
                        <SkipPreviousIcon />
                    </button>

                    <button
                        className="btn-icon btn-play-pause"
                        onClick={() => setIsPlaying(!isPlaying)}
                        aria-label={isPlaying ? 'Pause' : 'Play'}
                    >
                        {isPlaying ? <PauseIcon /> : <PlayIcon />}
                    </button>

                    <button
                        className="btn-icon btn-next"
                        onClick={onNext}
                        aria-label="Next"
                    >
                        <SkipNextIcon />
                    </button>

                    <button
                        className={`btn-icon btn-repeat ${repeat > 0 ? 'active' : ''}`}
                        onClick={toggleRepeat}
                        aria-label={repeat === 0 ? 'Repeat Off' : repeat === 1 ? 'Repeat All' : 'Repeat One'}
                    >
                        {repeat === 2 ? <RepeatOneIcon /> : repeat === 1 ? <RepeatOnIcon /> : <RepeatIcon />}
                    </button>
                </div>

                {/* Progress Bar */}
                <div className="progress-container">
                    <span className="time-current">{formatTime(currentTime)}</span>
                    <input
                        type="range"
                        className="progress-bar"
                        min="0"
                        max={duration || 0}
                        value={currentTime}
                        onChange={handleSeek}
                        step="0.1"
                        aria-label="Seek"
                    />
                    <span className="time-duration">{formatTime(duration)}</span>
                </div>
            </div>

            {/* Volume Control Section */}
            <div className="player-volume">
                <button
                    className="btn-icon btn-mute"
                    onClick={toggleMute}
                    aria-label={isMuted ? 'Unmute' : 'Mute'}
                >
                    {isMuted || volume === 0 ? <VolumeOffIcon /> : <VolumeUpIcon />}
                </button>
                <input
                    type="range"
                    className="volume-slider"
                    min="0"
                    max="1"
                    value={isMuted ? 0 : volume}
                    onChange={handleVolumeChange}
                    step="0.01"
                    aria-label="Volume"
                />
            </div>
        </div>
    );
};

export default Player;
