package com.example.mealomat.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

// TODO: move to local properties at some point
private const val SUPABASE_URL = "https://tjbgbpvhrprwfmwmfioi.supabase.co"
private const val SUPABASE_KEY = "sb_publishable_X_lwFUR3VuBHf32bn042KQ_hEHjslGW"

fun createSupabase(): SupabaseClient =
    createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
        install(Auth)
    }
