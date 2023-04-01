package be.t_ars.timekeeper.util

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

typealias OpenFile = (String) -> InputStream

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

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val countOff = generateCountOff(this::openFile, 120, 2)
            //val click = generateClick(880, 50, 120, 440, 60, 2)
            val click = generateShakerLoop(this::openFile, 120, 2)
            val buffer = ByteArray(countOff.sumOf { b -> b.size } + click.size * 4)
            var offset = 0
            copyBytes(countOff[0], buffer, offset, 2)
            offset += countOff[0].size
            copyBytes(countOff[1], buffer, offset, 2)
            offset += countOff[1].size
            copyBytes(countOff[2], buffer, offset, 2)
            offset += countOff[3].size
            copyBytes(countOff[3], buffer, offset, 2)
            offset += countOff[3].size
            repeat(4) {
                copyBytes(click, buffer, offset, 2)
                offset += click.size
            }
            FileOutputStream("click.wav").use { out ->
                save(out, 2, kSAMPLES_PER_SECOND, 1, buffer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun openFile(name: String) =
        FileInputStream("app\\src\\main\\assets\\$name")

    fun generateClick(
        beepFrequency: Int,
        beepDurationMillis: Int,
        bpm: Int,
        divisionFrequency: Int,
        divisionVolume: Int,
        divisionCount: Int,
        channelCount: Int
    ): ByteArray {
        val totalSamples = kSAMPLES_PER_SECOND * 60 / bpm
        val buffer = ByteArray(totalSamples * channelCount) { -128 }
        writeBeepInBuffer(beepFrequency, beepDurationMillis, 100, buffer, channelCount, 0)
        if (divisionCount in 2..7) {
            (1 until divisionCount).forEach { subDivisionIndex ->
                val samplesOffset =
                    (totalSamples.toDouble() / divisionCount.toDouble() * subDivisionIndex.toDouble()).roundToInt()
                writeBeepInBuffer(
                    divisionFrequency,
                    beepDurationMillis,
                    divisionVolume,
                    buffer,
                    channelCount,
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
        channelCount: Int,
        samplesOffset: Int
    ) {
        val maxVolume: Double = if (maxVolume in 1..100) maxVolume.toDouble() / 100.0 else 1.0
        val desiredSampleCount =
            kSAMPLES_PER_SECOND.toDouble() * beepDurationMillis.toDouble() / 1000.0
        val samplesBetweenZeroCrossings =
            kSAMPLES_PER_SECOND.toDouble() / sinesPerSecond.toDouble() / 2.0
        val actualSampleCount =
            floor((desiredSampleCount / samplesBetweenZeroCrossings).roundToLong() * samplesBetweenZeroCrossings).toInt()
        val fadeInSampleCount: Int = (actualSampleCount.toDouble() / 3.0).toInt()
        val amplitudeStepPerSample =
            2.0 * Math.PI * sinesPerSecond.toDouble() / kSAMPLES_PER_SECOND.toDouble()
        (0 until actualSampleCount).forEach { s ->
            val fadeCorrection = if (s < fadeInSampleCount)
                s.toDouble() / fadeInSampleCount.toDouble()
            else
                (actualSampleCount - s).toDouble() / (actualSampleCount.toDouble() - fadeInSampleCount.toDouble())

            val amplitude: Double = sin(s.toDouble() * amplitudeStepPerSample)
            val volumeAdjustedAmplitude: Double = amplitude * fadeCorrection * maxVolume
            val value: Double = (volumeAdjustedAmplitude + 1.0) * 255.0 / 2.0
            val byte: Byte = toByte(value.roundToInt())
            val sampleIndex: Int = (samplesOffset + s) * channelCount
            (0 until 2).forEach { channel ->
                val index = sampleIndex + channel
                if (index in buffer.indices)
                {
                    buffer[index] = byte
                }
            }
        }
    }

    fun generateShakerLoop(context: Context, bpm: Int, channelCount: Int) =
        generateShakerLoop({ name -> context.assets.open(name) }, bpm, channelCount)

    private fun generateShakerLoop(openFile: OpenFile, bpm: Int, channelCount: Int): ByteArray {
        val totalSamples = kSAMPLES_PER_SECOND * 60 / bpm
        val buffer = ByteArray(totalSamples * channelCount) { -128 }
        val samples = readSamples(openFile, "shakerloop")
        copyBytes(samples[0], buffer, 0, channelCount)
        copyBytes(samples[1], buffer, (totalSamples.toDouble() / 4.0 * 1.0).roundToInt() * channelCount, channelCount)
        copyBytes(samples[2], buffer, (totalSamples.toDouble() / 4.0 * 2.0).roundToInt() * channelCount, channelCount)
        copyBytes(samples[3], buffer, (totalSamples.toDouble() / 4.0 * 3.0).roundToInt() * channelCount, channelCount)
        return buffer
    }

    fun generateCountOff(context: Context, bpm: Int, channelCount: Int) =
        generateCountOff({ name -> context.assets.open(name) }, bpm, channelCount)

    private fun generateCountOff(openFile: OpenFile, bpm: Int, channelCount: Int): Array<ByteArray> {
        val totalSamples = kSAMPLES_PER_SECOND * 60 / bpm
        val buffers = Array(4) { ByteArray(totalSamples * channelCount) { -128 } }
        val samples = readSamples(openFile, "countdown")
        copyBytes(samples[0], buffers[0], 0, channelCount)
        copyBytes(samples[1], buffers[1], 0, channelCount)
        copyBytes(samples[2], buffers[2], 0, channelCount)
        copyBytes(samples[3], buffers[3], 0, channelCount)
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

    private fun copyBytes(from: ByteArray, to: ByteArray, bufferOffset: Int, channelCount: Int) {
        (0 until from.size / 2).forEach { sampleIndex ->
            val sourceIndex = sampleIndex * 2
            val targetIndex = bufferOffset + sampleIndex * channelCount
            if ((targetIndex + 1) in to.indices) {
                to[targetIndex] = from[sourceIndex]
                to[targetIndex + 1] = from[sourceIndex + 1]
            }
        }
    }

    private fun save(
        out: OutputStream,
        channelCount: Int,
        sampleRate: Int,
        bytesPerChannel: Int,
        data: ByteArray
    ) {
        val outFile = DataOutputStream(out)

        // write the wav file per the wav file format
        // 00 - RIFF
        outFile.writeBytes("RIFF")
        // 04 - how big is the rest of this file?
        outFile.write(intToByteArray(36 + data.size))
        // 08 - WAVE
        outFile.writeBytes("WAVE")
        // 12 - fmt
        outFile.writeBytes("fmt ")
        // 16 - size of this chunk
        outFile.write(intToByteArray(16))
        // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
        outFile.write(shortToByteArray(1))
        // 22 - mono or stereo? 1 or 2? (or 5 or ???)
        outFile.write(shortToByteArray(channelCount))
        // 24 - samples per second (numbers per second)
        outFile.write(intToByteArray(sampleRate))
        // 28 - bytes per second
        outFile.write(intToByteArray(bytesPerChannel * channelCount * sampleRate))
        // 32 - # of bytes in one sample, for all channels
        outFile.write(shortToByteArray(bytesPerChannel * channelCount))
        // 34 - how many bits in a sample(number)? usually 16 or 24
        outFile.write(shortToByteArray(bytesPerChannel * 8))
        // 36 - data
        outFile.writeBytes("data")
        // 40 - how big is this data chunk
        outFile.write(intToByteArray(data.size))
        // 44 - the actual data itself - just a long string of numbers
        outFile.write(data)
    }

    // ===========================
    // CONVERT JAVA TYPES TO BYTES
    // ===========================
    // returns a byte array of length 4
    private fun intToByteArray(i: Int): ByteArray {
        val b = ByteArray(4)
        b[0] = (i and 0x00FF).toByte()
        b[1] = (i shr 8 and 0x000000FF).toByte()
        b[2] = (i shr 16 and 0x000000FF).toByte()
        b[3] = (i shr 24 and 0x000000FF).toByte()
        return b
    }

    private fun readInt(input: InputStream): Int {
        var value = input.read()
        value += input.read() shl 8
        value += input.read() shl 16
        value += input.read() shl 24
        return value
    }

    // convert a short to a byte array
    private fun shortToByteArray(data: Int): ByteArray {
        return byteArrayOf((data and 0xff).toByte(), (data.ushr(8) and 0xff).toByte())
    }

    // convert a short to a byte array
    private fun toByte(data: Int): Byte {
        return (data and 0xff).toByte()
    }
}