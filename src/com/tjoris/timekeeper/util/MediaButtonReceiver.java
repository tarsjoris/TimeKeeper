package com.tjoris.timekeeper.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public abstract class MediaButtonReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(final Context context, final Intent intent)
	{
		if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()))
		{
			return;
		}
		final KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		if (event == null)
		{
			return;
		}
		if (event.getAction() == KeyEvent.ACTION_DOWN)
		{
			run();
			abortBroadcast();
		}
	}
	
	public abstract void run();
}
