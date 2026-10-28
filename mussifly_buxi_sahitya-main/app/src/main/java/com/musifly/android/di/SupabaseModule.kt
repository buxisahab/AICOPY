package com.musifly.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import javax.inject.Singleton
import kotlinx.serialization.json.Json

import io.github.jan.supabase.storage.Storage

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = com.musifly.android.secure.SecureString.s(com.musifly.android.secure.SecureString.SUPABASE_URL),
            supabaseKey = com.musifly.android.secure.SecureString.s(com.musifly.android.secure.SecureString.SUPABASE_KEY)
        ) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
            })
            install(Postgrest)
            install(Auth)
            install(Realtime)
            install(Storage)
        }
    }
}
