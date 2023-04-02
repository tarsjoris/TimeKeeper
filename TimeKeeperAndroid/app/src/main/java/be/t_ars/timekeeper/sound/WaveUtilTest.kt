package be.t_ars.timekeeper.sound

// by Evan X. Merz
// www.thisisnotalabel.com

// Example Wav file input and output
// this was written for educational purposes, but feel free to use it for anything you like
// as long as you credit me appropriately ("wav IO based on code by Evan Merz")

// if you catch any bugs in this, or improve upon it significantly, send me the changes
// at evan at thisisnotalabel dot com, so we can share your changes with the world

import java.io.*

typealias OpenFile = (String) -> InputStream

object WaveUtilTest {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val countOff = WaveUtil.generateCountOff(this::openFile, 120)
            //val click = generateClick(880, 50, 120, 440, 60, 2)
            val click = WaveUtil.generateShakerLoop(this::openFile, 120)
            val buffer = ByteArray(countOff.sumOf { b -> b.size } + click.size * 4)
            var offset = 0
            WaveUtil.copyBytes(countOff[0], buffer, offset)
            offset += countOff[0].size
            WaveUtil.copyBytes(countOff[1], buffer, offset)
            offset += countOff[1].size
            WaveUtil.copyBytes(countOff[2], buffer, offset)
            offset += countOff[3].size
            WaveUtil.copyBytes(countOff[3], buffer, offset)
            offset += countOff[3].size
            repeat(4) {
                WaveUtil.copyBytes(click, buffer, offset)
                offset += click.size
            }
            FileOutputStream("click.wav").use { out ->
                save(out, 2, WaveUtil.kSAMPLES_PER_SECOND, 1, buffer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun openFile(name: String) =
        FileInputStream("app\\src\\main\\assets\\$name")

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

    // convert a short to a byte array
    private fun shortToByteArray(data: Int): ByteArray {
        return byteArrayOf((data and 0xff).toByte(), (data.ushr(8) and 0xff).toByte())
    }
}