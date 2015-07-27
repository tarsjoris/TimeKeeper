package com.tjoris.timekeeper.data;

import java.util.List;

public interface IPlaylistStore
{
	public List<PlaylistHeader> readAllPlaylists();
	public void storePlaylistHeader(PlaylistHeader playlist);
	public Playlist readPlaylist(long id);
	public void addPlaylist(Playlist playlist);
	public void deletePlaylist(PlaylistHeader playlist);
	public int getNextPlaylistWeight();
	public void storeSong(Playlist playlist, Song song);
	public void deleteSong(Playlist playlist, Song song);
	public void close();
}
