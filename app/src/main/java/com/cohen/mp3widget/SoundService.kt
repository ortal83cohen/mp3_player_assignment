package com.cohen.mp3widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import java.util.*


const val ACTION_PLAY: String = "PLAY"
const val ACTION_STOP: String = "STOP"
const val POSITION: String = "POSITION"
const val ARTISTS: String = "ARTISTS"
const val TITLE: String = "TITLE"
const val ALBUM: String = "ALBUM"

private const val FOREGROUND_SERVICE_ID: Int = 1241

class SoundService : Service(), MediaPlayer.OnPreparedListener {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    var artist = ""
    var title = ""
    var albumName = ""
    var player: MediaPlayer? = null
    var notification: Notification? = null
    var timer = Timer()
    override fun onCreate() {

        player = MediaPlayer.create(this, R.raw.mp3)
        player?.isLooping = false

        val metaRetriever = MediaMetadataRetriever()
        val file = resources.openRawResourceFd(R.raw.mp3)
        metaRetriever.setDataSource(file.fileDescriptor, file.startOffset, file.length)

        artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "unknown for this file"
        title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "unknown for this file"
        albumName = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "unknown for this file"

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        timer.cancel()
        timer = Timer()

        when (intent.action) {
            ACTION_PLAY -> {
                player?.apply {
                    showNotification()
                    start()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            if (player?.isPlaying == true) {
                                val intent = Intent(this@SoundService, AppWidget::class.java)
                                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                val ids =
                                    AppWidgetManager.getInstance(application)
                                        .getAppWidgetIds(ComponentName(application, AppWidget::class.java))
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                                intent.putExtra(POSITION, player?.currentPosition ?: 0)
                                intent.putExtra(ARTISTS, artist)
                                intent.putExtra(TITLE, title)
                                intent.putExtra(ALBUM, albumName)
                                sendBroadcast(intent)
                            } else {
                                timer.cancel()
                            }
                        }
                    }, 0, 1000)
                }

            }
            ACTION_STOP -> {
                player?.apply {
                    hideNotification()
                    try {
                        if (isPlaying) {
                            stop()
                            release()
                        }
                    } catch (e: IllegalStateException) {
                    }
                    val intent = Intent(this@SoundService, AppWidget::class.java)
                    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    val ids =
                        AppWidgetManager.getInstance(application)
                            .getAppWidgetIds(ComponentName(application, AppWidget::class.java))
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    intent.putExtra(POSITION, 0)
                    sendBroadcast(intent)
                }
            }
        }

        return START_STICKY
    }

    private fun hideNotification() {
        stopForeground(true)
    }

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notification == null) {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "channel"
                val notificationChannel =
                    NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT)
                notificationChannel.description = channelId
                notificationChannel.setSound(null, null)

                notificationManager.createNotificationChannel(notificationChannel)
                notification = Notification.Builder(this, channelId)
                    .setContentTitle(resources.getString(R.string.playing_now))
                    .setSmallIcon(
                        R.drawable.notification_icon_background
                    )
                    .build()
            }
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }
    }

    override fun onDestroy() {
        player?.stop()
        player?.release()
        stopSelf()
        super.onDestroy()
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        mediaPlayer.start()
    }
}