package com.tjoris.timekeeper.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

public class FilePlaylistStore implements IPlaylistStore
{
	private static final String kTAG_PLAYLIST = "playlist";
	private static final String kTAG_SONG = "song";
	private static final String kATTR_NAME = "name";
	private static final String kATTR_WEIGHT = "weight";
	private static final String kATTR_TEMPO = "tempo";
	
	
	private static class EntryComparator implements Comparator<AbstractEntry>
	{
		@Override
		public int compare(final AbstractEntry lhs, final AbstractEntry rhs)
		{
			return lhs.getWeight() - rhs.getWeight();
		}
	}
	
	private static final File kBASE_PATH = new File(Environment.getExternalStorageDirectory(), "TimeKeeper");
	private static final EntryComparator kCOMPARATOR = new EntryComparator();
	private static final Pattern kFILENAME_PATTERN = Pattern.compile("([0-9]+)\\.xml");
	
	/*private final SQLPlaylistStore fSQLStore;
	
	
	FilePlaylistStore(final SQLPlaylistStore sqlStore)
    {
		fSQLStore = sqlStore;
    */
	
	@Override
	public List<PlaylistHeader> readAllPlaylists()
	{
		/*if (kBASE_PATH.listFiles().length == 1)
		{
			for (final PlaylistHeader playlistHeader : fSQLStore.readAllPlaylists())
			{
				final Playlist playlist = fSQLStore.readPlaylist(playlistHeader.getID());
				playlist.setID(AbstractEntry.kID_NEW);
				addPlaylist(playlist);
			}
		}*/

		final List<PlaylistHeader> playlists = new ArrayList<PlaylistHeader>();
		for (final File file : kBASE_PATH.listFiles())
		{
			final Matcher matcher = kFILENAME_PATTERN.matcher(file.getName());
			if (matcher.matches())
			{
				final PlaylistHeader playlistHeader = readPlaylistHeader(file, matcher.group(1));
				if (playlistHeader != null)
				{
					playlists.add(playlistHeader);
				}
			}
		}
		Collections.sort(playlists, kCOMPARATOR);
	    return playlists;
	}
	
	@Override
	public void storePlaylistHeader(final PlaylistHeader playlist)
	{
		final Playlist completePlaylist = readPlaylist(playlist.getID());
		completePlaylist.setName(playlist.getName());
		completePlaylist.setWeight(playlist.getWeight());
		savePlaylist(completePlaylist);
	}
	
	public Playlist readPlaylist(final long id)
	{
		try
		{
			final Playlist playlist = new Playlist(id, null, 0);
			final InputStream in = new BufferedInputStream(new FileInputStream(getPlaylistFile(playlist)));
			try
			{
				final XmlPullParser parser = Xml.newPullParser();
				parser.setInput(in, "UTF-8");
				for (;;)
				{
					switch (parser.next())
					{
					case XmlPullParser.START_TAG:
					{
						if (kTAG_PLAYLIST.equals(parser.getName()))
						{
							final String name = parser.getAttributeValue(null, kATTR_NAME);
							final int weight = Integer.parseInt(parser.getAttributeValue(null, kATTR_WEIGHT));
							playlist.setName(name);
							playlist.setWeight(weight);
						}
						else if (kTAG_SONG.equals(parser.getName()))
						{
							final String name = parser.getAttributeValue(null, kATTR_NAME);
							final int tempo = Integer.parseInt(parser.getAttributeValue(null, kATTR_TEMPO));
							playlist.addSong(new Song(name, tempo));
						}
						break;
					}
					case XmlPullParser.END_DOCUMENT:
					{
						return playlist;
					}
					default:
					{
						break;
					}
					}
				}
			}
			finally
			{
				in.close();
			}
		}
		catch (final XmlPullParserException | IOException e)
		{
			Log.e("TimeKeeper", "Could not read playlist: " + e.getMessage(), e);
		}
		return null;
	}
	
	@Override
	public void addPlaylist(final Playlist playlist)
	{
		savePlaylist(playlist);
	}
	
	@Override
	public void deletePlaylist(final PlaylistHeader playlist)
	{
		getPlaylistFile(playlist).delete();
	}
	
	@Override
	public int getNextPlaylistWeight()
	{
		final List<PlaylistHeader> playlists = readAllPlaylists();
		if (playlists.isEmpty())
		{
			return 0;
		}
		return playlists.get(playlists.size() - 1).getWeight() + 1;
	}
	
	@Override
	public void storeSong(final Playlist playlist, final Song song)
	{
		savePlaylist(playlist);
	}
	
	@Override
	public void deleteSong(final Playlist playlist, final Song song)
	{
		savePlaylist(playlist);
	}
	
	@Override
	public void close()
	{
	}
	
	private PlaylistHeader readPlaylistHeader(final File file, final String filename)
	{
		try
		{
			final long id = Long.parseLong(filename);
			final InputStream in = new BufferedInputStream(new FileInputStream(file));
			try
			{
				final XmlPullParser parser = Xml.newPullParser();
				parser.setInput(in, "UTF-8");
				for (;;)
				{
					switch (parser.next())
					{
					case XmlPullParser.START_TAG:
					{
						parser.require(XmlPullParser.START_TAG, null, kTAG_PLAYLIST);
						final String name = parser.getAttributeValue(null, kATTR_NAME);
						final int weight = Integer.parseInt(parser.getAttributeValue(null, kATTR_WEIGHT));
						return new PlaylistHeader(id, name, weight);
					}
					case XmlPullParser.END_DOCUMENT:
					{
						return null;
					}
					default:
					{
						break;
					}
					}
				}
			}
			finally
			{
				in.close();
			}
		}
		catch (final XmlPullParserException | IOException e)
		{
			Log.e("TimeKeeper", "Could not read playlist: " + e.getMessage(), e);
		}
		return null;
	}
	
	private void savePlaylist(final Playlist playlist)
	{
		try
		{
			if (playlist.isNew())
			{
				playlist.setID(System.currentTimeMillis());
			}
			final XmlSerializer serializer = Xml.newSerializer();
			final OutputStream out = new BufferedOutputStream(new FileOutputStream(getPlaylistFile(playlist)));
			try
			{
				serializer.setOutput(out, "UTF-8");
				serializer.startDocument("UTF-8", null);
				serializer.startTag(null, kTAG_PLAYLIST);
				serializer.attribute(null, kATTR_NAME, playlist.getName());
				serializer.attribute(null, kATTR_WEIGHT, Integer.toString(playlist.getWeight()));
				for (final Song song : playlist.getSongs())
				{
					serializer.startTag(null, kTAG_SONG);
					serializer.attribute(null, kATTR_NAME, song.getName());
					serializer.attribute(null, kATTR_TEMPO, Integer.toString(song.getTempo()));
					serializer.endTag(null, kTAG_SONG);
				}
				serializer.endTag(null, kTAG_PLAYLIST);
				serializer.endDocument();
			}
			finally
			{
				out.close();
			}
		}
		catch (final IOException e)
		{
			Log.e("TimeKeeper", "Could not write playlist: " + e.getMessage(), e);
		}
	}
	
	private File getPlaylistFile(final PlaylistHeader playlist)
	{
		return new File(kBASE_PATH, Long.toString(playlist.getID()) + ".xml");
	}
}
