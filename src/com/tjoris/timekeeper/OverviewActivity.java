package com.tjoris.timekeeper;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.tjoris.timekeeper.data.PlaylistHeader;
import com.tjoris.timekeeper.data.PlaylistStore;

public class OverviewActivity extends Activity
{
	private final PlaylistStore fStore;
	private List<PlaylistHeader> fPlaylists;

	public OverviewActivity()
	{
		fStore = new PlaylistStore(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.overview);

		getOverview().setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
			{
				trigger(position);
			}
		});
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		fillList();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.overview_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		// Handle presses on the action bar items
		switch (item.getItemId())
		{
		case R.id.settings:
		{
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		default:
		{
			return super.onOptionsItemSelected(item);
		}
		}
	}

	private void fillList()
	{
		fPlaylists = fStore.readAllPlaylists();
		final String[] data = new String[fPlaylists.size()];
		for (int i = 0; i < fPlaylists.size(); ++i)
		{
			data[i] = fPlaylists.get(i).getName();
		}
		getOverview().setAdapter(new ArrayAdapter<String>(this, R.layout.entry_playlist, data));
	}

	private void trigger(final int selection)
	{
		final Intent intent = new Intent(this, PlaylistActivity.class);
		intent.putExtra("playlist", fPlaylists.get(selection).getID());
		startActivity(intent);
	}

	private ListView getOverview()
	{
		return (ListView) findViewById(R.id.overview);
	}
}
