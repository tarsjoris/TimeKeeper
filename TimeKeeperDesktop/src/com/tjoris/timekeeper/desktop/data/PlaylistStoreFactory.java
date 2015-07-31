package com.tjoris.timekeeper.desktop.data;

public class PlaylistStoreFactory
{
	public static IPlaylistStore createStore()
	{
		return new FilePlaylistStore();
	}
}
