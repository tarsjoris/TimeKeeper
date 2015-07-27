package com.tjoris.timekeeper.data;

import android.content.Context;

public class PlaylistStoreFactory
{
	public static IPlaylistStore createStore(final Context context)
	{
		//final SQLPlaylistStore sqlStore = new SQLPlaylistStore(context);
		//final FilePlaylistStore fileStore = new FilePlaylistStore(sqlStore);
		final FilePlaylistStore fileStore = new FilePlaylistStore();
		return fileStore;
		//return sqlStore;
	}
}
