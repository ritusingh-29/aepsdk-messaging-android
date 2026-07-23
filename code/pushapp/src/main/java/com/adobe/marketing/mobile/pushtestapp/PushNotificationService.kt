package com.adobe.marketing.mobile.pushtestapp

import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.messaging.MessagingService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        MobileCore.setPushIdentifier(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        MessagingService.handleRemoteMessage(this, message)
    }
}
