package com.tjoris.timekeeper.desktop.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

public class FilePlaylistStore implements IPlaylistStore
{
	private static final String kTAG_PLAYLIST = "playlist";
	private static final String kTAG_SONG = "song";
	private static final String kATTR_NAME = "name";
	private static final String kATTR_WEIGHT = "weight";
	private static final String kATTR_TEMPO = "tempo";
	
	private static SAXParser kPARSER = null;
	private static TransformerHandler kTRANSFORMER = null;
	
	static
	{
		try
        {
	        kPARSER = SAXParserFactory.newInstance().newSAXParser();
	        kTRANSFORMER = ((SAXTransformerFactory)SAXTransformerFactory.newInstance()).newTransformerHandler();
        }
        catch (final ParserConfigurationException | SAXException | TransformerConfigurationException | TransformerFactoryConfigurationError e)
        {
	        e.printStackTrace();
        }
	}
	
	
	private static class EntryComparator implements Comparator<AbstractEntry>
	{
		@Override
		public int compare(final AbstractEntry lhs, final AbstractEntry rhs)
		{
			return lhs.getWeight() - rhs.getWeight();
		}
	}
	
	
	private static abstract class AbstractHandler extends DefaultHandler
	{
		private final PlaylistHeader fPlaylist;
		
		private AbstractHandler(final PlaylistHeader playlist)
		{
			fPlaylist = playlist;
		}
		
		@Override
		public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException
		{
			if (kTAG_PLAYLIST.equals(qName))
			{
				final String name = attributes.getValue(kATTR_NAME);
				final int weight = Integer.parseInt(attributes.getValue(kATTR_WEIGHT));
				fPlaylist.setName(name);
				fPlaylist.setWeight(weight);
			}
		}
	}
	
	
	private static class FullHandler extends AbstractHandler
	{
		private final Playlist fPlaylist;
		
		private FullHandler(final Playlist playlist)
		{
			super(playlist);
			fPlaylist = playlist;
		}
		
		@Override
		public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException
		{
			super.startElement(uri, localName, qName, attributes);
			if (kTAG_SONG.equals(qName))
			{
				final String name = attributes.getValue(kATTR_NAME);
				final int tempo = Integer.parseInt(attributes.getValue(kATTR_TEMPO));
				fPlaylist.addSong(new Song(name, tempo));
			}
		}
	}
	
	
	private static class HeaderHandler extends AbstractHandler
	{
		private HeaderHandler(final PlaylistHeader playlist)
		{
			super(playlist);
		}
		
		@Override
		public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException
		{
			super.startElement(uri, localName, qName, attributes);
			if (kTAG_PLAYLIST.equals(qName))
			{
				throw new SAXException("stop");
			}
		}
	}
	
	private static final File kBASE_PATH = new File("D:\\Home\\Dropbox", "TimeKeeper");
	private static final EntryComparator kCOMPARATOR = new EntryComparator();
	private static final Pattern kFILENAME_PATTERN = Pattern.compile("([0-9]+)\\.xml");
	
	@Override
	public List<PlaylistHeader> readAllPlaylists()
	{
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
			synchronized (kPARSER)
            {
				kPARSER.parse(getPlaylistFile(playlist), new FullHandler(playlist));
            }
			return playlist;
		}
		catch (final IOException | SAXException e)
		{
			e.printStackTrace();
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
		final long id = Long.parseLong(filename);
		final PlaylistHeader playlist = new PlaylistHeader(id, null, 0);
		try
		{
			synchronized (kPARSER)
			{
				kPARSER.parse(file, new HeaderHandler(playlist));
			}
			return playlist;
		}
		catch (final SAXException e)
		{
			if (e.getMessage().equals("stop"))
			{
				return playlist;
			}
			e.printStackTrace();
		}
		catch (final IOException e)
		{
			e.printStackTrace();
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
			synchronized (kTRANSFORMER)
			{
				kTRANSFORMER.setResult(new StreamResult(getPlaylistFile(playlist)));
				kTRANSFORMER.startDocument();
				final AttributesImpl atts = new AttributesImpl();
				atts.addAttribute("", kATTR_NAME, kATTR_NAME, "text", playlist.getName());
				atts.addAttribute("", kATTR_WEIGHT, kATTR_WEIGHT, "text", Integer.toString(playlist.getWeight()));
				kTRANSFORMER.startElement("", kTAG_PLAYLIST, kTAG_PLAYLIST, atts);
				for (final Song song : playlist.getSongs())
				{
					atts.clear();
					atts.addAttribute("", kATTR_NAME, kATTR_NAME, "text", song.getName());
					atts.addAttribute("", kATTR_TEMPO, kATTR_TEMPO, "text", Integer.toString(song.getTempo()));
					kTRANSFORMER.startElement("", kTAG_SONG, kTAG_SONG, atts);
					kTRANSFORMER.endElement("", kTAG_SONG, kTAG_SONG);
				}
				kTRANSFORMER.endElement("", kTAG_PLAYLIST, kTAG_PLAYLIST);
				kTRANSFORMER.endDocument();
			}
		}
		catch (final SAXException e)
		{
			e.printStackTrace();
		}
	}
	
	private File getPlaylistFile(final PlaylistHeader playlist)
	{
		return new File(kBASE_PATH, Long.toString(playlist.getID()) + ".xml");
	}
}
