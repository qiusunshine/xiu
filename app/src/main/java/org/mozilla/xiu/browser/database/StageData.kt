package org.mozilla.xiu.browser.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.database.bookmark.BookmarkDao
import org.mozilla.xiu.browser.database.history.History
import org.mozilla.xiu.browser.database.history.HistoryDao
import org.mozilla.xiu.browser.database.shortcut.Shortcut
import org.mozilla.xiu.browser.database.shortcut.ShortcutDao

@Database(
    entities = [Bookmark::class, History::class, Shortcut::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(1, 2)]
)
abstract class StageData : RoomDatabase() {
    abstract val historyDao: HistoryDao?
    abstract val bookmarkDao: BookmarkDao?
    abstract val shortcutDao: ShortcutDao?

    companion object {
        private var INSTANCE: StageData? = null

        @JvmStatic
        @Synchronized
        fun getDatabase(context: Context): StageData? {
            if (INSTANCE == null) {
                INSTANCE = databaseBuilder(
                    context.applicationContext,
                    StageData::class.java,
                    "UserDatabase"
                )
                    .allowMainThreadQueries()
                    //1,2没有开启exportSchema导致无法自动迁移
//                    .addMigrations(object : Migration(1, 2) {
//                        override fun migrate(database: SupportSQLiteDatabase) {
//                            database.execSQL("ALTER TABLE `History`  ADD COLUMN `dir` INTEGER NOT NULL DEFAULT 0")
//                            database.execSQL("ALTER TABLE `History`  ADD COLUMN `parentId` INTEGER NOT NULL DEFAULT 0")
//                            database.execSQL("ALTER TABLE `History`  ADD COLUMN `icon` TEXT NOT NULL DEFAULT ''")
//                            database.execSQL("ALTER TABLE `Shortcut`  ADD COLUMN `icon` TEXT NOT NULL DEFAULT ''")
//                        }
//                    })
                    .build()
            }
            return INSTANCE
        }
    }
}