package com.buxi.music.jiosaavn

import com.buxi.music.jiosaavn.models.JioSaavnSearchResponse
import com.buxi.music.jiosaavn.models.JioSaavnSong
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.json.JSONArray

object JioSaavn {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(ContentEncoding) {
            gzip()
            deflate()
        }
        defaultRequest {
            url("https://www.jiosaavn.com/api.php") {
                parameters.append("_format", "json")
                parameters.append("_marker", "0")
                parameters.append("ctx", "wap6dot0")
            }
        }
    }

    suspend fun searchSongs(query: String, page: Int = 0, limit: Int = 10): Result<JioSaavnSearchResponse> = runCatching {
        client.get {
            parameter("__call", "search.getResults")
            parameter("q", query)
            parameter("p", page + 1)
            parameter("n", limit)
            header("cookie", "L=english; gdpr_acceptance=true; DL=english")
        }.body<JioSaavnSearchResponse>()
    }

    suspend fun getSongDetails(id: String): Result<JioSaavnSong?> = runCatching {
        val response = client.get {
            parameter("__call", "song.getDetails")
            parameter("pids", id)
            header("cookie", "L=english; gdpr_acceptance=true; DL=english")
        }.body<String>()

        val jsonObj = JSONObject(response)
        val songsArray = jsonObj.optJSONArray("songs")
        if (songsArray != null && songsArray.length() > 0) {
            val songJson = songsArray.getJSONObject(0).toString()
            return@runCatching json.decodeFromString<JioSaavnSong>(songJson)
        }
        null
    }

    suspend fun getLyrics(songId: String): Result<String?> = runCatching {
        val response = client.get {
            parameter("__call", "lyrics.getLyrics")
            parameter("lyrics_id", songId)
            parameter("api_version", 4)
            header("cookie", "L=english; gdpr_acceptance=true; DL=english")
        }.body<String>()

        val jsonObj = JSONObject(response)
        jsonObj.optString("lyrics", null)
    }
}
