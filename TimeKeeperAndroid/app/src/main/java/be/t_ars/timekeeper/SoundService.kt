package be.t_ars.timekeeper

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.Serializable


class SoundService : Service() {
    private val fChannelID = "TimeKeeperChannel"

    private lateinit var fSoundGenerator: SoundGenerator

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        loadIntent(intent)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        loadIntent(intent)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val frequency = getSettingFrequency(this)
        val duration = getSettingDuration(this)
        fSoundGenerator = SoundGenerator(this, frequency, duration)

        createNotificationChannel()
    }

    override fun onDestroy() {
        fSoundGenerator.close()
        super.onDestroy()
    }

    private fun loadIntent(intent: Intent?) {
        if (intent != null) {
            val extras = intent.extras
            if (extras != null) {
                when (extras.getString(kINTENT_DATA_ACTION)) {
                    "start" -> doStart(
                        extras.getString(kINTENT_DATA_LABEL),
                        extras.getInt(kINTENT_DATA_BPM),
                        extras.get(kINTENT_DATA_RETURN_ACTIVITY_CLASS)
                            ?.let { if (it is Class<*>) it else null },
                        extras.get(kINTENT_DATA_RETURN_ACTIVITY_EXTRAS)
                            ?.let { if (it is HashMap<*, *>) it else null })
                    "stop" -> doStop()
                }
                return
            }
        }
    }

    private fun doStart(
        label: String?,
        bpm: Int,
        returnActivityClass: Class<out Any>?,
        returnActivityExtras: HashMap<out Any, out Any>?
    ) {
        Log.i("SoundService", "Starting $bpm")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showNotification(label, bpm, returnActivityClass, returnActivityExtras)
        }
        fSoundGenerator.start(bpm)
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        label: String?,
        bpm: Int,
        returnActivityClass: Class<out Any>?,
        returnActivityExtras: HashMap<out Any, out Any>?
    ) {
        val text = "$bpm BPM"
        val title = label ?: text
        val details = if (label == null) null else text
        val appPendingIntent = returnActivityClass?.let { Intent(this, it) }
            ?.also { intent ->
                if (returnActivityExtras != null) {
                    for ((key, value) in returnActivityExtras) {
                        if (key is String && value is Serializable) {
                            intent.putExtra(key, value)
                        }
                    }
                }
            }
            ?.let {
                PendingIntent.getActivity(
                    this,
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        val stopIntent = PendingIntent.getService(
            this,
            0,
            createStopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, fChannelID)
            .setContentTitle(title)
            .also { notificationBuilder ->
                if (details != null) {
                    notificationBuilder.setContentText(details)
                }
            }
            .also { notificationBuilder ->
                if (appPendingIntent != null) {
                    notificationBuilder.setContentIntent(appPendingIntent)
                }
            }
            .setSmallIcon(R.drawable.notification)
            .setTicker(title)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_pause,
                    "Stop",
                    stopIntent
                ).build()
            )
            .setDeleteIntent(stopIntent)
            .build()
        with(NotificationManagerCompat.from(this)) {
            notify(1, notification)
        }
    }

    private fun doStop() {
        fSoundGenerator.stop()
        with(NotificationManagerCompat.from(this)) {
            cancelAll()
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            fChannelID,
            "Sound",
            NotificationManager.IMPORTANCE_LOW
        )
        serviceChannel.setSound(null, null)
        serviceChannel.setShowBadge(false)

        getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
    }

    companion object {
        private const val kINTENT_DATA_ACTION = "action"
        private const val kINTENT_DATA_LABEL = "label"
        private const val kINTENT_DATA_BPM = "bpm"
        private const val kINTENT_DATA_RETURN_ACTIVITY_CLASS = "returnActivityClass"
        private const val kINTENT_DATA_RETURN_ACTIVITY_EXTRAS = "returnActivityExtras"

        fun startSound(
            context: Context,
            label: String?,
            tempo: Int,
            returnActivityClass: Class<out Any>? = null,
            returnActivityExtras: HashMap<String, Serializable>? = null
        ) =
            Intent(context, SoundService::class.java)
                .also { intent ->
                    intent.putExtra(kINTENT_DATA_ACTION, "start")
                    intent.putExtra(kINTENT_DATA_LABEL, label)
                    intent.putExtra(kINTENT_DATA_BPM, tempo)
                    returnActivityClass?.let {
                        intent.putExtra(
                            kINTENT_DATA_RETURN_ACTIVITY_CLASS,
                            it
                        )
                    }
                    returnActivityExtras?.let {
                        intent.putExtra(
                            kINTENT_DATA_RETURN_ACTIVITY_EXTRAS,
                            it
                        )
                    }
                }
                .let(context::startForegroundService)

        private fun createStopIntent(context: Context) =
            Intent(context, SoundService::class.java)
                .also { intent ->
                    intent.putExtra(kINTENT_DATA_ACTION, "stop")
                }

        fun stopSound(context: Context) =
            createStopIntent(context)
                .let(context::startService)
    }
}