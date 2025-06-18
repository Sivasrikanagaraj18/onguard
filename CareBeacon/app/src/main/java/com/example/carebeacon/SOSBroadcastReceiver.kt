package com.example.carebeacon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.widget.Toast

class SOSBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.example.carebeacon.ACTION_TRIGGER_SOS" && context != null) {
            val sharedPref = context.getSharedPreferences("EmergencyContacts", Context.MODE_PRIVATE)
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
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$contactToCall")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(callIntent)
            } else {
                Toast.makeText(context, "No emergency contacts found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


