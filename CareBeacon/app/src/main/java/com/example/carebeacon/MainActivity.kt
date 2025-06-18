package com.example.carebeacon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.view.KeyEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.carebeacon.R
import com.example.carebeacon.ContactSetupActivity
import com.example.carebeacon.SafeZoneMapActivity

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var sosButton: Button
    private lateinit var addContactsButton: Button
    private val volumePressTimestamps = mutableListOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sosButton = findViewById(R.id.sosButton)
        addContactsButton = findViewById(R.id.addContactsButton)

        sosButton.setOnClickListener {
            if (hasPermissions()) {
                triggerSOS()
            } else {
                requestPermissions()
            }
        }

        addContactsButton.setOnClickListener {
            startActivity(Intent(this, ContactSetupActivity::class.java))
        }

        val viewMapButton: Button = findViewById(R.id.viewMapButton)
        viewMapButton.setOnClickListener {
            startActivity(Intent(this, SafeZoneMapActivity::class.java))
        }

    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            triggerSOS()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
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
            startActivity(callIntent)
        } else {
            Toast.makeText(this, "No emergency contacts found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val currentTime = System.currentTimeMillis()
            volumePressTimestamps.add(currentTime)

            // Keep only the last 5 timestamps
            if (volumePressTimestamps.size > 5) {
                volumePressTimestamps.removeAt(0)
            }

            if (volumePressTimestamps.size == 5 &&
                volumePressTimestamps.last() - volumePressTimestamps.first() <= 2500
            ) {
                triggerSOS()
                volumePressTimestamps.clear()
            }
        }
        return super.onKeyDown(keyCode, event)
    }


}
