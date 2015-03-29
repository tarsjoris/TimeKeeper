package com.tjoris.timekeeper.data;

import java.util.ArrayList;
import java.util.List;

public class Playlist extends PlaylistHeader
{
	private List<Song> fSongs;
	
	public Playlist(final String name, final int weight)
	{
		this(kID_NEW, name, weight);
	}
	
	public Playlist(final long id, final String name, final int weight)
	{
		super(id, name, weight);
		fSongs = new ArrayList<Song>();
	}
	
	public List<Song> getSongs()
	{
		return fSongs;
	}
	
	public void addSong(final Song song)
	{
		final int weight;
		if (!fSongs.isEmpty())
		{
			weight = fSongs.get(fSongs.size() - 1).getWeight() + 1;
		}
		else
		{
			weight = 0;
		}
		song.setWeight(weight);
		// TODO store song
		onlyAddSong(song);
	}
	
	public void onlyAddSong(final Song song)
	{
		fSongs.add(song);
	}
	
	public void removeSong(final int position)
	{
		fSongs.remove(position);
	}
}
