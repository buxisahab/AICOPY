-- Enable RLS on tables if not already enabled
ALTER TABLE public.likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.playlists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.playlist_songs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.playlist_bookmarks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.album_bookmarks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.artist_bookmarks ENABLE ROW LEVEL SECURITY;

-- POLICIES FOR 'likes' TABLE
CREATE POLICY "Users can view their own likes" ON public.likes
FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own likes" ON public.likes
FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own likes" ON public.likes
FOR DELETE USING (auth.uid() = user_id);

-- POLICIES FOR 'playlists' TABLE
CREATE POLICY "Users can view their own playlists" ON public.playlists
FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own playlists" ON public.playlists
FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own playlists" ON public.playlists
FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own playlists" ON public.playlists
FOR DELETE USING (auth.uid() = user_id);


-- POLICIES FOR 'playlist_songs' TABLE
CREATE POLICY "Users can view songs in their playlists" ON public.playlist_songs
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM public.playlists 
        WHERE playlists.id = playlist_songs.playlist_id 
        AND playlists.user_id = auth.uid()
    )
);

CREATE POLICY "Users can insert songs into their playlists" ON public.playlist_songs
FOR INSERT WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.playlists 
        WHERE playlists.id = playlist_songs.playlist_id 
        AND playlists.user_id = auth.uid()
    )
);

CREATE POLICY "Users can delete songs from their playlists" ON public.playlist_songs
FOR DELETE USING (
    EXISTS (
        SELECT 1 FROM public.playlists 
        WHERE playlists.id = playlist_songs.playlist_id 
        AND playlists.user_id = auth.uid()
    )
);

-- POLICIES FOR 'playlist_bookmarks' TABLE (The one causing the error)
CREATE POLICY "Users can view their own playlist bookmarks" ON public.playlist_bookmarks
FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own playlist bookmarks" ON public.playlist_bookmarks
FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own playlist bookmarks" ON public.playlist_bookmarks
FOR DELETE USING (auth.uid() = user_id);

-- POLICIES FOR 'album_bookmarks' TABLE
CREATE POLICY "Users can view their own album bookmarks" ON public.album_bookmarks
FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own album bookmarks" ON public.album_bookmarks
FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own album bookmarks" ON public.album_bookmarks
FOR DELETE USING (auth.uid() = user_id);

-- POLICIES FOR 'artist_bookmarks' TABLE
CREATE POLICY "Users can view their own artist bookmarks" ON public.artist_bookmarks
FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own artist bookmarks" ON public.artist_bookmarks
FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own artist bookmarks" ON public.artist_bookmarks
FOR DELETE USING (auth.uid() = user_id);
