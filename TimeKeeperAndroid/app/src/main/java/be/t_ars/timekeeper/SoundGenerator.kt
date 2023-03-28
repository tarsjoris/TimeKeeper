package be.t_ars.timekeeper

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.SoundPool.OnLoadCompleteListener
import android.util.Log
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.util.WaveUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

class SoundGenerator(
    private val context: Context,
    private val fBeepFrequency: Int,
    private val fBeepDuration: Int,
    private val fDivisionFrequency: Int,
    private val fDivisionAmplitude: Int
) : OnLoadCompleteListener {
    private val fFile: File = File(context.cacheDir, "beep.wav")
    private val fSoundPool: SoundPool
    private var fSoundID = -1
    private val fPlaying = AtomicBoolean(false)
    private var fLastBPM = 0
    private var fLastClickType = EClickType.DIVISIONS_1

    init {
        val audioAttributesBuilder = AudioAttributes.Builder()
        audioAttributesBuilder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        val soundPoolBuilder = SoundPool.Builder()
        soundPoolBuilder.setAudioAttributes(audioAttributesBuilder.build())
        soundPoolBuilder.setMaxStreams(1)
        fSoundPool = soundPoolBuilder.build()
        fSoundPool.setOnLoadCompleteListener(this)
    }

    fun close() {
        stop()
        fSoundPool.release()
    }

    fun prepare(bpm: Int, clickType: EClickType) {
        synchronized(fPlaying) {
            stop()
            if (!clickEquals(bpm, clickType)) {
                configure()
            }
        }
    }

    fun start(bpm: Int, clickType: EClickType) {
        synchronized(fPlaying) {
            fPlaying.set(true)
            if (clickEquals(bpm, clickType)) {
                if (fSoundID != -1) {
                    fSoundPool.play(fSoundID, 1f, 1f, 1, -1, 1f)
                }
            } else {
                fLastBPM = bpm
                fLastClickType = clickType
                configure()
            }
        }
    }

    private fun configure() {
        try {
            BufferedOutputStream(FileOutputStream(fFile)).use(this::generateWaveData)
            fSoundPool.autoPause()
            if (fSoundID != -1) {
                fSoundPool.unload(fSoundID)
                fSoundID = -1
            }
            fSoundPool.load(fFile.absolutePath, 1)
        } catch (e: IOException) {
            Log.e("TimeKeeper", "Failed to write WAV file to " + fFile.absolutePath, e)
        }
    }

    private fun generateWaveData(out: OutputStream) {
        when (fLastClickType) {
            EClickType.SHAKER -> WaveUtil.generateShakerLoop(context, out, fLastBPM)
            else -> {
                val divisions = fLastClickType.value
                WaveUtil.generateSine(
                    out,
                    fBeepFrequency,
                    fBeepDuration,
                    fLastBPM,
                    fDivisionFrequency,
                    fDivisionAmplitude,
                    divisions
                )
            }
        }
    }

    fun stop() {
        synchronized(fPlaying) {
            fPlaying.set(false)
            fSoundPool.autoPause()
        }
    }

    private fun clickEquals(bpm: Int, clickType: EClickType) =
        fLastBPM == bpm && fLastClickType == clickType

    override fun onLoadComplete(soundPool: SoundPool, sampleId: Int, status: Int) {
        synchronized(fPlaying) {
            fSoundID = sampleId
            if (fPlaying.get()) {
                fSoundPool.play(fSoundID, 1f, 1f, 1, -1, 1f)
            }
        }
    }
}
