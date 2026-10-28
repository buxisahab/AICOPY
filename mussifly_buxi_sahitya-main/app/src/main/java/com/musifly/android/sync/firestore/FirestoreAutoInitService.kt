package com.musifly.android.sync.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Service for auto-initializing Firestore structure
 * Creates missing documents/collections silently without crashes
 */
object FirestoreAutoInitService {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Ensure user document exists in Firestore
     * Creates it silently if missing
     */
    suspend fun ensureUserDocument(userId: String): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val snapshot = userRef.get().await()
            
            if (!snapshot.exists()) {
                // Create user document with basic structure
                userRef.set(mapOf(
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )).await()
                Timber.tag("FirestoreAutoInit").d("Created user document for: $userId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestoreAutoInit").e(e, "Failed to ensure user document")
            Result.failure(e)
        }
    }
    
    /**
     * Ensure likes subcollection exists
     * Firestore creates subcollections automatically when documents are added
     * This method just ensures the parent user document exists
     */
    suspend fun ensureLikesCollection(userId: String): Result<Unit> {
        return ensureUserDocument(userId)
    }
    
    /**
     * Ensure playlists subcollection exists
     * Firestore creates subcollections automatically when documents are added
     * This method just ensures the parent user document exists
     */
    suspend fun ensurePlaylistsCollection(userId: String): Result<Unit> {
        return ensureUserDocument(userId)
    }
    
    /**
     * Initialize all required Firestore structures for current user
     * Called automatically when user logs in
     */
    suspend fun initializeForCurrentUser(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(
            IllegalStateException("No user logged in")
        )
        
        return try {
            ensureUserDocument(userId)
            ensureLikesCollection(userId)
            ensurePlaylistsCollection(userId)
            Timber.tag("FirestoreAutoInit").d("Initialized Firestore structure for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestoreAutoInit").e(e, "Failed to initialize Firestore structure")
            Result.failure(e)
        }
    }
    /**
     * Update user's lastActiveAt timestamp
     * Called when app enters foreground
     */
    suspend fun updateUserActivity(userId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "lastActiveAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                // Also update local timestamp for client-side reference if needed
                "lastActiveAtLocal" to System.currentTimeMillis()
            )
            
            firestore.collection("users").document(userId)
                .update(updates)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            // Start silently - if document doesn't exist, try to create it first
            try {
                ensureUserDocument(userId)
                val updates = mapOf(
                    "lastActiveAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "lastActiveAtLocal" to System.currentTimeMillis()
                )
                firestore.collection("users").document(userId)
                    .update(updates)
                    .await()
                Result.success(Unit)
            } catch (retryException: Exception) {
                 // Fail silently as requested - do not crash or disturb user
                Result.failure(retryException)
            }
        }
    }
}

