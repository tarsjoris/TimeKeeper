package com.tjoris.timekeeper.data;

public class Song extends AbstractEntry
{
	private int fTempo;

	public Song(final String name, final int tempo)
	{
		this(kID_NEW, name, -1, tempo);
	}
	
	public Song(final long id, final String name, final int weight, final int tempo)
	{
		super(id, name, weight);
		fTempo = tempo;
	}
	
	public Song(final Song other)
	{
		this(kID_NEW, other.getName(), other.getWeight(), other.getTempo());
	}

	public int getTempo()
	{
		return fTempo;
	}
	
	public void update(final PlaylistStore store, final PlaylistHeader playlist, final String name, final int tempo)
	{
		setName(name);
		fTempo = tempo;
		store.storeSong(playlist, this);
	}

}
