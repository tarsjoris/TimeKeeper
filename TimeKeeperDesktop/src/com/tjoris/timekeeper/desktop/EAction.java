package com.tjoris.timekeeper.desktop;

public enum EAction
{
	kSTART("start"),
	kSTOP("stop"),
	kNEXT("next");
	
	private final String fID;
	
	private EAction(final String id)
	{
		fID = id;
	}
	
	public String getID()
	{
		return fID;
	}
}
