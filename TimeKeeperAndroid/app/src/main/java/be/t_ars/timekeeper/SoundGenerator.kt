package be.t_ars.timekeeper

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.SoundPool.OnLoadCompleteListener
import android.util.Log
import be.t_ars.timekeeper.util.WaveUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class SoundGenerator(context: Context, private val fBeepFrequency: Int, private val fBeepDuration: Int, private val fDivisionFrequency: Int, private val fDivisionAmplitude: Int) : OnLoadCompleteListener {
    private val fFile: File = File(context.cacheDir, "beep.wav")
    private val fSoundPool: SoundPool
    private var fSoundID = -1
    private val fPlaying = AtomicBoolean(false)

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

    private fun configure(bpm: Int, divisions: Int) {
        try {
            BufferedOutputStream(FileOutputStream(fFile)).use { out ->
                WaveUtil.generateSine(out, fBeepFrequency, fBeepDuration, bpm, fDivisionFrequency, fDivisionAmplitude, divisions)
            }
            synchronized(fPlaying) {
                fSoundPool.autoPause()
                if (fSoundID != -1) {
                    fSoundPool.unload(fSoundID)
                    fSoundID = -1
                }
            }
            fSoundPool.load(fFile.absolutePath, 1)
        } catch (e: IOException) {
            Log.e("TimeKeeper", "Failed to write WAV file to " + fFile.absolutePath, e)
        }

    }

    fun start(bpm: Int, divisions: Int) {
        synchronized(fPlaying) {
            fPlaying.set(true)
        }
        configure(bpm, divisions)
    }

    fun stop() {
        synchronized(fPlaying) {
            fPlaying.set(false)
        }
        fSoundPool.autoPause()
    }

    override fun onLoadComplete(soundPool: SoundPool, sampleId: Int, status: Int) {
        synchronized(fPlaying) {
            fSoundID = sampleId
            if (fPlaying.get()) {
                fSoundPool.play(fSoundID, 1f, 1f, 1, -1, 1f)
            }
        }
    }
}
