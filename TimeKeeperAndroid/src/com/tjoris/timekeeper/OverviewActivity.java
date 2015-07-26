package com.tjoris.timekeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.tjoris.timekeeper.data.PlaylistHeader;
import com.tjoris.timekeeper.data.PlaylistStore;

public class OverviewActivity extends Activity
{
	private static final String kKEY_NAME = "name";

	private final List<Map<String, String>> fData;
	private InputDialog fAddPlaylistDialog;
	private final ConfirmationDialog fDeleteDialog;
	private final PlaylistStore fStore;
	private List<PlaylistHeader> fPlaylists;
	private ActionMode fActionMode = null;

	public OverviewActivity()
	{
		fData = new ArrayList<Map<String, String>>();
		fStore = new PlaylistStore(this);
		fDeleteDialog = new ConfirmationDialog()
		{
			@Override
			public void confirm()
			{
				deleteSelectedItems();
			}
		};
		fDeleteDialog.setOptions(R.string.overview_delete_message, R.string.overview_delete_yes, R.string.overview_delete_no);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		fAddPlaylistDialog = new InputDialog(getLayoutInflater(),
				R.string.overview_action_addplaylist,
				R.drawable.ic_action_new,
				R.layout.overview_addplaylist,
				R.string.overview_addplaylist_add,
				R.string.overview_addplaylist_cancel
		)
		{
			@Override
			public void dialogConfirmed(final Dialog dialog)
			{
				final CharSequence name = ((EditText)dialog.findViewById(R.id.overiew_addplaylist_name)).getText();
				final int weight = fPlaylists.isEmpty() ? 0 : fPlaylists.get(fPlaylists.size() - 1).getWeight() + 1;
				final PlaylistHeader playlist = new PlaylistHeader(name.toString(), weight);
				fStore.storePlaylistHeader(playlist);
				openPlaylist(playlist);
			}
		};

		setContentView(R.layout.overview);

		final ListView overview = getOverview();
		overview.setAdapter(new SimpleAdapter(this, fData, R.layout.overview_entry, new String[] { kKEY_NAME }, new int[] { R.id.overview_entry_name }));
		overview.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
			{
				trigger(position);
			}
		});
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
				fActionMode = null;
			}

			@Override
			public boolean onCreateActionMode(final ActionMode mode, final Menu menu)
			{
				fActionMode = mode;
				mode.getMenuInflater().inflate(R.menu.overview_contextactions, menu);
				return true;
			}

			@Override
			public boolean onActionItemClicked(final ActionMode mode, final MenuItem item)
			{
				switch (item.getItemId())
				{
				case R.id.overview_action_up:
				{
					move(true);
					return true;
				}
				case R.id.overview_action_down:
				{
					move(false);
					return true;
				}
				case R.id.overview_action_delete:
				{
					fDeleteDialog.show(getFragmentManager(), "delete_playlists");
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
		final int orientation = SettingsActivity.getIntPreference(this, SettingsActivity.kSCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
		setRequestedOrientation(orientation);
		reloadList();
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

	private void reloadList()
	{
		fData.clear();
		fPlaylists = fStore.readAllPlaylists();
		for (int i = 0; i < fPlaylists.size(); ++i)
		{
			final Map<String, String> map = new HashMap<String, String>();
			map.put(kKEY_NAME, fPlaylists.get(i).getName());
			fData.add(map);
		}
		((SimpleAdapter)getOverview().getAdapter()).notifyDataSetChanged();
	}

	private void trigger(final int selection)
	{
		openPlaylist(fPlaylists.get(selection));
	}
	
	private void openPlaylist(final PlaylistHeader playlist)
	{
		final Intent intent = new Intent(this, PlaylistActivity.class);
		intent.putExtra("playlist", playlist.getID());
		startActivity(intent);
	}

	private void deleteSelectedItems()
	{
		final ListView overview = getOverview();
		final SparseBooleanArray selection = overview.getCheckedItemPositions();
		for (int i = 0; i < fPlaylists.size(); ++i)
		{
			if (selection.get(i))
			{
				fStore.deletePlaylist(fPlaylists.get(i));
			}
		}
		if (fActionMode != null)
		{
			fActionMode.finish();
		}
		reloadList();
	}

	private void move(final boolean up)
	{
		final ListView overview = getOverview();
		final SparseBooleanArray selection = overview.getCheckedItemPositions();
		boolean ignore = false;
		for (int i = up ? 0 : fPlaylists.size() - 1; up ? i < fPlaylists.size() : i >= 0; i += up ? 1 : -1)
		{
			if (selection.get(i))
			{
				if (up ? i <= 0 : i >= fPlaylists.size() - 1)
				{
					// leave the first selection group alone
					ignore = true;
				}
				else if (!ignore)
				{
					final int otherIndex = up ? i - 1 : i + 1;
					if (!selection.get(otherIndex))
					{
						overview.setItemChecked(i, false);
						overview.setItemChecked(otherIndex, true);
					}
					switchPlaylists(otherIndex, i);
				}
			}
			else
			{
				ignore = false;
			}
		}
		reloadList();
	}
	
	private void switchPlaylists(final int index1, final int index2)
	{
		final PlaylistHeader playlist1 = fPlaylists.get(index1);
		final PlaylistHeader playlist2 = fPlaylists.get(index2);
		final int tempWeight = playlist1.getWeight();
		playlist1.setWeight(playlist2.getWeight());
		playlist2.setWeight(tempWeight);
		fPlaylists.set(index1, playlist2);
		fPlaylists.set(index2, playlist1);
		fStore.storePlaylistHeader(playlist1);
		fStore.storePlaylistHeader(playlist2);
	}

	private ListView getOverview()
	{
		return (ListView) findViewById(R.id.overview);
	}
}
