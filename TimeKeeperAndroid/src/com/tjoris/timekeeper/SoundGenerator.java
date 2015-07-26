package com.tjoris.timekeeper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;
import android.util.MutableBoolean;

import com.tjoris.timekeeper.util.WaveUtil;

public class SoundGenerator implements OnLoadCompleteListener
{
	private final File fFile;
	private final int fBeepFrequency;
	private final int fBeepDuration;
	private final SoundPool fSoundPool;
	private int fSoundID = -1;
	private MutableBoolean fPlaying = new MutableBoolean(false);

	public SoundGenerator(final Context context, final int beepFrequency, final int beepDuration)
	{
		fFile = new File(context.getCacheDir(), "beep.wav");
		fBeepFrequency = beepFrequency;
		fBeepDuration = beepDuration;
		final AudioAttributes.Builder audioAttributesBuilder = new AudioAttributes.Builder();
		audioAttributesBuilder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
		final SoundPool.Builder soundPoolBuilder = new SoundPool.Builder();
		soundPoolBuilder.setAudioAttributes(audioAttributesBuilder.build());
		soundPoolBuilder.setMaxStreams(1);
		fSoundPool = soundPoolBuilder.build();
		fSoundPool.setOnLoadCompleteListener(this);
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
		try
		{
			final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fFile));
			try
			{
				WaveUtil.generateSine(out, fBeepFrequency, fBeepDuration, bpm);
			}
			finally
			{
				out.close();
			}
			synchronized (fPlaying)
			{
				fSoundPool.autoPause();
				if (fSoundID != -1)
				{
					fSoundPool.unload(fSoundID);
					fSoundID = -1;
				}
			}
			fSoundPool.load(fFile.getAbsolutePath(), 1);
		}
		catch (final IOException e)
		{
			Log.e("TimeKeeper", "Failed to write WAV file to " + fFile.getAbsolutePath(), e);
		}
	}

	public void start(final int bpm)
	{
		synchronized (fPlaying)
		{
			fPlaying.value = true;
		}
		configure(bpm);
	}

	public void start()
	{
		synchronized (fPlaying)
		{
			fPlaying.value = true;
			if (fSoundID != -1)
			{
				fSoundPool.autoPause();
				fSoundPool.play(fSoundID, 1F, 1F, 1, -1, 1F);
			}
		}
	}

	public void stop()
	{
		synchronized (fPlaying)
		{
			fPlaying.value = false;
		}
		fSoundPool.autoPause();
	}
	
	@Override
	public void onLoadComplete(final SoundPool soundPool, final int sampleId, final int status)
	{
		synchronized (fPlaying)
        {
			fSoundID = sampleId;
			if (fPlaying.value)
			{
				fSoundPool.play(fSoundID, 1F, 1F, 1, -1, 1F);
			}
        }
	}
}
