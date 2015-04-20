package com.tjoris.timekeeper;

import java.io.FileDescriptor;
import java.io.IOException;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

public class SoundGenerator
{
	private static final int kSAMPLES_PER_SECOND = 44100;
	private static final int kBYTES_PER_SAMPLE = 2;
	private static final int kLOWEST_BPM = 30;
	
	private final FileDescriptor fFileDescriptor;
	private final SoundPool fSoundPool;
	private int fSoundID = -1;

	public SoundGenerator(final Context context)
	{
		try
		{
			fFileDescriptor = context.getAssets().openFd("beep.wav").getFileDescriptor();
		}
		catch (final IOException e)
		{
			Log.e("TimeKeeper", "Could not load beep", e);
			throw new RuntimeException("Could not load beep", e);
		}
		final AudioAttributes.Builder audioAttributesBuilder = new AudioAttributes.Builder();
		audioAttributesBuilder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
		final SoundPool.Builder soundPoolBuilder = new SoundPool.Builder();
		soundPoolBuilder.setAudioAttributes(audioAttributesBuilder.build());
		soundPoolBuilder.setMaxStreams(1);
		fSoundPool = soundPoolBuilder.build();
		configure(120);
	}
	
	public void close()
	{
		stop();
		fSoundPool.release();
		Log.i("TimeKeeper", "Stopped.");
	}

	public void configure(final int bpm)
	{
		stop();
		if (fSoundID != -1)
		{
			fSoundPool.unload(fSoundID);
		}
		final int loopPoint = kSAMPLES_PER_SECOND * 60 * kBYTES_PER_SAMPLE / Math.max(kLOWEST_BPM, bpm);
		fSoundID = fSoundPool.load(fFileDescriptor, 945, loopPoint, 1);
	}

	public void start(final int bpm)
	{
		configure(bpm);
		start();
	}
	
	public void start()
	{
		fSoundPool.autoPause();
		fSoundPool.play(fSoundID, 1F, 1F, 1, -1, 1F);
	}

	public void stop()
	{
		fSoundPool.autoPause();
	}
}
