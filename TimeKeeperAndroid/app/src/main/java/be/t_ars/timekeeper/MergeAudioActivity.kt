package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import be.t_ars.timekeeper.databinding.MergeAudioBinding

class MergeAudioActivity : AbstractActivity() {
    private lateinit var fBinding: MergeAudioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = MergeAudioBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        val preferredDevice = getSettingOutputDeviceString(this)
        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val text = devices.joinToString("\n") { device ->
            """${if ("${device.productName}/${device.type}" == preferredDevice) "* " else ""} Device ${getDeviceDescription(device)}
  Sample rates: ${device.sampleRates.joinToString()}
  Channel counts: ${device.channelCounts.joinToString()}
  Encodings: ${device.encodings.joinToString()}"""
        }

        fBinding.mergeText.text = text
    }

    companion object {
        fun startActivity(context: Context) =
            Intent(context, MergeAudioActivity::class.java)
                .let(context::startActivity)
    }
}