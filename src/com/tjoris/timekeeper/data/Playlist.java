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
	
	public Playlist(final Playlist other, final String name, final int weight)
	{
		this(name, weight);
		if (other != null)
		{
			for (final Song song : other.getSongs())
			{
				fSongs.add(new Song(song));
			}
		}
	}
	
	public List<Song> getSongs()
	{
		return fSongs;
	}
	
	public void addSong(final PlaylistStore store, final Song song)
	{
		addSong(song);
		store.storeSong(this, song);
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
		onlyAddSong(song);
	}
	
	public void onlyAddSong(final Song song)
	{
		fSongs.add(song);
	}
	
	public void removeSong(final PlaylistStore store, final int position)
	{
		if (position >= 0 && position < fSongs.size())
		{
			final Song song = fSongs.remove(position);
			store.deleteSong(song);
			for (int i = position + 1; i < fSongs.size(); ++i)
			{
				final Song after = fSongs.get(i);
				after.setWeight(after.getWeight() - 1);
				store.storeSong(this, after);
			}
		}
	}
	
	public void move(final PlaylistStore store, final int position, final boolean up)
	{
		final int otherPos = position + (up ? -1 : 1);
		if (position >= 0 && position < fSongs.size() && otherPos >= 0 && otherPos < fSongs.size())
		{
			final Song song = fSongs.get(position);
			final Song other = fSongs.get(otherPos);
			fSongs.set(position, other);
			fSongs.set(otherPos, song);
			final int tmpWeight = other.getWeight();
			other.setWeight(song.getWeight());
			song.setWeight(tmpWeight);
			store.storeSong(this, other);
			store.storeSong(this, song);
		}
	}
}
