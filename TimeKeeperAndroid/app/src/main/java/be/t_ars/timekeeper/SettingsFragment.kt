package be.t_ars.timekeeper

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragment

fun getDeviceDescription(device: AudioDeviceInfo) =
    "${device.productName} (${getDeviceType(device)}; ${device.sampleRates.joinToString()}; ${
        getEncodings(
            device
        )
    })"

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

private fun getEncodings(device: AudioDeviceInfo) =
    device.encodings.joinToString { getEncoding(it) }

private fun getEncoding(encoding: Int) =
    when (encoding) {
        AudioFormat.ENCODING_AAC_ELD -> "AAC ELD"
        AudioFormat.ENCODING_AAC_HE_V1 -> "AAC HE V1"
        AudioFormat.ENCODING_AAC_HE_V2 -> "AAC HE V2"
        AudioFormat.ENCODING_AAC_LC -> "AAC LC"
        AudioFormat.ENCODING_AAC_XHE -> "AAC XHE"
        AudioFormat.ENCODING_AC3 -> "AC3"
        AudioFormat.ENCODING_AC4 -> "AC4"
        AudioFormat.ENCODING_DEFAULT -> "Default"
        AudioFormat.ENCODING_DOLBY_MAT -> "Dolby MAT"
        AudioFormat.ENCODING_DOLBY_TRUEHD -> "Dolby True HD"
        AudioFormat.ENCODING_DRA -> "DRA"
        AudioFormat.ENCODING_DTS -> "DTS"
        AudioFormat.ENCODING_DTS_HD -> "DTS HD"
        AudioFormat.ENCODING_DTS_UHD -> "DTS UHD"
        AudioFormat.ENCODING_E_AC3 -> "E AC3"
        AudioFormat.ENCODING_E_AC3_JOC -> "E AC3 JOC"
        AudioFormat.ENCODING_IEC61937 -> "IEC61937"
        AudioFormat.ENCODING_INVALID -> "Invalid"
        AudioFormat.ENCODING_MP3 -> "MP3"
        AudioFormat.ENCODING_MPEGH_BL_L3 -> "MPEGH BL L3"
        AudioFormat.ENCODING_MPEGH_BL_L4 -> "MPEGH BL L4"
        AudioFormat.ENCODING_MPEGH_LC_L3 -> "MPEGH LC L3"
        AudioFormat.ENCODING_MPEGH_LC_L4 -> "MPEGH LC L4"
        AudioFormat.ENCODING_OPUS -> "Opus"
        AudioFormat.ENCODING_PCM_16BIT -> "PCM 18bit"
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM 24bit packed"
        AudioFormat.ENCODING_PCM_32BIT -> "PCM 32bit"
        AudioFormat.ENCODING_PCM_8BIT -> "PCM 8bit"
        AudioFormat.ENCODING_PCM_FLOAT -> "PCM float"
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
        outputDevice?.entryValues =
            devices.map { device -> "${device.productName}/${device.type}" }.toTypedArray()
    }
}
