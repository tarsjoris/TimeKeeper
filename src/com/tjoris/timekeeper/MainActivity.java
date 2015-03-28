package com.tjoris.timekeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class MainActivity extends Activity
{
	private static final String[][] kPLAYLIST = new String[][] { { "Run to you", "120" }, { "AC/DC", "160" } };
	private static final String kKEY_NAME = "name";
	private static final String kKEY_TEMPO = "tempo";

	private final SoundGenerator fSoundGenerator = new SoundGenerator();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final ListView playlist = (ListView) findViewById(R.id.playlist);
		playlist.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
            {
				final String tempo = kPLAYLIST[position][1];
				final int bpm = Integer.parseInt(tempo);
				fSoundGenerator.start(bpm);
				view.setSelected(true);
            }
		});
		fillList();
		register(R.id.button_start, new View.OnClickListener()
		{
			@Override
            public void onClick(final View v)
            {
				doStart();
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
	
	@Override
	protected void onDestroy()
	{
		fSoundGenerator.close();
	    super.onDestroy();
	}

	private void fillList()
	{
		final ListView playlist = (ListView) findViewById(R.id.playlist);
		final List<Map<String, String>> data = new ArrayList<Map<String,String>>();
		for (final String[] entry : kPLAYLIST)
		{
			final Map<String, String> map = new HashMap<String, String>();
			map.put(kKEY_NAME, entry[0]);
			map.put(kKEY_TEMPO, entry[1]);
			data.add(map);
		}
		playlist.setAdapter(new SimpleAdapter(this, data, R.layout.list_entry, new String[] {"name", "tempo"}, new int[] {R.id.entry_name, R.id.entry_tempo}));
	}

	private void register(final int id, final View.OnClickListener listener)
	{
		final Button button = (Button) findViewById(id);
		if (button != null)
		{
			button.setOnClickListener(listener);
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
