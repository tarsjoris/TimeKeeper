package com.tjoris.timekeeper.data;

public class AbstractEntry
{
	public static final long kID_NEW = -1L;
	
	private long fID;
	private String fName;
	private int fWeight;
	
	public AbstractEntry(final long id, final String name, final int weight)
	{
		fID = id;
		fName = name;
		fWeight = weight;
	}
	
	public boolean isNew()
	{
		return fID == kID_NEW;
	}
	
	public long getID()
	{
		return fID;
	}
	
	public void setID(final long id)
	{
		fID = id;
	}
	
	public String getName()
	{
		return fName;
	}
	
	protected void setName(final String name)
	{
		fName = name;
	}
	
	public int getWeight()
	{
		return fWeight;
	}
	
	public void setWeight(final int weight)
	{
		fWeight = weight;
	}
}
