package com.tjoris.timekeeper.data;

public class Song extends AbstractEntry
{
	private int fTempo;

	public Song(final String name, final int weight, final int tempo)
	{
		this(kID_NEW, name, weight, tempo);
	}
	
	public Song(final long id, final String name, final int weight, final int tempo)
	{
		super(id, name, weight);
		fTempo = tempo;
	}

	public int getTempo()
	{
		return fTempo;
	}

	public void setTempo(int tempo)
	{
		fTempo = tempo;
	}

}
