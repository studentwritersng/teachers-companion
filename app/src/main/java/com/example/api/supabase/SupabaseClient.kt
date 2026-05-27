package com.example.api.supabase

import com.example.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {

    private val client by lazy {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY

        if (url.isBlank() || url.contains("your-project-id")) {
            throw IllegalStateException(
                "Supabase not configured. Set SUPABASE_URL and SUPABASE_ANON_KEY in .env"
            )
        }

        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
        }
    }

    fun get(): io.github.jan.supabase.SupabaseClient = client

    val postgrest: Postgrest get() = client.postgrest
    val auth: Auth get() = client.auth
    val realtime: Realtime get() = client.realtime
}
