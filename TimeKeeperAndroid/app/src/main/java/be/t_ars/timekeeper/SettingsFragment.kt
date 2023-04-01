package be.t_ars.timekeeper

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragment

private fun getDeviceDescription(device: AudioDeviceInfo) =
    "${device.productName} (${getDeviceType(device)})"

private fun getDeviceType(device: AudioDeviceInfo) =
    when (device.type) {
        AudioDeviceInfo.TYPE_AUX_LINE -> "Aux line"
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> "BLE broadcast"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE headset"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE speaker"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Built-in earpiece"
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in mice"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in speaker"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "Built-in speaker safe"
        AudioDeviceInfo.TYPE_BUS -> "Bus"
        AudioDeviceInfo.TYPE_DOCK -> "Dock"
        AudioDeviceInfo.TYPE_FM -> "FM"
        AudioDeviceInfo.TYPE_FM_TUNER -> "FM Tuner"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
        AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI EARC"
        AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing aid"
        AudioDeviceInfo.TYPE_IP -> "IP"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "Line analog"
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Line digital"
        AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "Remote submix"
        AudioDeviceInfo.TYPE_TELEPHONY -> "Telephony"
        AudioDeviceInfo.TYPE_TV_TUNER -> "TV tuner"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB accessory"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB device"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
        else -> "Unknown"
    }

class SettingsFragment : PreferenceFragment() {
    @Deprecated("Deprecated in Java")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        val outputDevice = findPreference("outputdevice") as ListPreference?
        val audioManager: AudioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        outputDevice?.entries = devices.map(::getDeviceDescription).toTypedArray()
        outputDevice?.entryValues = devices.map { device -> device.id.toString() }.toTypedArray()
    }
}
