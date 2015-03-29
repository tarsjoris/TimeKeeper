package com.tjoris.timekeeper;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class SoundGenerator
{
	private static final int kLOWEST_BPM = 50;
	private static final int kFREQ = 44100;
	private static final int kTONE_TIME = 10;
	private static final int kTONE_FREQ = 880;

	private final AudioTrack[] fAudioTracks;
	private int fDeck = 0;
	private int fLoopPoint;

	public SoundGenerator()
	{
		final int totalSamples = kFREQ * 60 / kLOWEST_BPM;
		final int toneSamples = kFREQ * kTONE_TIME / 1000;
		final byte[] buffer = new byte[totalSamples];
		final double f = 2d * Math.PI * ((double) kTONE_FREQ) / ((double) kFREQ);
		for (int i = 0; i < toneSamples; ++i)
		{
			final double value = Math.sin(((double) i) * f) * 255d;
			buffer[i] = (byte) Math.round(value);
		}
		fAudioTracks = new AudioTrack[2];
		for (int i = 0; i < fAudioTracks.length; ++i)
		{
			fAudioTracks[i] = new AudioTrack(AudioManager.STREAM_MUSIC, kFREQ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT, totalSamples, AudioTrack.MODE_STATIC);
			fAudioTracks[i].write(buffer, 0, buffer.length);
		}
		configure(120);
	}
	
	public void close()
	{
		stop();
		for (final AudioTrack track : fAudioTracks)
		{
			track.release();
		}
		Log.i("TimeKeeper", "Stopped.");
	}

	public void configure(final int bpm)
	{
		fLoopPoint = kFREQ * 60 / Math.max(kLOWEST_BPM, bpm);
		prepareOtherDeck();
	}

	public void start(final int bpm)
	{
		configure(bpm);
		start();
	}
	
	public void start()
	{
		stop();
		fDeck = 1 - fDeck;
		fAudioTracks[fDeck].play();
		prepareOtherDeck();
	}

	public void stop()
	{
		final AudioTrack track = fAudioTracks[fDeck];
		if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
		{
			track.stop();
		}
	}
	
	private void prepareOtherDeck()
	{
		final AudioTrack track = fAudioTracks[1 - fDeck];
		track.reloadStaticData();
		track.setPlaybackHeadPosition(0);
		track.setLoopPoints(0, fLoopPoint, -1);
	}
}
