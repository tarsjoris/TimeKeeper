package com.tjoris.timekeeper.desktop.util;

// by Evan X. Merz
// www.thisisnotalabel.com

// Example Wav file input and output
// this was written for educational purposes, but feel free to use it for anything you like 
// as long as you credit me appropriately ("wav IO based on code by Evan Merz")

// if you catch any bugs in this, or improve upon it significantly, send me the changes
// at evan at thisisnotalabel dot com, so we can share your changes with the world

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WaveUtil
{
	private static final int kFREQ = 44100;
	
/*
     WAV File Specification
     FROM http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
    The canonical WAVE format starts with the RIFF header:
    0         4   ChunkID          Contains the letters "RIFF" in ASCII form
                                   (0x52494646 big-endian form).
    4         4   ChunkSize        36 + SubChunk2Size, or more precisely:
                                   4 + (8 + SubChunk1Size) + (8 + SubChunk2Size)
                                   This is the size of the rest of the chunk 
                                   following this number.  This is the size of the 
                                   entire file in bytes minus 8 bytes for the
                                   two fields not included in this count:
                                   ChunkID and ChunkSize.
    8         4   Format           Contains the letters "WAVE"
                                   (0x57415645 big-endian form).

    The "WAVE" format consists of two subchunks: "fmt " and "data":
    The "fmt " subchunk describes the sound data's format:
    12        4   Subchunk1ID      Contains the letters "fmt "
                                   (0x666d7420 big-endian form).
    16        4   Subchunk1Size    16 for PCM.  This is the size of the
                                   rest of the Subchunk which follows this number.
    20        2   AudioFormat      PCM = 1 (i.e. Linear quantization)
                                   Values other than 1 indicate some 
                                   form of compression.
    22        2   NumChannels      Mono = 1, Stereo = 2, etc.
    24        4   SampleRate       8000, 44100, etc.
    28        4   ByteRate         == SampleRate * NumChannels * BitsPerSample/8
    32        2   BlockAlign       == NumChannels * BitsPerSample/8
                                   The number of bytes for one sample including
                                   all channels. I wonder what happens when
                                   this number isn't an integer?
    34        2   BitsPerSample    8 bits = 8, 16 bits = 16, etc.

    The "data" subchunk contains the size of the data and the actual sound:
    36        4   Subchunk2ID      Contains the letters "data"
                                   (0x64617461 big-endian form).
    40        4   Subchunk2Size    == NumSamples * NumChannels * BitsPerSample/8
                                   This is the number of bytes in the data.
                                   You can also think of this as the size
                                   of the read of the subchunk following this 
                                   number.
    44        *   Data             The actual sound data.


NOTE TO READERS:

The thing that makes reading wav files tricky is that java has no unsigned types.  This means that the
binary data can't just be read and cast appropriately.  Also, we have to use larger types
than are normally necessary.

In many languages including java, an integer is represented by 4 bytes.  The issue here is
that in most languages, integers can be signed or unsigned, and in wav files the  integers
are unsigned.  So, to make sure that we can store the proper values, we have to use longs
to hold integers, and integers to hold shorts.

Then, we have to convert back when we want to save our wav data.

It's complicated, but ultimately, it just results in a few extra functions at the bottom of
this file.  Once you understand the issue, there is no reason to pay any more attention
to it.


ALSO:

This code won't read ALL wav files.  This does not use to full specification.  It just uses
a trimmed down version that most wav files adhere to.


*/
	
	public static void main(final String[] args)
    {
		try
		{
		    final OutputStream out = new FileOutputStream("beep.wav");
		    try
		    {
		    	generateSine(out, 440, 20, 120);
		    }
		    finally
		    {
		    	out.close();
		    }
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
    }
	
	public static void generateSine(final OutputStream out, final int beepFrequency, final int beepDuration, final int bpm) throws IOException
	{
		final int totalSamples = kFREQ * 60 * 2 / bpm;
		final double requestedToneSamples = (double)kFREQ * (double)beepDuration / 1000d;
		final double samplesPerSine = (double)kFREQ / (double)beepFrequency / 2d;
		final int toneSamples = (int)Math.floor(Math.round(requestedToneSamples / samplesPerSine) * samplesPerSine);
		final byte[] buffer = new byte[totalSamples];
		final double f = 2d * Math.PI * ((double) beepFrequency) / ((double) kFREQ);
		int i;
		for (i = 0; i < toneSamples; ++i)
		{
			final double value = (Math.sin(((double) i) * f) + 1d) * 255d / 2d;
			buffer[i * 2 + 1] = buffer[i * 2] = toByte((int)Math.round(value));
		}
		for (i = i * 2 ;i < totalSamples; ++i)
		{
			buffer[i] = toByte(128);
		}
		save(out, 2, kFREQ, 1, buffer);
	}

	public static void save(final OutputStream out, final int channelCount, final int sampleRate, final int bytesPerChannel, final byte[] data) throws IOException
	{
		final DataOutputStream outFile = new DataOutputStream(out);

		// write the wav file per the wav file format
		// 00 - RIFF
		outFile.writeBytes("RIFF");
		// 04 - how big is the rest of this file?
		outFile.write(intToByteArray(36 + data.length));
		// 08 - WAVE
		outFile.writeBytes("WAVE");
		// 12 - fmt
		outFile.writeBytes("fmt ");
		// 16 - size of this chunk
		outFile.write(intToByteArray(16));
		// 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
		outFile.write(shortToByteArray(1));
		// 22 - mono or stereo? 1 or 2? (or 5 or ???)
		outFile.write(shortToByteArray(channelCount));
		// 24 - samples per second (numbers per second)
		outFile.write(intToByteArray(sampleRate));
		// 28 - bytes per second
		outFile.write(intToByteArray(bytesPerChannel * channelCount * sampleRate)); 
		// 32 - # of bytes in one sample, for all channels
		outFile.write(shortToByteArray(bytesPerChannel * channelCount)); 
		// 34 - how many bits in a sample(number)? usually 16 or 24
		outFile.write(shortToByteArray(bytesPerChannel * 8));
		// 36 - data
		outFile.writeBytes("data");
		// 40 - how big is this data chunk
		outFile.write(intToByteArray(data.length)); 
		// 44 - the actual data itself - just a long string of numbers
		outFile.write(data); 
	}

	// ===========================
	// CONVERT JAVA TYPES TO BYTES
	// ===========================
	// returns a byte array of length 4
	private static byte[] intToByteArray(int i)
	{
		byte[] b = new byte[4];
		b[0] = (byte) (i & 0x00FF);
		b[1] = (byte) ((i >> 8) & 0x000000FF);
		b[2] = (byte) ((i >> 16) & 0x000000FF);
		b[3] = (byte) ((i >> 24) & 0x000000FF);
		return b;
	}

	// convert a short to a byte array
	private static byte[] shortToByteArray(int data)
	{
		return new byte[] { (byte) (data & 0xff), (byte) ((data >>> 8) & 0xff) };
	}

	// convert a short to a byte array
	private static byte toByte(int data)
	{
		return (byte) (data & 0xff);
	}
}