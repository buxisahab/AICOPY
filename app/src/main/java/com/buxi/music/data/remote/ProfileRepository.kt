package com.buxi.music.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.buxi.music.data.remote.SupabaseClient.API_KEY
import com.buxi.music.data.remote.SupabaseClient.STORAGE
import com.buxi.music.data.remote.SupabaseClient.URL
import com.buxi.music.data.remote.SupabaseClient.bodyOrNull
import com.buxi.music.data.remote.SupabaseClient.json
import com.buxi.music.data.remote.SupabaseClient.supabaseHeaders
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class UserProfile(
    val id: String = "",
    val email: String? = null,
    val username: String? = null,
    val display_name: String? = null,
    val bio: String? = null,
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val is_dev: Boolean = false,
    val followers_count: Int = 0,
    val following_count: Int = 0,
    // Joined from verification_badges (nullable — only present when verified)
    val badge_type: String? = null
)

@Singleton
class ProfileRepository @Inject constructor(
    private val httpClient: HttpClient,
    @ApplicationContext private val ctx: Context
) {
    private val TAG = "ProfileRepository"

    // ── Upsert profile on login / signup ─────────────────────────────────────
    suspend fun upsertProfile(
        uid: String, email: String?, displayName: String?, avatarUrl: String?
    ): UserProfile? {
        return try {
            val body = buildJsonObject {
                put("id", uid)
                put("email", email ?: "")
                put("display_name", displayName ?: email?.substringBefore("@") ?: "")
                put("username", email?.substringBefore("@")
                    ?.replace(Regex("[^a-z0-9_]"), "") ?: uid.take(10))
                avatarUrl?.let { put("avatar_url", it) }
            }.toString()

            val res: HttpResponse = httpClient.post("$URL/rest/v1/profiles") {
                supabaseHeaders(uid)
                header("Prefer", "resolution=merge-duplicates,return=representation")
                setBody(body)
            }
            val el = res.bodyOrNull() ?: return null
            val arr = el.jsonArray
            json.decodeFromJsonElement<UserProfile>(arr.first())
        } catch (e: Exception) {
            Log.e(TAG, "upsertProfile error", e); null
        }
    }

    // ── Fetch profile by UID ──────────────────────────────────────────────────
    suspend fun fetchProfile(uid: String): UserProfile? {
        return try {
            // Main profile
            val res: HttpResponse = httpClient.get("$URL/rest/v1/profiles") {
                supabaseHeaders()
                parameter("id", "eq.$uid")
                parameter("select", "*,verification_badges(badge_type)")
                parameter("limit", "1")
            }
            val el = res.bodyOrNull() ?: return null
            val arr = el.jsonArray
            if (arr.isEmpty()) return null
            val obj = arr.first().jsonObject
            // Parse base profile
            val base = json.decodeFromJsonElement<UserProfile>(obj)
            // Extract badge_type from nested object
            val badgeEl = obj["verification_badges"]
            val badgeType = when {
                badgeEl is JsonArray && badgeEl.isNotEmpty() ->
                    badgeEl.first().jsonObject["badge_type"]?.jsonPrimitive?.content
                else -> null
            }
            base.copy(badge_type = badgeType)
        } catch (e: Exception) {
            Log.e(TAG, "fetchProfile error", e); null
        }
    }

    // ── Update editable profile fields ────────────────────────────────────────
    suspend fun updateProfile(
        uid: String, displayName: String, bio: String
    ): UserProfile? {
        return try {
            val body = buildJsonObject {
                put("display_name", displayName)
                put("bio", bio)
            }.toString()
            val res: HttpResponse = httpClient.patch("$URL/rest/v1/profiles") {
                supabaseHeaders(uid)
                parameter("id", "eq.$uid")
                setBody(body)
            }
            val el = res.bodyOrNull() ?: return null
            json.decodeFromJsonElement(el.jsonArray.first())
        } catch (e: Exception) {
            Log.e(TAG, "updateProfile error", e); null
        }
    }

    // ── Upload avatar to Supabase Storage ─────────────────────────────────────
    suspend fun uploadAvatar(uid: String, imageUri: Uri): String? {
        return try {
            val bytes = ctx.contentResolver.openInputStream(imageUri)?.readBytes() ?: return null
            val filename = "$uid/avatar.jpg"
            val res: HttpResponse = httpClient.put("${URL}/storage/v1/object/avatars/$filename") {
                header("apikey", API_KEY)
                header("Authorization", "Bearer $API_KEY")
                header("Content-Type", "image/jpeg")
                header("x-upsert", "true")
                setBody(bytes)
            }
            if (res.status.isSuccess()) "$STORAGE/$filename?t=${System.currentTimeMillis()}" else null
        } catch (e: Exception) {
            Log.e(TAG, "uploadAvatar error", e); null
        }
    }

    suspend fun updateAvatarUrl(uid: String, url: String): Boolean {
        return try {
            val res: HttpResponse = httpClient.patch("$URL/rest/v1/profiles") {
                supabaseHeaders(uid)
                parameter("id", "eq.$uid")
                setBody("""{"avatar_url":"$url"}""")
            }
            res.status.isSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "updateAvatarUrl error", e); false
        }
    }

    // ── Follow / Unfollow ─────────────────────────────────────────────────────
    suspend fun follow(followerId: String, followingId: String): Boolean {
        return try {
            val res: HttpResponse = httpClient.post("$URL/rest/v1/follows") {
                supabaseHeaders(followerId)
                header("Prefer", "resolution=ignore-duplicates")
                setBody("""{"follower_id":"$followerId","following_id":"$followingId"}""")
            }
            res.status.isSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "follow error", e); false
        }
    }

    suspend fun unfollow(followerId: String, followingId: String): Boolean {
        return try {
            val res: HttpResponse = httpClient.delete("$URL/rest/v1/follows") {
                supabaseHeaders(followerId)
                parameter("follower_id", "eq.$followerId")
                parameter("following_id", "eq.$followingId")
            }
            res.status.isSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "unfollow error", e); false
        }
    }

    suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        return try {
            val res: HttpResponse = httpClient.get("$URL/rest/v1/follows") {
                supabaseHeaders()
                parameter("follower_id", "eq.$followerId")
                parameter("following_id", "eq.$followingId")
                parameter("select", "id")
                parameter("limit", "1")
            }
            val el = res.bodyOrNull() ?: return false
            el.jsonArray.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // ── Followers / Following lists ────────────────────────────────────────────
    suspend fun fetchFollowers(uid: String): List<UserProfile> = fetchUserList(
        "follower_id", uid, selectAs = "profiles!follows_follower_id_fkey(*)"
    )

    suspend fun fetchFollowing(uid: String): List<UserProfile> = fetchUserList(
        "following_id", uid, selectAs = "profiles!follows_following_id_fkey(*)"
    )

    private suspend fun fetchUserList(
        idField: String, uid: String, selectAs: String
    ): List<UserProfile> {
        return try {
            val res: HttpResponse = httpClient.get("$URL/rest/v1/follows") {
                supabaseHeaders()
                parameter(idField, "eq.$uid")
                parameter("select", selectAs)
            }
            val el = res.bodyOrNull() ?: return emptyList()
            el.jsonArray.mapNotNull { item ->
                runCatching {
                    val inner = item.jsonObject[selectAs.substringBefore("!")]
                        ?: item.jsonObject["profiles"]
                    inner?.let { json.decodeFromJsonElement<UserProfile>(it) }
                }.getOrNull()
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchUserList error", e); emptyList()
        }
    }
}
