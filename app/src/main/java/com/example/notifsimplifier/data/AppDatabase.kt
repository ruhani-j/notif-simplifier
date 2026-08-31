package com.example.notifsimplifier.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NotificationEntity::class, AppSettingEntity::class], version = 6)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS app_settings (
                        packageName TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        isRedirected INTEGER NOT NULL DEFAULT 0,
                        isAlwaysShowNormally INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE app_settings ADD COLUMN mode TEXT NOT NULL DEFAULT 'UNSET'")
                database.execSQL("UPDATE app_settings SET mode = 'REDIRECT' WHERE isRedirected = 1")
                database.execSQL("UPDATE app_settings SET mode = 'INSTANT' WHERE isRedirected = 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notifications ADD COLUMN intentBytes BLOB")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // source: "REDIRECT" for existing rows (they were all redirect captures)
                database.execSQL("ALTER TABLE notifications ADD COLUMN source TEXT NOT NULL DEFAULT 'REDIRECT'")
                // expiresAt: 0 = never (existing rows kept forever, no change in behaviour)
                database.execSQL("ALTER TABLE notifications ADD COLUMN expiresAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE app_settings ADD COLUMN collectHistory INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // -2 = use global TTL setting
                database.execSQL("ALTER TABLE app_settings ADD COLUMN historyTtlHours INTEGER NOT NULL DEFAULT -2")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notifications.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
