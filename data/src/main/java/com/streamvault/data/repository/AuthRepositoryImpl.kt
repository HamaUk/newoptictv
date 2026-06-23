package com.streamvault.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.AppUser
import com.streamvault.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) : AuthRepository {

    override val currentUser: Flow<AppUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(user?.let { AppUser(id = it.uid, email = it.email, isAnonymous = it.isAnonymous) })
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun loginWithCode(code: String): Result<Unit> = runCatching {
        // Look up the code field inside the activeSessions path dynamically
        val snapshot = firebaseDatabase.getReference("sync/global/activeSessions")
            .orderByChild("code")
            .equalTo(code)
            .get()
            .await()
            
        if (!snapshot.exists() || !snapshot.hasChildren()) {
            return Result.Error("Invalid login code")
        }
        
        // Grab the first matching device session node (e.g., DEV_107424)
        val sessionNode = snapshot.children.firstOrNull()
        
        val email = sessionNode?.child("email")?.getValue(String::class.java)
        val password = sessionNode?.child("password")?.getValue(String::class.java)
        
        if (email != null && password != null) {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.Success(Unit)
        } else {
            // Fallback to anonymous authentication if credentials aren't provided
            firebaseAuth.signInAnonymously().await()
            Result.Success(Unit)
        }
    }.getOrElse { Result.Error(it.message ?: "Login failed", it) }

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: "Login failed", it) }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        firebaseAuth.currentUser?.delete()?.await()
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: "Delete account failed", it) }
}
