package com.tjoris.timekeeper.desktop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.tjoris.timekeeper.desktop.util.WaveUtil;

public class SoundGenerator
{
	private final int fBeepFrequency;
	private final int fBeepDuration;
	private Clip fClip;
	
	
	public SoundGenerator(final int beepFrequency, final int beepDuration)
	{
		fBeepFrequency = beepFrequency;
		fBeepDuration = beepDuration;
	}
	
	public void close()
	{
		stop();
		if (fClip != null)
		{
			fClip.close();
		}
		System.out.println("Stopped");
	}

	public void configure(final int bpm)
	{
		try
		{
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			WaveUtil.generateSine(out, fBeepFrequency, fBeepDuration, bpm);
			final Clip newClip = AudioSystem.getClip();
			final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(out.toByteArray()));
			newClip.open(audioInputStream);
			fClip.stop();
			fClip.close();
			fClip = newClip;
		}
		catch (final IOException | LineUnavailableException | UnsupportedAudioFileException e)
		{
			System.out.println("Failed to configure SoundGenerator");
			e.printStackTrace();
		}
	}

	public void start(final int bpm)
	{
		configure(bpm);
		fClip.loop(Clip.LOOP_CONTINUOUSLY);
	}

	public void start()
	{
		fClip.stop();
		fClip.setFramePosition(0);
		fClip.loop(Clip.LOOP_CONTINUOUSLY);
	}

	public void stop()
	{
		if (fClip != null)
		{
			fClip.stop();
		}
	}
}
