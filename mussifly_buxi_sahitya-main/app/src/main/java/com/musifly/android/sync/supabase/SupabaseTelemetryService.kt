package com.musifly.android.sync.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseTelemetryService @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun recordActivity(userId: String, isGuest: Boolean) = withContext(Dispatchers.IO) {
        try {
            val activity = SupabaseUserActivity(
                userId = userId,
                userType = if (isGuest) "guest" else "logged_in"
            )
            
            supabase.from("user_activity").upsert(listOf(activity)) {
                onConflict = "user_id"
            }
            Timber.tag("SupabaseTelemetry").d("✅ User activity recorded: $userId (isGuest: $isGuest)")
        } catch (e: Exception) {
            Timber.tag("SupabaseTelemetry").e(e, "❌ Failed to record user activity")
        }
    }
}
