package com.example.kairo.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

object SupabaseClient {
    // These should ideally be fetched from BuildConfig or securely managed
    private const val SUPABASE_URL = "https://your-project-url.supabase.co"
    private const val SUPABASE_ANON_KEY = "your-anon-key"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
    
    val auth get() = client.auth
    val postgrest get() = client.postgrest
}
