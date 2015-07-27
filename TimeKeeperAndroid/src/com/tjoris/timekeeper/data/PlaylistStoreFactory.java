package com.tjoris.timekeeper.data;

import android.content.Context;

public class PlaylistStoreFactory
{
	public static IPlaylistStore createStore(final Context context)
	{
		return new FilePlaylistStore();
	}
}
