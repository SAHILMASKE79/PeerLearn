package com.sahil.peerlearn

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class PeerLearnApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firebase is automatically initialized by the google-services plugin.
        // Manual initialization here is redundant and can cause issues.
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )

        // Reduce log level to avoid internal GMS/Awareness API noise
        OneSignal.Debug.logLevel = LogLevel.WARN

        // OneSignal Initialization
        // TODO: Replace with your actual OneSignal App ID
        val oneSignalAppId = "74ed12e3-94a3-48bc-b54e-41651cc735cc"
        OneSignal.initWithContext(this, oneSignalAppId)

        // Disable location sharing to avoid Awareness API errors if not used
        OneSignal.Location.isShared = false

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "peerlearn_requests",
                "PeerLearn Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for connection requests and messages"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
