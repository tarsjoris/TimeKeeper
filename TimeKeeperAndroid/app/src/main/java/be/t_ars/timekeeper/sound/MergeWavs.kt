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
                clickInput.skip(112)
                val clickDataSize = readInt(clickInput)
                Files.newInputStream(generateTrackPath(clickFile)).let(::BufferedInputStream)
                    .use { trackInput ->
                        trackInput.skip(112)
                        val trackDataSize = readInt(trackInput)
                        Files.newOutputStream(generateOutputPath(clickFile))
                            .let(::BufferedOutputStream)
                            .use { output ->
                                println("Merging $clickFile")
                                merge(
                                    clickInput,
                                    clickDataSize,
                                    trackInput,
                                    trackDataSize,
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
        clickDataSize: Int,
        trackInput: BufferedInputStream,
        trackDataSize: Int,
        output: DataOutputStream
    ) {
        val maxDataSize = max(clickDataSize, trackDataSize)
        val mergedDataSize = maxDataSize * 2
        val channelCount = 4
        val sampleRate = 44100
        val bytesPerChannel = 2

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
        output.write(shortToByteArray(1))
        // 22 - mono or stereo? 1 or 2? (or 5 or ???)
        output.write(shortToByteArray(channelCount))
        // 24 - samples per second (numbers per second)
        output.write(intToByteArray(sampleRate))
        // 28 - bytes per second
        output.write(intToByteArray(bytesPerChannel * channelCount * sampleRate))
        // 32 - # of bytes in one sample, for all channels
        output.write(shortToByteArray(bytesPerChannel * channelCount))
        // 34 - how many bits in a sample(number)? usually 16 or 24
        output.write(shortToByteArray(bytesPerChannel * 8))
        // 36 - data
        output.writeBytes("data")
        // 40 - how big is this data chunk
        output.write(intToByteArray(mergedDataSize))
        // 44 - the actual data itself - just a long string of numbers
        val silenceBuffer = ByteArray(bytesPerChannel * 2) {
            if (it % bytesPerChannel == 0) -128 else 0
        }
        val sampleBuffer = ByteArray(bytesPerChannel * 2)
        (0 until maxDataSize step sampleBuffer.size).forEach {
            if (it < clickDataSize) {
                clickInput.read(sampleBuffer)
                output.write(sampleBuffer)
            } else {
                output.write(silenceBuffer)
            }
            if (it < trackDataSize) {
                trackInput.read(sampleBuffer)
                output.write(sampleBuffer)
            } else {
                output.write(silenceBuffer)
            }
        }
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
        val b1 = input.read()
        val b2 = input.read()
        val b3 = input.read()
        val b4 = input.read()
        val result = b1 + (b2 shl 8) + (b3 shl 16) + (b4 shl 24)
        return result
    }

    // convert a short to a byte array
    private fun shortToByteArray(data: Int): ByteArray {
        return byteArrayOf((data and 0xff).toByte(), (data.ushr(8) and 0xff).toByte())
    }
}