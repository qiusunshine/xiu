package org.mozilla.xiu.browser.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.database.bookmark.BookmarkDao
import org.mozilla.xiu.browser.database.history.History
import org.mozilla.xiu.browser.database.history.HistoryDao
import org.mozilla.xiu.browser.database.shortcut.Shortcut
import org.mozilla.xiu.browser.database.shortcut.ShortcutDao

@Database(entities = [Bookmark::class, History::class,Shortcut::class], version = 1, exportSchema = false)
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
                    .build()
            }
            return INSTANCE
        }
    }
}