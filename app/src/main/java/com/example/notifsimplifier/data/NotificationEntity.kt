package com.example.notifsimplifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appName: String,      // package name of the app that posted it
    val title: String,
    val text: String,
    val timestamp: Long,      // System.currentTimeMillis() when captured
    val intentBytes: ByteArray? = null  // marshalled PendingIntent for deep-link on tap
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotificationEntity) return false
        return id == other.id && appName == other.appName && title == other.title &&
               text == other.text && timestamp == other.timestamp &&
               (intentBytes === other.intentBytes || intentBytes?.contentEquals(other.intentBytes ?: return false) == true)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + appName.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (intentBytes?.contentHashCode() ?: 0)
        return result
    }
}
