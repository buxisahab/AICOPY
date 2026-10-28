package com.musifly.android.db

import androidx.room.*
import com.musifly.android.db.entities.LocalProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalProfileDao {
    @Upsert
    suspend fun upsertProfile(profile: LocalProfileEntity)

    @Query("SELECT * FROM local_profile WHERE id = :userId")
    suspend fun getProfile(userId: String): LocalProfileEntity?

    @Query("SELECT * FROM local_profile WHERE id = :userId")
    fun getProfileFlow(userId: String): Flow<LocalProfileEntity?>

    @Query("DELETE FROM local_profile WHERE id = :userId")
    suspend fun deleteProfile(userId: String)
}
