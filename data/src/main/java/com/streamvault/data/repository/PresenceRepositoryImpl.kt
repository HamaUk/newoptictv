package com.streamvault.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.domain.repository.PresenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class PresenceRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val preferencesRepository: PreferencesRepository
) : PresenceRepository {

    private val rtdbPath = "sync/global/activeSessions"
    private var timerJob: Job? = null
    private var deviceId: String? = null
    private var currentChannel: String? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private suspend fun getDeviceId(): String {
        if (deviceId != null) return deviceId!!
        var id = preferencesRepository.monitorDeviceId.first()
        if (id == null) {
            id = "DEV_${Random.nextInt(100000, 999999)}"
            preferencesRepository.setMonitorDeviceId(id)
        }
        deviceId = id
        return id
    }

    override suspend fun startTracking() {
        val id = getDeviceId()
        setupHeartbeat(id)
    }

    override fun updateActivity(channelName: String?) {
        currentChannel = channelName
        ping() // Immediate ping on activity change
    }

    private fun setupHeartbeat(id: String) {
        timerJob?.cancel()
        
        // Clean up when app disconnects
        firebaseDatabase.getReference("$rtdbPath/$id").onDisconnect().removeValue()
        
        // Heartbeat every 60 seconds
        timerJob = scope.launch {
            while (true) {
                ping()
                delay(60_000)
            }
        }
    }

    private fun ping() {
        val currentId = deviceId ?: return
        val currentCode = firebaseAuth.currentUser?.uid ?: "UNKNOWN"
        
        val payload = mapOf(
            "code" to currentCode,
            "channel" to (currentChannel ?: "Dashboard"),
            "lastSeen" to ServerValue.TIMESTAMP,
            "platform" to "App"
        )
        
        scope.launch {
            try {
                firebaseDatabase.getReference("$rtdbPath/$currentId").setValue(payload).await()
            } catch (e: Exception) {
                // Silent fail for pings
            }
        }
    }

    override fun stopTracking() {
        timerJob?.cancel()
        timerJob = null
        val currentId = deviceId
        if (currentId != null) {
            firebaseDatabase.getReference("$rtdbPath/$currentId").removeValue()
        }
    }
}
