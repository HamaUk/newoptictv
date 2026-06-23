package com.streamvault.domain.repository

interface PresenceRepository {
    suspend fun startTracking()
    fun updateActivity(channelName: String?)
    fun stopTracking()
}
