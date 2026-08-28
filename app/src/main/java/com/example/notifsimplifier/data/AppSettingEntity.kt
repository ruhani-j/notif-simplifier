package com.example.notifsimplifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val isRedirected: Boolean = false,
    // Per-app override: when true, notifications always pass through even if isRedirected is true.
    // Useful as a manual fallback for apps whose OTP format the regex misses.
    val isAlwaysShowNormally: Boolean = false
)
