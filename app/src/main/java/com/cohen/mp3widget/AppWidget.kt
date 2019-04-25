package com.cohen.mp3widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import java.util.concurrent.TimeUnit


class AppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        intent?.apply {
            position = TimeUnit.MILLISECONDS.toSeconds(getIntExtra(POSITION, 0).toLong())
            title = getStringExtra(TITLE) ?: ""
            artists = getStringExtra(ARTISTS) ?: ""
            album = getStringExtra(ALBUM) ?: ""

        }
    }

    companion object {
        var position = 0L
        var title = ""
        var artists = ""
        var album = ""
        internal fun updateAppWidget(
            context: Context, appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {

            val views = RemoteViews(context.packageName, R.layout.app_widget)

            views.setTextViewText(R.id.time, position.toString())
            views.setTextViewText(R.id.title_name, title)
            views.setTextViewText(R.id.artists_name, artists)
            views.setTextViewText(R.id.album_name, album)
            val playIntent = Intent(context, SoundService::class.java)
            playIntent.action = ACTION_PLAY
            val playPendingIntent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    PendingIntent.getForegroundService(
                        context,
                        0,
                        playIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                } else {
                    PendingIntent.getService(
                        context,
                        0,
                        playIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
            views.setOnClickPendingIntent(R.id.play, playPendingIntent)


            val stopIntent = Intent(context, SoundService::class.java)
            stopIntent.action = ACTION_STOP
            val stopPendingIntent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    PendingIntent.getForegroundService(
                        context,
                        0,
                        stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                } else {
                    PendingIntent.getService(
                        context,
                        0,
                        stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
            views.setOnClickPendingIntent(R.id.stop, stopPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

