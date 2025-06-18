package com.example.carebeacon

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import android.widget.Toast

class MyAccessibilityService : AccessibilityService() {

    private lateinit var volumeKeyHandler: VolumeKeyHandler

    override fun onServiceConnected() {
        Log.d("CareBeacon", "Accessibility Service Connected")

        volumeKeyHandler = VolumeKeyHandler {
            triggerSOS()
        }

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }

        this.serviceInfo = info
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            volumeKeyHandler.handlePress()
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        Log.d("CareBeacon", "Accessibility Service Interrupted")
    }

    private fun triggerSOS() {
        val sharedPref = getSharedPreferences("EmergencyContacts", MODE_PRIVATE)
        val smsManager = SmsManager.getDefault()
        val sosMessage = "⚠️ This is an SOS Alert. Please help me immediately!"

        var contactToCall: String? = null

        for (i in 1..5) {
            val number = sharedPref.getString("contact$i", null)
            if (!number.isNullOrEmpty()) {
                smsManager.sendTextMessage(number, null, sosMessage, null, null)
                if (contactToCall == null) contactToCall = number
            }
        }

        if (contactToCall != null) {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:$contactToCall")
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(callIntent)
        } else {
            Toast.makeText(this, "No emergency contacts found", Toast.LENGTH_SHORT).show()
        }
    }
}
