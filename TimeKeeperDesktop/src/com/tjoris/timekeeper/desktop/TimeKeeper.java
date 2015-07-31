package com.tjoris.timekeeper.desktop;

import java.io.File;

import javax.swing.UIManager;

public class TimeKeeper
{
	public static void main(final String[] args)
	{
		try
        {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	        final TimeKeeperFrame frame = new TimeKeeperFrame(new File("data/timekeeper.ini"));
	        frame.setVisible(true);
        }
        catch (final Exception e)
        {
	        e.printStackTrace();
        }
	}
}
