package be.t_ars.timekeeper.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.getSettingOutputDevice
import java.util.concurrent.atomic.AtomicBoolean

class SoundGenerator(
    private val context: Context,
    private val fBeepFrequency: Int,
    private val fBeepDuration: Int,
    private val fDivisionFrequency: Int,
    private val fDivisionVolume: Int
) {
    private var fAudioTrack: AudioTrack? = null
    private var fLastClick: ClickDescription? = null

    init {
        val audioAttributesBuilder = AudioAttributes.Builder()
        audioAttributesBuilder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        val soundPoolBuilder = SoundPool.Builder()
        soundPoolBuilder.setAudioAttributes(audioAttributesBuilder.build())
        soundPoolBuilder.setMaxStreams(1)
    }

    fun stop() {
        synchronized(this) {
            fAudioTrack?.pause()
            fAudioTrack?.flush()
            fAudioTrack = null
        }
    }

    fun start(click: ClickDescription) {
        synchronized(this) {
            if (fLastClick != click || fAudioTrack == null) {
                stop()
                fLastClick = click

                generateSound(click)
            }
        }
    }

    private fun generateSound(click: ClickDescription) {
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
                    .setSampleRate(SAMPLES_PER_SECOND)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(clickBuffer.size)
            .build()

        getSettingOutputDevice(context)?.let { audioTrack.setPreferredDevice(it) }
        audioTrack.play()
        fAudioTrack = audioTrack

        Thread {
            if (click.countOff) {
                val countOffBuffer = WaveUtil(context).generateCountOff(click.bpm, 4)
                audioTrack.write(countOffBuffer, 0, countOffBuffer.size)
            }
            while (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.write(clickBuffer, 0, clickBuffer.size)
            }
        }.start()
    }

    private fun createClickBuffer(click: ClickDescription): ByteArray {
        val waveUtil = WaveUtil(context)
        return when (click.type) {
            EClickType.SHAKER -> waveUtil.generateShakerLoop(click.bpm, click.divisionCount)
            EClickType.COWBELL -> waveUtil.generateCowbell(
                click.bpm,
                click.divisionCount,
                click.beatCount,
                fDivisionVolume
            )

            else -> waveUtil.generateClick(
                fBeepFrequency,
                fBeepDuration,
                click.bpm,
                fDivisionFrequency,
                fDivisionVolume,
                click.divisionCount,
                click.beatCount
            )
        }
    }
}
