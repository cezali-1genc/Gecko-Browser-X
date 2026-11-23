package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class BrowserConverters {
    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus =
        runCatching { DownloadStatus.valueOf(value) }.getOrDefault(DownloadStatus.FAILED)

    @TypeConverter
    fun fromPermissionDecision(value: PermissionDecision): String = value.name

    @TypeConverter
    fun toPermissionDecision(value: String): PermissionDecision =
        runCatching { PermissionDecision.valueOf(value) }.getOrDefault(PermissionDecision.ASK)
}

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        DownloadEntity::class,
        SitePermissionEntity::class,
        SpeedDialEntity::class,
        SavedCredentialEntity::class,
        ReadingListEntity::class,
        BrowserExtensionEntity::class,
        DictionaryEntryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(BrowserConverters::class)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun sitePermissionDao(): SitePermissionDao
    abstract fun speedDialDao(): SpeedDialDao
    abstract fun credentialDao(): CredentialDao
    abstract fun readingListDao(): ReadingListDao
    abstract fun extensionDao(): ExtensionDao
    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        @Volatile
        private var instance: BrowserDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN failureReason TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN supportsResume INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_credentials` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `origin` TEXT NOT NULL,
                        `host` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `encryptedPassword` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_credentials_origin_username` ON `saved_credentials` (`origin`, `username`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_credentials_origin` ON `saved_credentials` (`origin`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_list` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `excerpt` TEXT NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `savedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reading_list_url` ON `reading_list` (`url`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `browser_extensions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `extensionId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `assetPath` TEXT NOT NULL,
                        `isBuiltIn` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `customRuleDomains` TEXT NOT NULL,
                        `installedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_browser_extensions_extensionId` ON `browser_extensions` (`extensionId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dictionary_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `term` TEXT NOT NULL,
                        `trDef` TEXT NOT NULL,
                        `enDef` TEXT NOT NULL,
                        `isUserAdded` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dictionary_entries_term` ON `dictionary_entries` (`term`)")
            }
        }

        fun getInstance(context: Context): BrowserDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "browserx_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
