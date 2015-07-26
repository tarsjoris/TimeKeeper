package com.tjoris.timekeeper;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity
{
	public static final String kFREQUENCY = "frequency";
	public static final String kDURATION = "duration";
	public static final String kSCREEN_ORIENTATION = "screenorientation";
	
		
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
	}
	
	public static int getIntPreference(final Activity activity, final String key, final int defaultValue)
	{
		final String result = PreferenceManager.getDefaultSharedPreferences(activity).getString(key, Integer.toString(defaultValue));
		try
		{
			return Integer.parseInt(result);
		}
		catch (final NumberFormatException e)
		{
			return defaultValue;
		}
	}

}
