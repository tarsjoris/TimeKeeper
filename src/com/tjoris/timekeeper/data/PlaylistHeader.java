package com.tjoris.timekeeper.data;


public class PlaylistHeader extends AbstractEntry
{
	public PlaylistHeader(final String name, final int weight)
	{
		this(kID_NEW, name, weight);
	}
	
	public PlaylistHeader(final long id, final String name, final int weight)
	{
		super(id, name, weight);
	}
	
	public void setName(final PlaylistStore store, final String name)
	{
		super.setName(name);
		store.storePlaylistHeader(this);
	}
}
