package com.example.carebeacon

class VolumeKeyHandler(private val sosTrigger: () -> Unit) {

    private val pressTimestamps = mutableListOf<Long>()
    private val timeWindow = 2500L // 2.5 seconds
    private val pressThreshold = 5

    fun handlePress() {
        val now = System.currentTimeMillis()
        pressTimestamps.add(now)

        // Clean up old timestamps outside the time window
        pressTimestamps.removeAll { now - it > timeWindow }

        if (pressTimestamps.size >= pressThreshold) {
            pressTimestamps.clear()
            sosTrigger()
        }
    }
}
