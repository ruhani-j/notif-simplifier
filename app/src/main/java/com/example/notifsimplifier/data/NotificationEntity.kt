package com.example.notifsimplifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val intentBytes: ByteArray? = null,
    // "REDIRECT" = captured from Redirect-mode app (status bar notif cancelled)
    // "COLLECT"  = copied from Instant-mode app with history collection enabled
    val source: String = "REDIRECT",
    // epoch millis when to auto-delete; 0 = never
    val expiresAt: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotificationEntity) return false
        return id == other.id && appName == other.appName && title == other.title &&
               text == other.text && timestamp == other.timestamp && source == other.source &&
               expiresAt == other.expiresAt &&
               (intentBytes === other.intentBytes || intentBytes?.contentEquals(other.intentBytes ?: return false) == true)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + appName.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (intentBytes?.contentHashCode() ?: 0)
        result = 31 * result + source.hashCode()
        result = 31 * result + expiresAt.hashCode()
        return result
    }
}
