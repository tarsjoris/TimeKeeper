package com.tjoris.timekeeper.desktop;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.tjoris.timekeeper.desktop.data.IPlaylistStore;
import com.tjoris.timekeeper.desktop.data.PlaylistStoreFactory;

public class TimeKeeperFrame extends JFrame implements Receiver, IActionListener
{
    private static final long serialVersionUID = 819721379466345139L;
    
    private static final String kPROPERTY_MIDI_IN_DEVICE = "MidiInDevice";
	private static final String kPROPERTY_FRAME_X = "FrameX";
	private static final String kPROPERTY_FRAME_Y = "FrameY";
	private static final String kPROPERTY_FRAME_WIDTH = "FrameWidth";
	private static final String kPROPERTY_FRAME_HEIGHT = "FrameHeight";
	
	private final Configuration fConfiguration;
	private final IPlaylistStore fStore;
	private SoundGenerator fSoundGenerator;
	private MidiDevice fMidiDevice;
	private Transmitter fTransmitter = null;
	private final Map<Integer, EAction> fActionsByNote = new HashMap<Integer, EAction>();
	
	
	public TimeKeeperFrame(final File propertyFile) throws IOException, MidiUnavailableException
	{
		setTitle("Time Keeper");
		final URL resource = TimeKeeperFrame.class.getResource("/icons/timekeeper.png");
		setIconImage(Toolkit.getDefaultToolkit().createImage(resource));

		fStore = PlaylistStoreFactory.createStore();
		
		fConfiguration = new Configuration(propertyFile);
		readProperties();
		
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(final WindowEvent e)
			{
				try
				{
					shutDown();
				}
				catch (final Exception exception)
				{
					exception.printStackTrace();
				}

				System.exit(0);
			}
		});
	}
	
	public void close()
	{
	}

	public void send(final MidiMessage message, final long timeStamp)
	{
		final boolean on;
		final int note;
		final byte[] data = message.getMessage();
		if ((data[0] >= (byte) 0x80) && (data[0] <= (byte ) 0x8F))
		{
			// note off event
			on = false;
			note = data[1];
		}
		else if ((data[0] >= (byte) 0x90) && (data[0] <= (byte ) 0x9F))
		{
			// note on event
			on = true;
			note = data[1];
		}
		else if ((data[0] >= (byte) 0xB0) && (data[0] <= (byte) 0xBF))
		{
			// control change
			on = true;
			note = 128 + data[1];
		}
		else
		{
			on = false;
			note = -1;
		}
		if (on && note != -1)
		{
			/* TODO
			if (fToggleButton.isSelected())
			{
				final int selectedRow = fTable.getSelectedRow();
				if (selectedRow != -1)
				{
					final int oldRowIndex = fModel.getRowForNote(note);
					if ((oldRowIndex != -1) && (selectedRow != oldRowIndex))
					{
						fModel.setNote(oldRowIndex, -1);
					}
					fModel.setNote(selectedRow, note);
				}
			}
			else*/
			{
				final EAction action = fActionsByNote.get(Integer.valueOf(note));
				if (action != null)
				{
					triggerAction(action);
					SwingUtilities.invokeLater(new Runnable ()
					{
						public void run()
						{
							// TODO highlight button
						}
					});
				}
			}
		}
	}
	
	@Override
	public void triggerAction(final EAction action)
	{
		switch (action)
		{
		case kSTART:
		{
			fSoundGenerator.start();
			break;
		}
		case kSTOP:
		{
			fSoundGenerator.stop();
			break;
		}
		case kNEXT:
		{
			fSoundGenerator.start(120);
			break;
		}
		}
	}
	
	private void readProperties() throws MidiUnavailableException
	{
		updateMidiDevice(getMidiDevice(fConfiguration.getSystemProperty(kPROPERTY_MIDI_IN_DEVICE)));
		final Rectangle bounds = new Rectangle(100, 100, 200, 300);
		final String propertyFrameX = fConfiguration.getSystemProperty(kPROPERTY_FRAME_X);
		if (propertyFrameX != null)
		{
			bounds.x = Integer.parseInt(propertyFrameX);
		}
		final String propertyFrameY = fConfiguration.getSystemProperty(kPROPERTY_FRAME_Y);
		if (propertyFrameY != null)
		{
			bounds.y = Integer.parseInt(propertyFrameY);
		}
		final String propertyFrameWidth = fConfiguration.getSystemProperty(kPROPERTY_FRAME_WIDTH);
		if (propertyFrameWidth != null)
		{
			bounds.width = Integer.parseInt(propertyFrameWidth);
		}
		final String propertyFrameHeight = fConfiguration.getSystemProperty(kPROPERTY_FRAME_HEIGHT);
		if (propertyFrameHeight != null)
		{
			bounds.height = Integer.parseInt(propertyFrameHeight);
		}
		setBounds(bounds);
		
		if (fSoundGenerator != null)
		{
			fSoundGenerator.close();
		}
		fSoundGenerator = new SoundGenerator(880, 20);

		for (final EAction action : EAction.values())
		{
			final String value = fConfiguration.getUserProperty(action.getID());
			if (value != null)
			{
				fActionsByNote.put(Integer.valueOf(value), action);
			}
		}
	}

	private void updateMidiDevice(final MidiDevice midiDevice) throws MidiUnavailableException
	{
		if (fMidiDevice != null)
		{
			fMidiDevice.close();
			fMidiDevice = null;
		}
		if (fTransmitter != null)
		{
			fTransmitter.close();
			fTransmitter = null;
		}
		fMidiDevice = midiDevice;
		if (fMidiDevice != null)
		{
			if (!fMidiDevice.isOpen())
			{
				fMidiDevice.open();
			}
			fTransmitter = fMidiDevice.getTransmitter();
			fTransmitter.setReceiver(this);
		}
	}
	
	private void shutDown() throws IOException
	{
		fConfiguration.setSystemProperty(kPROPERTY_FRAME_X, Integer.toString(getX()));
		fConfiguration.setSystemProperty(kPROPERTY_FRAME_Y, Integer.toString(getY()));
		fConfiguration.setSystemProperty(kPROPERTY_FRAME_WIDTH, Integer.toString(getWidth()));
		fConfiguration.setSystemProperty(kPROPERTY_FRAME_HEIGHT, Integer.toString(getHeight()));
		// TODO fModel.storeProperties(fConfiguration);
		if (fMidiDevice != null)
		{
			fConfiguration.setSystemProperty(kPROPERTY_MIDI_IN_DEVICE, fMidiDevice.getDeviceInfo().getName());
		}
		fConfiguration.saveProperties();
		
		if (fSoundGenerator != null)
		{
			fSoundGenerator.close();
			fSoundGenerator = null;
		}
		if (fMidiDevice != null)
		{
			fMidiDevice.close();
			fMidiDevice = null;
		}
		if (fTransmitter != null)
		{
			fTransmitter.close();
			fTransmitter = null;
		}
	}
	
	private static MidiDevice getMidiDevice(final String name) throws MidiUnavailableException
	{
		if (name == null)
		{
			return null;
		}
		final Info[] midiDeviceInfo = MidiSystem.getMidiDeviceInfo();
		for (int i = 0; i < midiDeviceInfo.length; ++i)
		{
			if (name.equals(midiDeviceInfo[i].getName()))
			{
				return MidiSystem.getMidiDevice(midiDeviceInfo[i]);
			}
		}
		return null;
	}
}
