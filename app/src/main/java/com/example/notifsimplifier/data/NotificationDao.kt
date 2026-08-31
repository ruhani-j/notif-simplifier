package com.example.notifsimplifier.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(notification: NotificationEntity): Long

    // Redirect captures — shown in the main notification list.
    @Query("SELECT * FROM notifications WHERE source = 'REDIRECT' ORDER BY timestamp DESC")
    fun getAll(): Flow<List<NotificationEntity>>

    // Collect captures — shown in the history screen.
    @Query("SELECT * FROM notifications WHERE source = 'COLLECT' ORDER BY timestamp DESC")
    fun getCollected(): Flow<List<NotificationEntity>>

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications WHERE source = 'REDIRECT'")
    suspend fun clearRedirect()

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE source = 'COLLECT'")
    suspend fun clearCollected()

    // Removes entries whose TTL has elapsed. expiresAt = 0 means never delete.
    @Query("DELETE FROM notifications WHERE expiresAt > 0 AND expiresAt <= :now")
    suspend fun deleteExpired(now: Long)
}
