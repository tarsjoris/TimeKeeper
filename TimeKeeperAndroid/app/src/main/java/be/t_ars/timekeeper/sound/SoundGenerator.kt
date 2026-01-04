package be.t_ars.timekeeper.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.data.ClickDetails
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.data.Section
import be.t_ars.timekeeper.getSettingOutputDevice
import java.util.concurrent.atomic.AtomicBoolean

class SoundGenerator(
    private val context: Context,
    private val fBeepFrequency: Int,
    private val fBeepDuration: Int,
    private val fMainVolume: Int,
    private val fDivisionFrequency: Int,
    private val fDivisionVolume: Int,
) {
    private var fAudioTrack: AudioTrack? = null
    private var fLastClick: ClickDetails? = null

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

    fun start(click: ClickDetails) {
        synchronized(this) {
            if (fLastClick != click || fAudioTrack == null) {
                stop()
                fLastClick = click

                generateSound(click)
            }
        }
    }

    private fun generateSound(click: ClickDetails) {
        val waveUtil = WaveUtil(context)
        val clickBuffer = createClickBuffer(waveUtil, click)

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

        Thread clickLoop@{
            val sections = click.clickDescription.sections
            if (sections.isNotEmpty()) {
                generateCountOff(audioTrack, waveUtil, clickBuffer, click)
                for (i in sections.indices) {
                    repeat(sections[i].barCount - 1) {
                        if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            return@clickLoop
                        }
                        audioTrack.write(clickBuffer, 0, clickBuffer.size)
                    }
                    if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        return@clickLoop
                    }
                    if ((i + 1) in sections.indices) {
                        val nextCue = sections[i + 1].cue
                        if (nextCue != null) {
                            audioTrack.write(
                                waveUtil.mixCue(clickBuffer, nextCue.id, fMainVolume, click.stereo),
                                0,
                                clickBuffer.size
                            )
                        } else {
                            audioTrack.write(clickBuffer, 0, clickBuffer.size)
                        }
                    } else {
                        audioTrack.write(clickBuffer, 0, clickBuffer.size)
                    }
                }
            } else {
                if (click.clickDescription.countOff) {
                    generateCountOff(audioTrack, waveUtil, clickBuffer, click)
                }
                while (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.write(clickBuffer, 0, clickBuffer.size)
                }
            }
        }.start()
    }

    private fun generateCountOff(audioTrack: AudioTrack, waveUtil: WaveUtil, clickBuffer: ByteArray, click: ClickDetails) {
        if (click.clickDescription.beatCount == 4) {
            audioTrack.write(
                waveUtil.mixCountOff(clickBuffer, click.clickDescription.bpm / 2, 2, fMainVolume, click.stereo),
                0,
                clickBuffer.size
            )
        }
        audioTrack.write(
            waveUtil.mixCountOff(clickBuffer, click.clickDescription.bpm, click.clickDescription.beatCount, fMainVolume, click.stereo),
            0,
            clickBuffer.size
        )
    }

    private fun createClickBuffer(waveUtil: WaveUtil, click: ClickDetails): ByteArray {
        return when (click.clickDescription.type) {
            EClickType.SHAKER -> waveUtil.generateShakerLoop(
                click.clickDescription.bpm,
                click.clickDescription.divisionCount,
                click.clickDescription.beatCount,
                fMainVolume,
                click.stereo
            )
            EClickType.COWBELL -> waveUtil.generateCowbell(
                click.clickDescription.bpm,
                click.clickDescription.divisionCount,
                click.clickDescription.beatCount,
                fMainVolume,
                fDivisionVolume,
                click.stereo
            )

            else -> waveUtil.generateClick(
                fBeepFrequency,
                fBeepDuration,
                click.clickDescription.bpm,
                click.clickDescription.divisionCount,
                click.clickDescription.beatCount,
                fMainVolume,
                fDivisionFrequency,
                fDivisionVolume,
                click.stereo
            )
        }
    }
}
