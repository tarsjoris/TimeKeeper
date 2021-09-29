package be.t_ars.timekeeper

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService

@RequiresApi(Build.VERSION_CODES.P)
class BubbleManager(private val context: Context) {
    private val fChannelID = "TimeKeeperBubble"
    private val fShortcutID = "0"
    private val user = Person.Builder().setName(context.getString(R.string.app_name)).build()
    private val notificationManager: NotificationManager = context.getSystemService()
            ?: throw IllegalStateException()
    private val shortcutManager: ShortcutManager = context.getSystemService()
            ?: throw IllegalStateException()

    init {
        val serviceChannel = NotificationChannel(
                fChannelID,
                "Metronome",
                NotificationManager.IMPORTANCE_HIGH
        )

        notificationManager.createNotificationChannel(serviceChannel)

        shortcutManager.addDynamicShortcuts(listOf(
                ShortcutInfo.Builder(context, fShortcutID)
                        .setLocusId(LocusId(fShortcutID))
                        .setActivity(ComponentName(context, OverviewActivity::class.java))
                        .setShortLabel(context.getString(R.string.app_name))
                        .setIcon(Icon.createWithResource(context, R.drawable.ic_score_dark))
                        .setLongLived(true)
                        .setCategories(setOf("be.t_ars.timekeeper.bubbles.category.TEXT_SHARE_TARGET"))
                        .setIntent(
                                Intent(context, OverviewActivity::class.java)
                                        .setAction(Intent.ACTION_VIEW)
                        )
                        .setPerson(user)
                        .build()
        ))
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun showBubble() {
        val contentIntent = Intent(context, PlaylistActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val bubbleIntent = Intent(context, BubbleActivity::class.java)
        val bubblePendingIntent = PendingIntent.getActivity(context, 0, bubbleIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val bubbleData = Notification.BubbleMetadata
                .Builder(
                        bubblePendingIntent,
                        Icon.createWithResource(context, R.drawable.ic_score_dark)
                )
                .setDesiredHeight(180)
                .setSuppressNotification(true)
                .build()

        val notification = Notification.Builder(context, fChannelID)
                .setBubbleMetadata(bubbleData)
                .setContentTitle("TimeKeeper")
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setShortcutId(fShortcutID)
                .setContentIntent(contentPendingIntent)
                .setSmallIcon(R.drawable.ic_score)
                .setContentText("Hello")
                .setStyle(Notification.MessagingStyle(user))
                .build()

        notificationManager.notify(2, notification)
    }
}