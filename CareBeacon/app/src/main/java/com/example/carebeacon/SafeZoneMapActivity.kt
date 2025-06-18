package com.example.carebeacon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class SafeZoneMapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var locationManager: LocationManager

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Safe zone center (example: College or Home coordinates)
    private val safeZoneCenter = GeoPoint(11.3560987, 77.8264442)
    private val safeZoneRadiusMeters = 100.0f // 100m radius

    private var wasInsideZone = true // To prevent toast spamming

    private val locationListener = LocationListener { location ->
        checkIfOutsideGeofence(location)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        setContentView(R.layout.activity_safezone_map)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        map.controller.setZoom(18.0)
        map.controller.setCenter(safeZoneCenter)

        drawSafeZone()
        addSafeZoneMarker()

        checkLocationPermissionAndInit()
    }

    private fun checkLocationPermissionAndInit() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            setupLocationOverlay()
        }
    }

    private fun setupLocationOverlay() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        map.overlays.add(locationOverlay)

        locationOverlay.runOnFirstFix {
            runOnUiThread {
                map.controller.animateTo(locationOverlay.myLocation)
            }
        }

        // Use real Android location updates
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L, // 2 seconds
                5f,    // 5 meters
                locationListener,
                Looper.getMainLooper()
            )
        }
    }

    private fun drawSafeZone() {
        val circle = Polygon().apply {
            points = Polygon.pointsAsCircle(safeZoneCenter, safeZoneRadiusMeters.toDouble())
            fillColor = 0x44FF0000  // semi-transparent red
            strokeColor = 0xAAFF0000.toInt()
            strokeWidth = 4f
        }
        map.overlays.add(circle)
    }

    private fun addSafeZoneMarker() {
        val marker = Marker(map).apply {
            position = safeZoneCenter
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Safe Zone"
        }
        map.overlays.add(marker)
    }

    private fun checkIfOutsideGeofence(currentLocation: Location) {
        val safeLocation = Location("").apply {
            latitude = safeZoneCenter.latitude
            longitude = safeZoneCenter.longitude
        }

        val distance = currentLocation.distanceTo(safeLocation)

        if (distance > safeZoneRadiusMeters) {
            if (wasInsideZone) {
                Toast.makeText(this, "⚠️ You're outside the safe zone!", Toast.LENGTH_LONG).show()
                wasInsideZone = false
                sosTrigger(currentLocation)
            }
        } else {
            if (!wasInsideZone) {
                Toast.makeText(this, "✅ You're back in the safe zone.", Toast.LENGTH_SHORT).show()
                wasInsideZone = true
            }
        }
    }

    private fun sosTrigger(currentLocation: Location) {
        Toast.makeText(this, "🚨 SOS Triggered!", Toast.LENGTH_LONG).show()
//        println("📍 SOS LOCATION: ${currentLocation.latitude}, ${currentLocation.longitude}")
        val sharedPref = getSharedPreferences("EmergencyContacts", MODE_PRIVATE)
        val smsManager = SmsManager.getDefault()
        val sosMessage = "⚠️ This is an SOS Alert. Please help me immediately! ${currentLocation.latitude}, ${currentLocation.longitude}"

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setupLocationOverlay()
        } else {
            Toast.makeText(this, "Location permission is required to show your position.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(locationListener)
        }
    }

}
