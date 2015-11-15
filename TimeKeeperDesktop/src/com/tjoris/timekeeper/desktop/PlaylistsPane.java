package com.tjoris.timekeeper.desktop;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import com.tjoris.timekeeper.desktop.data.IPlaylistStore;
import com.tjoris.timekeeper.desktop.data.PlaylistHeader;

public class PlaylistsPane extends JPanel
{
	private final IPlaylistStore fStore;
	private final DefaultListModel<PlaylistHeader> fListModel;
	private final JList<PlaylistHeader> fList;
	
	private static class Renderer implements ListCellRenderer<PlaylistHeader>
	{
		private final JLabel fLabel;
		
		private Renderer()
		{
			fLabel = new JLabel();
		}
		
		@Override
        public Component getListCellRendererComponent(final JList<? extends PlaylistHeader> list, final PlaylistHeader value, final int index, final boolean isSelected, final boolean cellHasFocus)
        {
			fLabel.setText(value.getName());
	        return fLabel;
        }
		
	}
	public PlaylistsPane(final IPlaylistStore store)
	{
		fStore = store;
		setLayout(new BorderLayout(5, 5));
		fListModel = new DefaultListModel<>();
		fList = new JList<>(fListModel);
		fList.setCellRenderer(new Renderer());
		add(fList, BorderLayout.CENTER);
	}
	
	public void refreshList()
	{
		final List<PlaylistHeader> playlists = fStore.readAllPlaylists();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				fListModel.clear();
				for (final PlaylistHeader playlist : playlists)
				{
					fListModel.addElement(playlist);
				}
			}
		});
	}
}
