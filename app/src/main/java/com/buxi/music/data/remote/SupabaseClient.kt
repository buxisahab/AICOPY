package com.buxi.music.data.remote

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

object SupabaseClient {
    const val URL     = "https://vxhdsrvktonrfasmccco.supabase.co"
    const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ4aGRzcnZrdG9ucmZhc21jY2NvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE2OTY1MzUsImV4cCI6MjA4NzI3MjUzNX0.u_JnSybGDaZ4hvO311hUu0MvZmCTlCxBZCbeS6CKz-Q"
    const val STORAGE = "$URL/storage/v1/object/public/avatars"

    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun HttpRequestBuilder.supabaseHeaders(firebaseUid: String? = null) {
        header("apikey", API_KEY)
        header("Authorization", "Bearer $API_KEY")
        header("Content-Type", "application/json")
        header("Prefer", "return=representation")
        firebaseUid?.let { header("x-firebase-uid", it) }
    }

    suspend fun HttpResponse.bodyOrNull(): JsonElement? = runCatching {
        if (status.isSuccess()) json.parseToJsonElement(bodyAsText())
        else { Log.e("Supabase", "HTTP ${status.value}: ${bodyAsText()}"); null }
    }.getOrNull()
}
