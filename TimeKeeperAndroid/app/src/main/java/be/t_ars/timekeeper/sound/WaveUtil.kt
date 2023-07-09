package be.t_ars.timekeeper.sound

// by Evan X. Merz
// www.thisisnotalabel.com

// Example Wav file input and output
// this was written for educational purposes, but feel free to use it for anything you like
// as long as you credit me appropriately ("wav IO based on code by Evan Merz")

// if you catch any bugs in this, or improve upon it significantly, send me the changes
// at evan at thisisnotalabel dot com, so we can share your changes with the world

import android.content.Context
import java.io.*
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

object WaveUtil {
    const val kSAMPLES_PER_SECOND = 44100

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
    fun generateClick(
        beepFrequency: Int,
        beepDurationMillis: Int,
        bpm: Int,
        divisionFrequency: Int,
        divisionVolume: Int,
        divisionCount: Int
    ): ByteArray {
        val totalSamples = kSAMPLES_PER_SECOND * 60 / bpm
        val buffer = ByteArray(totalSamples * 2) { -128 }
        writeBeepInBuffer(beepFrequency, beepDurationMillis, 100, buffer, 0)
        if (divisionCount in 2..7) {
            (1 until divisionCount).forEach { subDivisionIndex ->
                val samplesOffset =
                    (totalSamples.toDouble() / divisionCount.toDouble() * subDivisionIndex.toDouble()).roundToInt()
                writeBeepInBuffer(
                    divisionFrequency,
                    beepDurationMillis,
                    divisionVolume,
                    buffer,
                    samplesOffset
                )
            }
        }
        return buffer
    }

    private fun writeBeepInBuffer(
        sinesPerSecond: Int,
        beepDurationMillis: Int,
        maxVolume: Int,
        buffer: ByteArray,
        samplesOffset: Int
    ) {
        val maxVolume: Double = if (maxVolume in 1..100) maxVolume.toDouble() / 100.0 else 1.0
        val desiredSampleCount =
            kSAMPLES_PER_SECOND.toDouble() * beepDurationMillis.toDouble() / 1000.0
        val samplesBetweenZeroCrossings =
            kSAMPLES_PER_SECOND.toDouble() / sinesPerSecond.toDouble() / 2.0
        val actualSampleCount =
            floor((desiredSampleCount / samplesBetweenZeroCrossings).roundToLong() * samplesBetweenZeroCrossings).toInt()
        val amplitudeStepPerSample =
            2.0 * Math.PI * sinesPerSecond.toDouble() / kSAMPLES_PER_SECOND.toDouble()
        (0 until actualSampleCount).forEach { s ->
            val amplitude: Double = sin(s.toDouble() * amplitudeStepPerSample)
            val volumeAdjustedAmplitude: Double = amplitude * maxVolume
            val value: Double = (volumeAdjustedAmplitude + 1.0) * 255.0 / 2.0
            val byte: Byte = toByte(value.roundToInt())
            val index: Int = (samplesOffset + s) * 2
            if (index in buffer.indices) {
                buffer[index] = byte
            }
            if (index + 1 in buffer.indices) {
                buffer[index + 1] = byte
            }
        }
    }

    fun generateShakerLoop(context: Context, bpm: Int) =
        generateShakerLoop({ name -> context.assets.open(name) }, bpm)

    fun generateShakerLoop(openFile: OpenFile, bpm: Int): ByteArray {
        val totalSamples = kSAMPLES_PER_SECOND * 60 / bpm
        val buffer = ByteArray(totalSamples * 2) { -128 }
        val samples = readSamples(openFile, "shakerloop")
        copyBytes(samples[0], buffer, 0)
        copyBytes(samples[1], buffer, (totalSamples.toDouble() / 4.0 * 1.0).roundToInt() * 2)
        copyBytes(samples[2], buffer, (totalSamples.toDouble() / 4.0 * 2.0).roundToInt() * 2)
        copyBytes(samples[3], buffer, (totalSamples.toDouble() / 4.0 * 3.0).roundToInt() * 2)
        return buffer
    }

    fun generateCountOff(context: Context, bpm: Int) =
        generateCountOff({ name -> context.assets.open(name) }, bpm)

    fun generateCountOff(openFile: OpenFile, bpm: Int): Array<ByteArray> {
        val totalSamples = kSAMPLES_PER_SECOND * 60 / bpm
        val buffers = Array(4) { ByteArray(totalSamples * 2) { -128 } }
        val samples = readSamples(openFile, "countdown")
        copyBytes(samples[0], buffers[0], 0)
        copyBytes(samples[1], buffers[1], 0)
        copyBytes(samples[2], buffers[2], 0)
        copyBytes(samples[3], buffers[3], 0)
        return buffers
    }

    private fun readSamples(openFile: OpenFile, prefix: String) =
        arrayOf(
            readSample(openFile, "${prefix}1.wav"),
            readSample(openFile, "${prefix}2.wav"),
            readSample(openFile, "${prefix}3.wav"),
            readSample(openFile, "${prefix}4.wav")
        )

    private fun readSample(openFile: OpenFile, filename: String): ByteArray {
        openFile(filename).use { input ->
            input.skip(40)
            val size = readInt(input)
            val buffer = ByteArray(size)
            input.read(buffer)
            return buffer
        }
    }

    fun copyBytes(from: ByteArray, to: ByteArray, bufferOffset: Int) {
        from.indices.forEach { index ->
            val targetIndex = bufferOffset + index
            if (targetIndex in to.indices) {
                to[targetIndex] = from[index]
            }
        }
    }

    private fun readInt(input: InputStream): Int {
        var value = input.read()
        value += input.read() shl 8
        value += input.read() shl 16
        value += input.read() shl 24
        return value
    }

    // convert a short to a byte array
    private fun toByte(data: Int): Byte {
        return (data and 0xff).toByte()
    }
}