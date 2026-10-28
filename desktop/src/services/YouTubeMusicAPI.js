// Real YouTube Music API using ytmusic-api package
import YTMusic from 'ytmusic-api';
import YouTubeSr from 'youtube-sr';

class YouTubeMusicService {
    constructor() {
        this.ytmusic = null;
        this.initialized = false;
        this.initPromise = null;
    }

    async initialize() {
        if (this.initialized) return;
        if (this.initPromise) return this.initPromise;

        this.initPromise = (async () => {
            try {
                this.ytmusic = new YTMusic();
                await this.ytmusic.initialize();
                this.initialized = true;
                console.log('✅ YouTube Music API initialized successfully');
            } catch (error) {
                console.error('❌ Failed to initialize YouTube Music API:', error);
                this.initialized = false;
            }
        })();

        return this.initPromise;
    }

    async getHomePage() {
        try {
            await this.initialize();

            if (!this.initialized) {
                console.log('Using fallback data...');
                return this.getFallbackData();
            }

            // Get home feed from YouTube Music
            const home = await this.ytmusic.getHome();

            const quickPicks = [];
            const trending = [];
            const newReleases = [];

            // Parse home sections
            if (home && home.sections) {
                for (const section of home.sections) {
                    const title = section.title || '';
                    const items = section.items || [];

                    for (const item of items.slice(0, 12)) {
                        const song = this.parseSongItem(item);
                        if (song) {
                            if (title.toLowerCase().includes('quick') || title.toLowerCase().includes('mix') || title.toLowerCase().includes('recommend')) {
                                quickPicks.push(song);
                            } else if (title.toLowerCase().includes('trend') || title.toLowerCase().includes('chart')) {
                                trending.push(song);
                            } else if (title.toLowerCase().includes('new') || title.toLowerCase().includes('release')) {
                                newReleases.push(song);
                            } else {
                                quickPicks.push(song); // Default to quick picks
                            }
                        }
                    }
                }
            }

            return {
                quickPicks: quickPicks.slice(0, 12),
                trendingNow: trending.slice(0, 10),
                forgottenFavorites: [],
                keepListening: [],
                similarRecommendations: [],
                newReleaseAlbums: newReleases.slice(0, 8)
            };
        } catch (error) {
            console.error('Error fetching home page:', error);
            return this.getFallbackData();
        }
    }

    parseSongItem(item) {
        try {
            if (!item) return null;

            const videoId = item.videoId || item.id;
            const title = item.name || item.title || 'Unknown';
            const artist = item.artist?.name || item.artists?.[0]?.name || 'Unknown Artist';
            const album = item.album?.name || '';
            const thumbnail = item.thumbnails?.[0]?.url || item.thumbnail?.url || '';
            const duration = item.duration || 0;

            if (!videoId) return null;

            return {
                id: videoId,
                title,
                artist,
                album,
                thumbnail: thumbnail.replace('w60-h60', 'w500-h500').replace('w120-h120', 'w500-h500'),
                duration,
                url: `https://www.youtube.com/watch?v=${videoId}`,
                videoId,
                type: 'song'
            };
        } catch (error) {
            return null;
        }
    }

    async search(query) {
        if (!query) return { songs: [], albums: [], artists: [], playlists: [] };

        try {
            await this.initialize();

            if (!this.initialized) {
                // Use youtube-sr as fallback
                const results = await YouTubeSr.search(query, { limit: 20, type: 'video' });
                const songs = results.map(video => ({
                    id: video.id,
                    title: video.title,
                    artist: video.channel?.name || 'Unknown Artist',
                    album: '',
                    thumbnail: video.thumbnail?.url || '',
                    duration: video.duration / 1000, // Convert to seconds
                    url: video.url,
                    videoId: video.id,
                    type: 'song'
                }));

                return { songs, albums: [], artists: [], playlists: [] };
            }

            const results = await this.ytmusic.search(query);
            const songs = [];

            if (results && results.length > 0) {
                for (const item of results.slice(0, 20)) {
                    const song = this.parseSongItem(item);
                    if (song) songs.push(song);
                }
            }

            return { songs, albums: [], artists: [], playlists: [] };
        } catch (error) {
            console.error('Search error:', error);

            // Fallback to youtube-sr
            try {
                const results = await YouTubeSr.search(query, { limit: 20, type: 'video' });
                const songs = results.map(video => ({
                    id: video.id,
                    title: video.title,
                    artist: video.channel?.name || 'Unknown Artist',
                    album: '',
                    thumbnail: video.thumbnail?.url || '',
                    duration: video.duration / 1000,
                    url: video.url,
                    videoId: video.id,
                    type: 'song'
                }));

                return { songs, albums: [], artists: [], playlists: [] };
            } catch (fallbackError) {
                return { songs: [], albums: [], artists: [], playlists: [] };
            }
        }
    }

