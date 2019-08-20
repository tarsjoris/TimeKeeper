package be.t_ars.timekeeper

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
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
        val frequency = getIntPreference(this, kFREQUENCY, 880)
        val duration = getIntPreference(this, kDURATION, 20)
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
                            extras.getInt(kINTENT_DATA_BPM),
                            extras.get(kINTENT_DATA_RETURN_ACTIVITY_CLASS)?.let { if (it is Class<*>) it else null },
                            extras.get(kINTENT_DATA_RETURN_ACTIVITY_EXTRAS)?.let { if (it is HashMap<*, *>) it else null })
                    "stop" -> doStop()
                }
                return
            }
        }
    }

    private fun doStart(bpm: Int, returnActivityClass: Class<out Any>?, returnActivityExtras: HashMap<out Any, out Any>?) {
        Log.i("SoundService", "Starting $bpm")
        val text = "$bpm BPM"
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
                ?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT) }
        val stopIntent = PendingIntent.getService(this, 0, createStopIntent(this), PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, fChannelID)
                .setContentTitle(text)
                .also { notificationBuilder ->
                    if (appPendingIntent != null) {
                        notificationBuilder.setContentIntent(appPendingIntent)
                    }
                }
                .setSmallIcon(R.drawable.notification)
                .setTicker(text)
                .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause, "Stop", stopIntent).build())
                .setDeleteIntent(stopIntent)
                .build()
        startForeground(1, notification)
        fSoundGenerator.start(bpm)
    }

    private fun doStop() {
        fSoundGenerator.stop()
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
                fChannelID,
                "Metronome",
                NotificationManager.IMPORTANCE_DEFAULT
        )

        getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
    }

    companion object {
        private const val kINTENT_DATA_ACTION = "action"
        private const val kINTENT_DATA_BPM = "bpm"
        private const val kINTENT_DATA_RETURN_ACTIVITY_CLASS = "returnActivityClass"
        private const val kINTENT_DATA_RETURN_ACTIVITY_EXTRAS = "returnActivityExtras"

        fun startSound(context: Context, tempo: Int, returnActivityClass: Class<out Any>? = null, returnActivityExtras: HashMap<String, Serializable>? = null) =
                Intent(context, SoundService::class.java)
                        .also { intent ->
                            intent.putExtra(kINTENT_DATA_ACTION, "start")
                            intent.putExtra(kINTENT_DATA_BPM, tempo)
                            returnActivityClass?.let { intent.putExtra(kINTENT_DATA_RETURN_ACTIVITY_CLASS, it) }
                            returnActivityExtras?.let { intent.putExtra(kINTENT_DATA_RETURN_ACTIVITY_EXTRAS, it) }
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