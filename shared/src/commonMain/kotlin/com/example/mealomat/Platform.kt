package com.example.mealomat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform