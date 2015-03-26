package com.tjoris.timekeeper;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends Activity
{
	private static final String[][] kPLAYLIST = new String[][] { { "Run to you", "120" }, { "AC/DC", "160" } };

	private final SoundGenerator fSoundGenerator = new SoundGenerator();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		fillList();
		register(R.id.button_start, new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(final View v, final MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					doStart();
					return true;
				}
				return false;
			}
		});
		register(R.id.button_stop, new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(final View v, final MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					doStop();
					return true;
				}
				return false;
			}
		});
	}
	
	@Override
	protected void onDestroy()
	{
		fSoundGenerator.close();
	    super.onDestroy();
	}

	private void fillList()
	{
		final TableLayout playlist = (TableLayout) findViewById(R.id.playlist);
		if (playlist != null)
		{
			for (final String[] entry : kPLAYLIST)
			{
				final TableRow row = new TableRow(this);
				row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				for (final String data : entry)
				{
					final TextView tv = new TextView(this);
					tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
					tv.setText(data);
					row.addView(tv);
				}
				playlist.addView(row);
			}
		}
	}

	private void register(final int id, final View.OnTouchListener listener)
	{
		final Button button = (Button) findViewById(id);
		if (button != null)
		{
			button.setOnTouchListener(listener);
		}
	}

	private void doStart()
	{
		fSoundGenerator.start();
	}

	private void doStop()
	{
		fSoundGenerator.stop();
	}
}
