package com.tjoris.timekeeper;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity
{
	public static final String kFREQUENCY = "frequency";
	public static final String kDURATION = "duration";
	
	
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
	}

}