    getFallbackData() {
        // Fallback with real YouTube video IDs
        return {
            quickPicks: [
                {
                    id: 'dQw4w9WgXcQ',
                    title: 'Never Gonna Give You Up',
                    artist: 'Rick Astley',
                    album: 'Whenever You Need Somebody',
                    thumbnail: 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg',
                    duration: 213,
                    url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
                    videoId: 'dQw4w9WgXcQ',
                    type: 'song'
                },
                {
                    id: '4NRXx6U8ABQ',
                    title: 'Blinding Lights',
                    artist: 'The Weeknd',
                    album: 'After Hours',
                    thumbnail: 'https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg',
                    duration: 200,
                    url: 'https://www.youtube.com/watch?v=4NRXx6U8ABQ',
                    videoId: '4NRXx6U8ABQ',
                    type: 'song'
                },
                {
                    id: 'JGwWNGJdvx8',
                    title: 'Shape of You',
                    artist: 'Ed Sheeran',
                    album: '÷ (Divide)',
                    thumbnail: 'https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg',
                    duration: 234,
                    url: 'https://www.youtube.com/watch?v=JGwWNGJdvx8',
                    videoId: 'JGwWNGJdvx8',
                    type: 'song'
                },
                {
                    id: 'TUVcZfQe-Kw',
                    title: 'Levitating',
                    artist: 'Dua Lipa',
                    album: 'Future Nostalgia',
                    thumbnail: 'https://i.ytimg.com/vi/TUVcZfQe-Kw/hqdefault.jpg',
                    duration: 203,
                    url: 'https://www.youtube.com/watch?v=TUVcZfQe-Kw',
                    videoId: 'TUVcZfQe-Kw',
                    type: 'song'
                },
                {
                    id: '34Na4j8AVgA',
                    title: 'Starboy',
                    artist: 'The Weeknd ft. Daft Punk',
                    album: 'Starboy',
                    thumbnail: 'https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg',
                    duration: 230,
                    url: 'https://www.youtube.com/watch?v=34Na4j8AVgA',
                    videoId: '34Na4j8AVgA',
                    type: 'song'
                },
                {
                    id: 'XXYlFuWEuKI',
                    title: 'Save Your Tears',
                    artist: 'The Weeknd',
                    album: 'After Hours',
                    thumbnail: 'https://i.ytimg.com/vi/XXYlFuWEuKI/hqdefault.jpg',
                    duration: 215,
                    url: 'https://www.youtube.com/watch?v=XXYlFuWEuKI',
                    videoId: 'XXYlFuWEuKI',
                    type: 'song'
                },
                {
                    id: 'tQ0yjYUFKAE',
                    title: 'Peaches',
                    artist: 'Justin Bieber ft. Daniel Caesar',
                    album: 'Justice',
                    thumbnail: 'https://i.ytimg.com/vi/tQ0yjYUFKAE/hqdefault.jpg',
                    duration: 198,
                    url: 'https://www.youtube.com/watch?v=tQ0yjYUFKAE',
                    videoId: 'tQ0yjYUFKAE',
                    type: 'song'
                },
                {
                    id: 'kTJczUoc26U',
                    title: 'Stay',
                    artist: 'The Kid LAROI & Justin Bieber',
                    album: "F*ck Love 3: Over You",
                    thumbnail: 'https://i.ytimg.com/vi/kTJczUoc26U/hqdefault.jpg',
                    duration: 141,
                    url: 'https://www.youtube.com/watch?v=kTJczUoc26U',
                    videoId: 'kTJczUoc26U',
                    type: 'song'
                },
                {
                    id: 'b1kbLwvqugk',
                    title: 'Anti-Hero',
                    artist: 'Taylor Swift',
                    album: 'Midnights',
                    thumbnail: 'https://i.ytimg.com/vi/b1kbLwvqugk/hqdefault.jpg',
                    duration: 201,
                    url: 'https://www.youtube.com/watch?v=b1kbLwvqugk',
                    videoId: 'b1kbLwvqugk',
                    type: 'song'
                },
                {
                    id: 'H5v3kku4y6Q',
                    title: 'As It Was',
                    artist: 'Harry Styles',
                    album: "Harry's House",
                    thumbnail: 'https://i.ytimg.com/vi/H5v3kku4y6Q/hqdefault.jpg',
                    duration: 167,
                    url: 'https://www.youtube.com/watch?v=H5v3kku4y6Q',
                    videoId: 'H5v3kku4y6Q',
                    type: 'song'
                },
                {
                    id: 'G7KNmW9a75Y',
                    title: 'Flowers',
                    artist: 'Miley Cyrus',
                    album: 'Endless Summer Vacation',
                    thumbnail: 'https://i.ytimg.com/vi/G7KNmW9a75Y/hqdefault.jpg',
                    duration: 200,
                    url: 'https://www.youtube.com/watch?v=G7KNmW9a75Y',
                    videoId: 'G7KNmW9a75Y',
                    type: 'song'
                },
                {
                    id: 'Uq9gPaIzbe8',
                    title: 'Unholy',
                    artist: 'Sam Smith & Kim Petras',
                    album: 'Gloria',
                    thumbnail: 'https://i.ytimg.com/vi/Uq9gPaIzbe8/hqdefault.jpg',
                    duration: 156,
                    url: 'https://www.youtube.com/watch?v=Uq9gPaIzbe8',
                    videoId: 'Uq9gPaIzbe8',
                    type: 'song'
                }
            ],
            trendingNow: [
                {
                    id: 'b1kbLwvqugk',
                    title: 'Anti-Hero',
                    artist: 'Taylor Swift',
                    album: 'Midnights',
                    thumbnail: 'https://i.ytimg.com/vi/b1kbLwvqugk/hqdefault.jpg',
                    duration: 201,
                    url: 'https://www.youtube.com/watch?v=b1kbLwvqugk',
                    videoId: 'b1kbLwvqugk',
                    type: 'song'
                },
                {
                    id: 'H5v3kku4y6Q',
                    title: 'As It Was',
                    artist: 'Harry Styles',
                    album: "Harry's House",
                    thumbnail: 'https://i.ytimg.com/vi/H5v3kku4y6Q/hqdefault.jpg',
                    duration: 167,
                    url: 'https://www.youtube.com/watch?v=H5v3kku4y6Q',
                    videoId: 'H5v3kku4y6Q',
                    type: 'song'
                },
                {
                    id: 'G7KNmW9a75Y',
                    title: 'Flowers',
                    artist: 'Miley Cyrus',
                    album: 'Endless Summer Vacation',
                    thumbnail: 'https://i.ytimg.com/vi/G7KNmW9a75Y/hqdefault.jpg',
                    duration: 200,
                    url: 'https://www.youtube.com/watch?v=G7KNmW9a75Y',
                    videoId: 'G7KNmW9a75Y',
                    type: 'song'
                },
                {
                    id: 'Uq9gPaIzbe8',
                    title: 'Unholy',
                    artist: 'Sam Smith & Kim Petras',
                    album: 'Gloria',
                    thumbnail: 'https://i.ytimg.com/vi/Uq9gPaIzbe8/hqdefault.jpg',
                    duration: 156,
                    url: 'https://www.youtube.com/watch?v=Uq9gPaIzbe8',
                    videoId: 'Uq9gPaIzbe8',
                    type: 'song'
                }
            ],
            forgottenFavorites: [],
            keepListening: [],
            similarRecommendations: [],
            newReleaseAlbums: []
        };
    }
}

// Export singleton instance
export default new YouTubeMusicService();
