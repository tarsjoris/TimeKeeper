package com.tjoris.timekeeper;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends Activity
{
	private static final String[][] kPLAYLIST = new String[][] { { "Run to you", "120" }, { "AC/DC", "160" } };

	private SoundGenerator fSoundGenerator = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		fillList();
		register(R.id.button_reset, new View.OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				doReset();
			}
		});
		register(R.id.button_stop, new View.OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				doStop();
			}
		});
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

	private void register(final int id, final View.OnClickListener listener)
	{
		final Button button = (Button) findViewById(id);
		if (button != null)
		{
			button.setOnClickListener(listener);
		}
	}

	private void doReset()
	{
		if (fSoundGenerator != null)
		{
			fSoundGenerator.stop();
		}
		fSoundGenerator = new SoundGenerator(120);
	}

	private void doStop()
	{
		if (fSoundGenerator != null)
		{
			fSoundGenerator.stop();
			fSoundGenerator = null;
		}
	}
}
