package be.t_ars.timekeeper.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import be.t_ars.timekeeper.resolveFileUri

class TrackPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    fun playTrack(context: Context, trackFilename: String) {
        stop()
        try {
            parcelFileDescriptor =
                context.contentResolver.openFileDescriptor(Uri.parse(trackFilename), "r")
            parcelFileDescriptor?.let { localParcelFileDescriptor ->
                val player = MediaPlayer()
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                player.setDataSource(localParcelFileDescriptor.fileDescriptor)
                player.prepare()
                player.start()
                mediaPlayer = player
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not play track: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        parcelFileDescriptor?.close()

        mediaPlayer = null
        parcelFileDescriptor = null
    }
}