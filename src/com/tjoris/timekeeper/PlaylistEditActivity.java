package com.tjoris.timekeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.tjoris.timekeeper.data.Playlist;
import com.tjoris.timekeeper.data.PlaylistStore;
import com.tjoris.timekeeper.data.Song;

public class PlaylistEditActivity extends Activity
{
	private static final String kKEY_NAME = "name";
	private static final String kKEY_TEMPO = "tempo";

	private final List<Map<String, String>> fData;
	private InputDialog fAddSongDialog;
	private InputDialog fRenameDialog;
	private InputDialog fCopyDialog;
	private final PlaylistStore fStore;
	private Playlist fPlaylist = null;

	public PlaylistEditActivity()
	{
		fData = new ArrayList<Map<String, String>>();
		fStore = new PlaylistStore(this);
	}

	@SuppressLint("ClickableViewAccessibility")
    @Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		fAddSongDialog = new InputDialog(getLayoutInflater(),
				R.string.playlistedit_action_addsong,
				R.drawable.ic_action_new,
				R.layout.playlistedit_addsong,
				R.string.playlistedit_addsong_add,
				R.string.playlistedit_addsong_cancel
		)
		{
			@Override
			public void dialogConfirmed(final Dialog dialog)
			{
				final CharSequence name = ((EditText)dialog.findViewById(R.id.playlistedit_addsong_name)).getText();
				final CharSequence tempoS = ((EditText)dialog.findViewById(R.id.playlistedit_addsong_tempo)).getText();
				int tempo = 120;
				try
				{
					tempo = Math.min(300, Math.max(30, Integer.parseInt(tempoS.toString())));
				}
				catch (final NumberFormatException e)
				{
					Log.e("TimeKeeper", "Invalid tempo: " + e.getMessage(), e);
				}
				final Song song = new Song(name.toString(), tempo);
				fPlaylist.addSong(fStore, song);
				reloadSongs();
			}
		};
		fRenameDialog = new InputDialog(getLayoutInflater(),
				R.string.playlistedit_action_rename,
				R.drawable.ic_action_edit,
				R.layout.playlistedit_rename,
				R.string.playlistedit_rename_save,
				R.string.playlistedit_rename_cancel
		)
		{
			@Override
			public void viewCreated(final View view)
			{
				((EditText)view.findViewById(R.id.playlistedit_rename_name)).setText(fPlaylist != null ? fPlaylist.getName() : "");
			}
			
			@Override
			public void dialogConfirmed(final Dialog dialog)
			{
				final CharSequence name = ((EditText)dialog.findViewById(R.id.playlistedit_rename_name)).getText();
				fPlaylist.setName(fStore, name.toString());
				reloadName();
			}
		};
		fCopyDialog = new InputDialog(getLayoutInflater(),
				R.string.playlistedit_action_copy,
				R.drawable.ic_action_copy,
				R.layout.playlistedit_copy,
				R.string.playlistedit_copy_copy,
				R.string.playlistedit_copy_cancel
		)
		{
			@Override
			public void viewCreated(final View view)
			{
				((EditText)view.findViewById(R.id.playlistedit_copy_name)).setText(fPlaylist != null ? fPlaylist.getName() : "");
			}
			
			@Override
			public void dialogConfirmed(final Dialog dialog)
			{
				final CharSequence name = ((EditText)dialog.findViewById(R.id.playlistedit_copy_name)).getText();
				final Playlist playlist = new Playlist(fPlaylist, name.toString(), fStore.getNextPlaylistWeight());
				fStore.addPlaylist(playlist);
				
				fPlaylist = playlist;
				loadPlaylist();
			}
		};

		setContentView(R.layout.playlistedit);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		final ListView playlistView = getPlaylist();
		playlistView.setAdapter(new SimpleAdapter(this, fData, R.layout.playlistedit_entry, new String[] { kKEY_NAME, kKEY_TEMPO }, new int[] { R.id.playlistedit_entry_name, R.id.playlistedit_entry_tempo })
		{
			@Override
			public View getView(final int position, final View convertView, final ViewGroup parent)
			{
			    final View view = super.getView(position, convertView, parent);
			    view.findViewById(R.id.playlistedit_entry_delete).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(final View v)
					{
						fPlaylist.removeSong(fStore, position);
						reloadSongs();
					}
				});
			    view.findViewById(R.id.playlistedit_entry_up).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(final View v)
					{
						fPlaylist.moveUp(fStore, position);
						reloadSongs();
					}
				});
			    view.findViewById(R.id.playlistedit_entry_down).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(final View v)
					{
						fPlaylist.moveDown(fStore, position);
						reloadSongs();
					}
				});
				return view;
			}
		});
		playlistView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
			{
				//trigger(position);
			}
		});
		
		loadIntent();
	}
	
	private void loadIntent()
	{
		final Bundle bundle = getIntent().getExtras();
		if (bundle != null)
		{
			final long playlist = bundle.getLong("playlist");
			if (fPlaylist == null || fPlaylist.getID() != playlist)
			{
				fPlaylist = fStore.readPlaylist(playlist);
				loadPlaylist();
			}
		}
		else
		{
			fPlaylist = null;
			loadPlaylist();
		}
	}

	@Override
	protected void onDestroy()
	{
		fStore.close();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.playlistedit_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		// Handle presses on the action bar items
		switch (item.getItemId())
		{
		case R.id.playlistedit_action_addsong:
		{
			fAddSongDialog.show(getFragmentManager(), "addsong");
			return true;
		}
		case R.id.playlistedit_action_rename:
		{
			fRenameDialog.show(getFragmentManager(), "renameplaylist");
			return true;
		}
		case R.id.playlistedit_action_copy:
		{
			fCopyDialog.show(getFragmentManager(), "copyplaylist");
			return true;
		}
		default:
		{
			return super.onOptionsItemSelected(item);
		}
		}
	}

	private void loadPlaylist()
	{
		reloadName();
		reloadSongs();
	}
	
	private void reloadName()
	{
		if (fPlaylist != null)
		{
			getActionBar().setTitle(fPlaylist.getName());
		}
		else
		{
			getActionBar().setTitle("<no playlist>");
		}
	}
	
	private void reloadSongs()
	{
		fData.clear();
		if (fPlaylist != null)
		{
			for (final Song song : fPlaylist.getSongs())
			{
				final Map<String, String> map = new HashMap<String, String>();
				map.put(kKEY_NAME, song.getName());
				map.put(kKEY_TEMPO, Integer.toString(song.getTempo()));
				fData.add(map);
			}
		}
		((SimpleAdapter)getPlaylist().getAdapter()).notifyDataSetChanged();
	}
	
	private ListView getPlaylist()
	{
		return (ListView) findViewById(R.id.playlistedit);
	}
}
