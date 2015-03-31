package com.tjoris.timekeeper;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.tjoris.timekeeper.data.Playlist;
import com.tjoris.timekeeper.data.PlaylistHeader;
import com.tjoris.timekeeper.data.PlaylistStore;

public class OverviewActivity extends Activity implements PlaylistNameDialog.IListener
{
	private final PlaylistNameDialog fAddPlaylistDialog;
	private final PlaylistStore fStore;
	private List<PlaylistHeader> fPlaylists;

	public OverviewActivity()
	{
		fStore = new PlaylistStore(this);
		fAddPlaylistDialog = new PlaylistNameDialog(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.overview);

		final ListView overview = getOverview();
		overview.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
			{
				trigger(position);
			}
		});
		overview.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		overview.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener()
		{
			@Override
			public boolean onPrepareActionMode(final ActionMode mode, final Menu menu)
			{
				return false;
			}

			@Override
			public void onDestroyActionMode(final ActionMode mode)
			{
			}

			@Override
			public boolean onCreateActionMode(final ActionMode mode, final Menu menu)
			{
				mode.getMenuInflater().inflate(R.menu.overview_contextactions, menu);
		        return true;
			}

			@Override
			public boolean onActionItemClicked(final ActionMode mode, final MenuItem item)
			{
				switch (item.getItemId())
				{
				case R.id.overview_action_delete:
				{
					//deleteSelectedItems();
					mode.finish();
					return true;
				}
				default:
				{
					return false;
				}
				}
			}

			@Override
			public void onItemCheckedStateChanged(final ActionMode mode, final int position, final long id, final boolean checked)
			{
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
		case R.id.overview_action_settings:
		{
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		case R.id.overview_action_addplaylist:
		{
			fAddPlaylistDialog.show(getFragmentManager(), "addplaylist");
			return true;
		}
		default:
		{
			return super.onOptionsItemSelected(item);
		}
		}
	}

	@Override
	public void add(final CharSequence name)
	{
		final int weight = fPlaylists.isEmpty() ? 0 : fPlaylists.get(fPlaylists.size() - 1).getWeight() + 1;
		fStore.storePlaylist(new Playlist(name.toString(), weight));
		fillList();
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
