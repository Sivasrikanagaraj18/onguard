package com.example.carebeacon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.carebeacon.MainActivity
import com.example.carebeacon.R


class ContactSetupActivity : AppCompatActivity() {

    private lateinit var contactFields: List<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_setup)

        contactFields = listOf(
            findViewById(R.id.contact1),
            // Add these after adding more EditText in XML
             findViewById(R.id.contact2),
             findViewById(R.id.contact3),
             findViewById(R.id.contact4),
             findViewById(R.id.contact5)
        )

        val saveBtn = findViewById<Button>(R.id.saveButton)
        saveBtn.setOnClickListener {
            saveContacts()
        }
    }

    private fun saveContacts() {
        val sharedPref = getSharedPreferences("EmergencyContacts", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        for ((index, field) in contactFields.withIndex()) {
            val number = field.text.toString().trim()
            if (number.isEmpty() || number.length < 10) {
                Toast.makeText(this, "Enter valid number in Contact ${index + 1}", Toast.LENGTH_SHORT).show()
                return
            }
            editor.putString("contact${index + 1}", number)
        }

        editor.apply()
        Toast.makeText(this, "Contacts saved!", Toast.LENGTH_SHORT).show()

        // Optional: Navigate back to main activity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
