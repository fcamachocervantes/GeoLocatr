package com.csci448.geolocatr

import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startActivity
import java.text.SimpleDateFormat
import java.util.*

class LocationAlarmReceiver : BroadcastReceiver() {
    var lastLocation: Location? = null

    companion object{
        private const val LOG_TAG = "448.Alarm"
        private const val ALARM_ACTION = "448_ACTION"
        private const val EXTRA_LATITUDE = "latitude"
        private const val EXTRA_LONGITUDE = "logitude"
        private const val CHANNEL_ID = "LocationNotificationID"
        private const val CHANNEL_NAME = "LocationChannel"
        private const val CHANNEL_DESC = "This channel is used to notify about location"
        private fun createIntent(context: Context, location: Location?): Intent {
            val intent = Intent(context, LocationAlarmReceiver::class.java).apply {
                action = ALARM_ACTION
                putExtra(EXTRA_LATITUDE, location?.latitude ?: 0.0)
                putExtra(EXTRA_LONGITUDE, location?.longitude ?: 0.0)
            }
            return intent
        }
    }

    private fun scheduleAlarm(activity: Activity) {
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createIntent(activity, lastLocation)
        val pendingIntent = PendingIntent.getBroadcast(
            activity,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val alarmDelayInSeconds = 10
        val alarmTimeInUTC = System.currentTimeMillis() + alarmDelayInSeconds * 1_000L
        Log.d(LOG_TAG, "Setting alarm for ${SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US).format(Date(alarmTimeInUTC))}")

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(LOG_TAG, "running on Version S or newer, checking if can schedule exact alarms")
            if (alarmManager.canScheduleExactAlarms()) {
                Log.d(LOG_TAG, "can schedule exact alarms")
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    alarmTimeInUTC,
                    pendingIntent)
            } else {
                Log.d(LOG_TAG, "can’t schedule exact alarms, launching intent to bring up settings")
                val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(activity, settingsIntent, null)
            }
        } else {
            Log.d(LOG_TAG, "running on Version R or older, can set alarm directly")
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                alarmTimeInUTC,
                pendingIntent)
        }
    }

    fun checkPermissionAndScheduleAlarm( activity: Activity,  permissionLauncher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(LOG_TAG, "running on version tiramisu or newer, need permission")
            if(activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "have notification permission")
                scheduleAlarm(activity)
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.POST_NOTIFICATIONS)) {
                    Log.d(LOG_TAG, "previously denied notification permission")
                    //display toast with reason
                } else {
                    Log.d(LOG_TAG, "request notification permission")
                    permissionLauncher.launch( android.Manifest.permission.POST_NOTIFICATIONS )
                }
            }
        } else {
            Log.d(LOG_TAG, "running on Version S or older, post away")
            scheduleAlarm(activity)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(LOG_TAG, "received alarm for action ${intent.action}")

        if (intent.action == ALARM_ACTION) {
            val lat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
            val long = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
            Log.d(LOG_TAG, "received our intent with $lat / $long")

            if(ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "have permission to post notifications")
                val notificationManager = NotificationManagerCompat.from(context)

                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = CHANNEL_DESC
                    }

                notificationManager.createNotificationChannel(channel)

                val startingLocation = Location("").apply {
                    latitude = lat
                    longitude = long
                }

                Log.d(LOG_TAG, "startingLocation: lat: ${startingLocation.latitude} long: ${startingLocation.longitude}")

                val deepLinkPendingIntent = MainActivity
                    .createPendingIntent(context, startingLocation)

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_map)
                    .setContentTitle("You are here!")
                    .setContentText("You are at $lat / $long")
                    .setContentIntent(deepLinkPendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(0, notification)
            }
        }
    }
}