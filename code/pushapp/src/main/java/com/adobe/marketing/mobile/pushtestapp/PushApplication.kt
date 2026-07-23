package com.adobe.marketing.mobile.pushtestapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.Lifecycle
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.identity.Identity

class PushApplication : Application() {
    private val ENVIRONMENT_FILE_ID = "staging/1b50a869c4a2/c13fe528d279/launch-bf8b84a00a21"

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        MobileCore.registerExtensions(
            listOf(Messaging.EXTENSION, Identity.EXTENSION, Lifecycle.EXTENSION, Edge.EXTENSION, Assurance.EXTENSION)
        ) {
            MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID)
            MobileCore.updateConfiguration(
                hashMapOf("edge.environment" to "int") as Map<String, Any>)
            MobileCore.lifecycleStart(null)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "push_notifications",
                "Push Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
