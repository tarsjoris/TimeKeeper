package com.tjoris.timekeeper;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundGenerator
{
	private static final int kFREQ = 44100;
	private static final int kTONE_TIME = 20;
	private static final int kTONE_FREQ = 440;
	
	private final AudioTrack fAudioTrack;
	
	public SoundGenerator(final int bpm)
	{
		final int totalSamples = kFREQ * 60 / bpm;
		final int toneSamples = kFREQ * kTONE_TIME / 1000;
		fAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, kFREQ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT, totalSamples, AudioTrack.MODE_STATIC);
		final byte[] buffer = new byte[totalSamples];
		final double f = 2d * Math.PI * ((double)kTONE_FREQ) / ((double)kFREQ);
		for (int i = 0; i < toneSamples; ++i)
		{
			final double value = Math.sin(((double)i) * f) * 255d;
			buffer[i] = (byte)Math.round(value);
		}
		fAudioTrack.write(buffer, 0, buffer.length);
		fAudioTrack.flush();
		fAudioTrack.setLoopPoints(0, totalSamples, -1);
		fAudioTrack.play();
	}
	
	public void stop()
	{
		fAudioTrack.stop();
	}
	
}
