package be.t_ars.timekeeper.sound

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.math.max

private const val FORMAT_PCM = 1
private const val FORMAT_FLOAT = 3

private data class WaveHeader(
    val audioFormat: Int,
    val numChannels: Int,
    val sampleRate: Long,
    val bitsPerSample: Int,
    val dataSize: Long,
)

object MergeWavs {
    @JvmStatic
    fun main(args: Array<String>) {
        Files.walk(Paths.get("D:\\Home\\Bands\\Acoustic Delight\\Samples\\TODO"))
            .filter { path -> path.isRegularFile() }
            .filter { path -> path.extension == "wav" }
            .filter { path -> path.fileName.toString().indexOf(" click ") != -1 }
            .forEach(MergeWavs::merge)
    }

    private fun merge(clickFile: Path) {
        Files.newInputStream(clickFile).let(::BufferedInputStream)
            .use { clickInput ->
                val clickHeader = readHeader(clickInput)
                Files.newInputStream(generateTrackPath(clickFile)).let(::BufferedInputStream)
                    .use { trackInput ->
                        val trackHeader = readHeader(trackInput)
                        Files.newOutputStream(generateOutputPath(clickFile))
                            .let(::BufferedOutputStream)
                            .use { output ->
                                println("Merging $clickFile")
                                merge(
                                    clickInput,
                                    clickHeader,
                                    trackInput,
                                    trackHeader,
                                    DataOutputStream(output)
                                )
                            }
                    }
            }
    }

    private fun generateTrackPath(clickFile: Path) =
        clickFile.parent.resolve(
            clickFile.fileName.toString().substringBefore(" click ") + "AR_ph.wav"
        )

    private fun generateOutputPath(clickFile: Path) =
        clickFile.parent.resolve(
            clickFile.fileName.toString().substringBefore(" click ") + " merged.wav"
        )

    private fun merge(
        clickInput: BufferedInputStream,
        clickHeader: WaveHeader,
        trackInput: BufferedInputStream,
        trackHeader: WaveHeader,
        output: DataOutputStream
    ) {
        if (clickHeader.audioFormat != trackHeader.audioFormat) {
            throw IllegalArgumentException("Mismatching audio formats ${clickHeader.audioFormat} and ${trackHeader.audioFormat}")
        }
        if (clickHeader.sampleRate != trackHeader.sampleRate) {
            throw IllegalArgumentException("Mismatching sample rates ${clickHeader.sampleRate} and ${trackHeader.sampleRate}")
        }
        if (clickHeader.bitsPerSample != trackHeader.bitsPerSample) {
            throw IllegalArgumentException("Mismatching bites per sample rates ${clickHeader.bitsPerSample} and ${trackHeader.bitsPerSample}")
        }
        val maxDataSize = max(clickHeader.dataSize, trackHeader.dataSize)
        val mergedDataSize = maxDataSize * 2
        val numChannels = clickHeader.numChannels + trackHeader.numChannels
        val sampleRate = clickHeader.sampleRate
        val bytesPerChannel = clickHeader.bitsPerSample / 8

        // write the wav file per the wav file format
        // 00 - RIFF
        output.writeBytes("RIFF")
        // 04 - how big is the rest of this file?
        output.write(intToByteArray(36 + mergedDataSize))
        // 08 - WAVE
        output.writeBytes("WAVE")
        // 12 - fmt
        output.writeBytes("fmt ")
        // 16 - size of this chunk
        output.write(intToByteArray(16))
        // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
        output.write(shortToByteArray(clickHeader.audioFormat))
        // 22 - mono or stereo? 1 or 2? (or 5 or ???)
        output.write(shortToByteArray(numChannels))
        // 24 - samples per second (numbers per second)
        output.write(intToByteArray(sampleRate))
        // 28 - bytes per second
        output.write(intToByteArray(bytesPerChannel * numChannels * sampleRate))
        // 32 - # of bytes in one sample, for all channels
        output.write(shortToByteArray(bytesPerChannel * numChannels))
        // 34 - how many bits in a sample(number)? usually 16 or 24
        output.write(shortToByteArray(bytesPerChannel * 8))
        // 36 - data
        output.writeBytes("data")
        // 40 - how big is this data chunk
        output.write(intToByteArray(mergedDataSize))
        // 44 - the actual data itself - just a long string of numbers
        val silenceBuffer = ByteArray(bytesPerChannel * 2) { 0 }
        val sampleBuffer = ByteArray(bytesPerChannel * 2)
        (0 until maxDataSize step sampleBuffer.size.toLong()).forEach {
            if (it < clickHeader.dataSize) {
                clickInput.read(sampleBuffer)
                output.write(sampleBuffer)
            } else {
                output.write(silenceBuffer)
            }
            if (it < trackHeader.dataSize) {
                trackInput.read(sampleBuffer)
                output.write(sampleBuffer)
            } else {
                output.write(silenceBuffer)
            }
        }
    }

    private fun readHeader(input: InputStream): WaveHeader {
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

    private fun intToByteArray(data: Long): ByteArray {
        val b = ByteArray(4)
        b[0] = (data and 0x00FF).toByte()
        b[1] = (data shr 8 and 0x000000FF).toByte()
        b[2] = (data shr 16 and 0x000000FF).toByte()
        b[3] = (data shr 24 and 0x000000FF).toByte()
        return b
    }

    private fun shortToByteArray(data: Int): ByteArray {
        return byteArrayOf((data and 0xff).toByte(), (data.ushr(8) and 0xff).toByte())
    }
}