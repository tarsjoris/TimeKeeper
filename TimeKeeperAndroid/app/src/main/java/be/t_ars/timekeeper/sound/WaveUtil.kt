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

data class WaveHeader(
    val audioFormat: Int,
    val numChannels: Int,
    val sampleRate: Long,
    val bitsPerSample: Int,
    val dataSize: Long,
)

const val SAMPLES_PER_SECOND = 44100

private const val FORMAT_PCM_8BIT = 1
private const val FORMAT_FLOAT = 3

typealias OpenFile = (String) -> InputStream

class WaveUtil(private val openFile: OpenFile) {
    constructor(context: Context) : this({ name -> context.assets.open(name) })

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
        divisionCount: Int,
        beatCount: Int,
    ): ByteArray {
        val divisionCount = if (divisionCount in 2..7) divisionCount else 1
        val beatCount = if (beatCount in 1..7) beatCount else 1
        val samplesPerBeat = SAMPLES_PER_SECOND * 60 / bpm
        val buffer = ByteArray(beatCount * samplesPerBeat * 2) { -128 }
        (0 until beatCount).forEach { beatIndex ->
            val beatFrequency = if (beatIndex == 0) beepFrequency else divisionFrequency
            val sampleCountOffset = beatIndex * samplesPerBeat
            writeBeepInBuffer(beatFrequency, beepDurationMillis, 100, buffer, sampleCountOffset)
            (1 until divisionCount).forEach { subDivisionIndex ->
                val divisionSampleCountOffset = sampleCountOffset +
                        (samplesPerBeat.toDouble() / divisionCount.toDouble() * subDivisionIndex.toDouble()).roundToInt()
                writeBeepInBuffer(
                    divisionFrequency,
                    beepDurationMillis,
                    divisionVolume,
                    buffer,
                    divisionSampleCountOffset
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
        sampleCountOffset: Int
    ) {
        val maxVolume: Double = if (maxVolume in 1..100) maxVolume.toDouble() / 100.0 else 1.0
        val desiredSampleCount =
            SAMPLES_PER_SECOND.toDouble() * beepDurationMillis.toDouble() / 1000.0
        val samplesBetweenZeroCrossings =
            SAMPLES_PER_SECOND.toDouble() / sinesPerSecond.toDouble() / 2.0
        val actualSampleCount =
            floor((desiredSampleCount / samplesBetweenZeroCrossings).roundToLong() * samplesBetweenZeroCrossings).toInt()
        val amplitudeStepPerSample =
            2.0 * Math.PI * sinesPerSecond.toDouble() / SAMPLES_PER_SECOND.toDouble()
        (0 until actualSampleCount).forEach { s ->
            val amplitude: Double = sin(s.toDouble() * amplitudeStepPerSample)
            val volumeAdjustedAmplitude: Double = amplitude * maxVolume
            val value: Double = (volumeAdjustedAmplitude + 1.0) * 255.0 / 2.0
            val byte: Byte = toByte(value.roundToInt())
            val index: Int = (sampleCountOffset + s) * 2
            if (index in buffer.indices) {
                buffer[index] = byte
            }
            if (index + 1 in buffer.indices) {
                buffer[index + 1] = byte
            }
        }
    }

    fun generateShakerLoop(bpm: Int, divisionCount: Int): ByteArray {
        val divisionCount = if (divisionCount in 1..4) divisionCount else 4
        val samplesPerBeat = SAMPLES_PER_SECOND * 60 / bpm
        val buffer = ByteArray(samplesPerBeat * 2) { -128 }
        val samples = readSamples("shakerloop", 4)
        for (i in 0 until divisionCount) {
            copyBytes(
                samples[i],
                buffer,
                (samplesPerBeat.toDouble() / divisionCount.toDouble() * i.toDouble()).roundToInt() * 2
            )
        }
        return buffer
    }

    fun generateCowbell(
        bpm: Int,
        divisionCount: Int,
        beatCount: Int,
        divisionVolume: Int
    ): ByteArray {
        val samplesPerBeat = SAMPLES_PER_SECOND * 60 / bpm
        val buffer = ByteArray(beatCount * samplesPerBeat * 2) { -128 }
        val high = readSample("high.wav")
        val low = readSample("low.wav")
        val quiet = adjustVolume(low, divisionVolume)
        copyBytes(high, buffer, 0)
        for (i in 0 until beatCount) {
            for (j in 0 until divisionCount) {
                val sample = if (j == 0)
                    if (i == 0) high else low
                else
                    quiet
                copyBytes(
                    sample,
                    buffer,
                    ((i.toDouble() + j.toDouble() / divisionCount.toDouble()) * samplesPerBeat.toDouble()).toInt() * 2
                )
            }
        }
        return buffer
    }

    private fun generateSample(bpm: Int, filename: String): ByteArray {
        val samplesPerBeat = SAMPLES_PER_SECOND * 60 / bpm
        val buffer = ByteArray(samplesPerBeat * 2) { -128 }
        val sample = readSample(filename)
        copyBytes(sample, buffer, 0)
        return buffer
    }

    fun generateCountOff(bpm: Int, beats: Int): ByteArray {
        val samplesPerBeat = SAMPLES_PER_SECOND * 60 / bpm
        val buffer = ByteArray(beats * samplesPerBeat * 2) { -128 }
        val samples = readSamples("countdown", beats)
        for (i in 0 until beats) {
            copyBytes(
                samples[i],
                buffer,
                (samplesPerBeat.toDouble() * i.toDouble()).roundToInt() * 2
            )
        }
        return buffer
    }

    private fun readSamples(prefix: String, beats: Int) =
        Array(beats) { readSample("${prefix}${it + 1}.wav") }

    private fun readSample(filename: String): ByteArray {
        openFile(filename).use { input ->
            val header = readHeader(input)
            if (header.audioFormat != FORMAT_PCM_8BIT) {
                throw IllegalArgumentException("Only 8bit PCM is supported")
            }
            if (header.bitsPerSample != 8) {
                throw IllegalArgumentException("Only 8bit PCM is supported")
            }
            if (header.sampleRate != SAMPLES_PER_SECOND.toLong()) {
                throw IllegalArgumentException("Only $SAMPLES_PER_SECOND samples per second are supported")
            }
            if (header.numChannels != 2) {
                throw IllegalArgumentException("Only 2 channels are supported")
            }
            val buffer = ByteArray(header.dataSize.toInt())
            input.read(buffer)
            return buffer
        }
    }

    companion object {
        fun readHeader(input: InputStream): WaveHeader {
            if (readWord(input) != "RIFF") {
                throw IllegalArgumentException("Expected 'RIFF'")
            }
            input.skip(4) // skip ChunkSize
            if (readWord(input) != "WAVE") {
                throw IllegalArgumentException("Expected 'WAVE'")
            }
            while (readWord(input) != "fmt ") {
                val chunkSize = read4ByteNumber(input)
                input.skip(chunkSize)
            }
            if (read4ByteNumber(input) != 16L) {
                throw IllegalArgumentException("Expected fmt size 16")
            }
            val audioFormat = read2ByteNumber(input)
            val numChannels = read2ByteNumber(input)
            val sampleRate = read4ByteNumber(input)
            input.skip(4) // skip ByteRate
            input.skip(2) // skip BlockAlign
            val bitsPerSample = read2ByteNumber(input)
            while (readWord(input) != "data") {
                val chunkSize = read4ByteNumber(input)
                input.skip(chunkSize)
            }
            val dataSize = read4ByteNumber(input)
            return WaveHeader(audioFormat, numChannels, sampleRate, bitsPerSample, dataSize)
        }

        private fun adjustVolume(input: ByteArray, volumePercentage: Int) =
            ByteArray(input.size) {
                val amplitude = input[it].toUByte().toInt() - 128
                val adjustedAmplitude =
                    (amplitude.toDouble() * volumePercentage.toDouble() / 100.0).roundToInt()
                (adjustedAmplitude + 128).toByte()
            }

        fun copyBytes(from: ByteArray, to: ByteArray, bufferOffset: Int) {
            from.indices.forEach { index ->
                val targetIndex = bufferOffset + index
                if (targetIndex in to.indices) {
                    to[targetIndex] = from[index]
                }
            }
        }

        private fun read4ByteNumber(input: InputStream): Long {
            val b1 = input.read().toLong()
            val b2 = input.read().toLong()
            val b3 = input.read().toLong()
            val b4 = input.read().toLong()
            return b1 + (b2 shl 8) + (b3 shl 16) + (b4 shl 24)
        }

        private fun read2ByteNumber(input: InputStream): Int {
            val b1 = input.read()
            val b2 = input.read()
            return b1 + (b2 shl 8)
        }

        private fun readWord(input: InputStream) =
            (0..3)
                .map {
                    input.read().toChar()
                }
                .joinToString("")

        // convert a short to a byte array
        private fun toByte(data: Int) =
            (data and 0xff).toByte()
    }
}