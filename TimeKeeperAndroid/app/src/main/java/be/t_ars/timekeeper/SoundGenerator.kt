package be.t_ars.timekeeper

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.util.WaveUtil
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

private const val CLICK_ONLY_CHANNEL_COUNT = 2
private const val TRACK_AND_CLICK_CHANNEL_COUNT = 4

class SoundGenerator(
    private val context: Context,
    private val fBeepFrequency: Int,
    private val fBeepDuration: Int,
    private val fDivisionFrequency: Int,
    private val fDivisionVolume: Int
) {
    private var fPlaying = AtomicBoolean(false)
    private var fLastClick: ClickDescription? = null

    init {
        val audioAttributesBuilder = AudioAttributes.Builder()
        audioAttributesBuilder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        val soundPoolBuilder = SoundPool.Builder()
        soundPoolBuilder.setAudioAttributes(audioAttributesBuilder.build())
        soundPoolBuilder.setMaxStreams(1)
    }

    fun close() {
        stop()
    }

    fun start(click: ClickDescription) {
        synchronized(fPlaying) {
            if (fLastClick != click || !fPlaying.get()) {
                fPlaying.set(false)
                fLastClick = click
                fPlaying = AtomicBoolean(true)
                Thread {
                    generateSound(click, fPlaying)
                }.start()
            }
        }
    }

    private fun generateSound(click: ClickDescription, playing: AtomicBoolean) {
        //if (click.trackFilename != null) {
            generateTrackAndClick(click, "track01.wav", playing)
        //} else {
        //    generateClickOnly(click, playing)
        //}
    }

    private fun generateTrackAndClick(
        click: ClickDescription,
        trackFilename: String,
        playing: AtomicBoolean
    ) {
        val clickBuffer = createClickBuffer(click, TRACK_AND_CLICK_CHANNEL_COUNT)

        val audioTrack = buildAudioTrack(TRACK_AND_CLICK_CHANNEL_COUNT, clickBuffer.size)
        try {
            audioTrack.play()

            val trackUri = resolveFileUri(context, trackFilename)
            context.contentResolver.openInputStream(trackUri)?.use { inputStream ->
                inputStream.skip(40) // skip header
                while (true) {
                    copyTrackToBuffer(inputStream, clickBuffer)
                    audioTrack.write(clickBuffer, 0, clickBuffer.size)
                    synchronized(playing) {
                        if (!playing.get()) {
                            return
                        }
                    }
                }
            }
        } catch (e: IllegalArgumentException) {

        } finally {
            audioTrack.stop()
        }
    }

    private fun copyTrackToBuffer(inputStream: InputStream, buffer: ByteArray) {
        val sampleCount = buffer.size / TRACK_AND_CLICK_CHANNEL_COUNT
        (0 until sampleCount).forEach { sampleIndex ->
            val index = sampleIndex * TRACK_AND_CLICK_CHANNEL_COUNT + 2
            if ((index + 1) in buffer.indices) {
                inputStream.read(buffer, index, 2)
            }
        }
    }

    private fun generateClickOnly(click: ClickDescription, playing: AtomicBoolean) {
        val clickBuffer = createClickBuffer(click, CLICK_ONLY_CHANNEL_COUNT)

        val audioTrack = buildAudioTrack(CLICK_ONLY_CHANNEL_COUNT, clickBuffer.size)
        try {
            audioTrack.play()

            if (click.countOff) {
                val countOffBuffers = WaveUtil.generateCountOff(context, click.bpm, CLICK_ONLY_CHANNEL_COUNT)
                countOffBuffers.forEach { countOffBuffer ->
                    audioTrack.write(countOffBuffer, 0, countOffBuffer.size)
                    synchronized(playing) {
                        if (!playing.get()) {
                            return
                        }
                    }
                }
            }

            while (true) {
                audioTrack.write(clickBuffer, 0, clickBuffer.size)
                synchronized(playing) {
                    if (!playing.get()) {
                        return
                    }
                }
            }
        } finally {
            audioTrack.stop()
        }
    }

    private fun buildAudioTrack(channelCount: Int, bufferSizeInBytes: Int) =
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                    .setSampleRate(WaveUtil.kSAMPLES_PER_SECOND)
                    .setChannelIndexMask((1 shl channelCount) - 1)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeInBytes)
            .build()

    private fun createClickBuffer(click: ClickDescription, channelCount: Int) =
        when (click.type) {
            EClickType.SHAKER -> WaveUtil.generateShakerLoop(context, click.bpm, channelCount)
            else -> WaveUtil.generateClick(
                fBeepFrequency,
                fBeepDuration,
                click.bpm,
                fDivisionFrequency,
                fDivisionVolume,
                click.type.value,
                channelCount
            )
        }

    fun stop() {
        synchronized(fPlaying) {
            fPlaying.set(false)
        }
    }
}
