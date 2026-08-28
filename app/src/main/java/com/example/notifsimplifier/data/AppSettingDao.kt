package com.example.notifsimplifier.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {

    // Silently skips if the package is already known — preserves the user's existing toggle state.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(app: AppSettingEntity)

    @Update
    suspend fun update(app: AppSettingEntity)

    @Query("SELECT * FROM app_settings ORDER BY displayName ASC")
    fun getAll(): Flow<List<AppSettingEntity>>

    @Query("SELECT * FROM app_settings WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): AppSettingEntity?

    @Query("SELECT packageName FROM app_settings")
    suspend fun getAllKnownPackageNames(): List<String>
}
