package com.tjoris.timekeeper;

import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.tjoris.timekeeper.data.PlaylistHeader;
import com.tjoris.timekeeper.data.PlaylistStore;

public class OverviewActivity extends Activity
{
	private InputDialog fAddPlaylistDialog;
	private final ConfirmationDialog fDeleteDialog;
	private final PlaylistStore fStore;
	private List<PlaylistHeader> fPlaylists;
	private ActionMode fActionMode = null;

	public OverviewActivity()
	{
		fStore = new PlaylistStore(this);
		fDeleteDialog = new ConfirmationDialog(R.string.overview_delete_message, new ConfirmationDialog.IListener()
		{
			@Override
			public void confirm()
			{
				deleteSelectedItems();
			}
		});
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

	private void fillList()
	{
		fPlaylists = fStore.readAllPlaylists();
		final String[] data = new String[fPlaylists.size()];
		for (int i = 0; i < fPlaylists.size(); ++i)
		{
			data[i] = fPlaylists.get(i).getName();
		}
		getOverview().setAdapter(new ArrayAdapter<String>(this, R.layout.overview_entry, data));
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
		for (int i = 0; i < overview.getChildCount(); ++i)
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
		fillList();
	}

	private ListView getOverview()
	{
		return (ListView) findViewById(R.id.overview);
	}
}
