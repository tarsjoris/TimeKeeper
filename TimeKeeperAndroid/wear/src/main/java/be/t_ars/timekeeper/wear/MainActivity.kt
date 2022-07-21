package be.t_ars.timekeeper.wear

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.wearable.activity.WearableActivity
import be.t_ars.timekeeper.wear.databinding.ActivityMainBinding


class MainActivity : WearableActivity() {
    private lateinit var fBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(fBinding.root)

        fBinding.button.setOnClickListener {
            start()
        }

        // Enables Always-on
        setAmbientEnabled()
    }

    private fun start() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 20, 480), 1)
        vibrator.vibrate(effect)
    }
}
