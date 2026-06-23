package com.streamvault.domain.repository

import com.streamvault.domain.model.Result
import kotlinx.coroutines.flow.Flow

data class AppUser(
    val id: String,
    val email: String? = null,
    val isAnonymous: Boolean = false
)

interface AuthRepository {
    val currentUser: Flow<AppUser?>
    
    suspend fun loginWithCode(code: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun deleteAccount(): Result<Unit>
}
