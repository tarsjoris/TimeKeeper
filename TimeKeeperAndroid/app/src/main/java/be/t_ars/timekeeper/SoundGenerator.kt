package be.t_ars.timekeeper

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.util.WaveUtil
import java.util.concurrent.atomic.AtomicBoolean

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
        val clickBuffer = createClickBuffer(click)

        val audioTrack = AudioTrack.Builder()
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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(clickBuffer.size)
            .build()
        try {
            getSettingOutputDevice(context)?.let { audioTrack.setPreferredDevice(it) }
            audioTrack.play()

            if (click.countOff) {
                val countOffBuffers = WaveUtil.generateCountOff(context, click.bpm)
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

    private fun createClickBuffer(click: ClickDescription) =
        when (click.type) {
            EClickType.SHAKER -> WaveUtil.generateShakerLoop(context, click.bpm)
            else -> WaveUtil.generateClick(
                fBeepFrequency,
                fBeepDuration,
                click.bpm,
                fDivisionFrequency,
                fDivisionVolume,
                click.type.value
            )
        }

    fun stop() {
        synchronized(fPlaying) {
            fPlaying.set(false)
        }
    }
}
