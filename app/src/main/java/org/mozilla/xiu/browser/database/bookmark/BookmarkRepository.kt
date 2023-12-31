package org.mozilla.xiu.browser.database.bookmark

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import org.mozilla.xiu.browser.database.StageData.Companion.getDatabase

class BookmarkRepository internal constructor(context: Context) {
    var bookmarkDao: BookmarkDao?
    var allBookmarkLive: LiveData<List<Bookmark?>?>?

    init {
        val stageData = getDatabase(context.applicationContext)
        bookmarkDao = stageData!!.bookmarkDao
        allBookmarkLive = bookmarkDao?.allBookmarksLive
    }

    fun loadAllBookmarks(): List<Bookmark?>? {
        return bookmarkDao?.allBookmarks
    }

    fun insertBookmark(vararg bookmarks: Bookmark) {
        InsertAsyncTask(bookmarkDao).execute(*bookmarks)
    }

    fun insertBookmarkSync(vararg bookmarks: Bookmark): Array<Int> {
        return bookmarkDao!!.insertBookmark(*bookmarks).map { it.toInt() }.toTypedArray()
    }

    fun updateBookmark(vararg bookmarks: Bookmark) {
        UpdateAsyncTask(bookmarkDao).execute(*bookmarks)
    }

    fun deleteBookmark(vararg bookmarks: Bookmark) {
        DeleteAsyncTask(bookmarkDao).execute(*bookmarks)
    }

    fun deleteAllbookmarks() {
        DeleteAllAsyncTask(bookmarkDao).execute()
    }

    fun findBookmarksWithPattern(pattern: String): LiveData<List<Bookmark?>?>? {
        return bookmarkDao!!.findBookmarksWithPattern("%$pattern%")
    }

    fun findBookmarksWithTitle(pattern: String?): LiveData<List<Bookmark?>?>? {
        return bookmarkDao!!.findBookmarksWithTitle(pattern)
    }

    fun findBookmarkWithTitle(pattern: String?): List<Bookmark?>? {
        return bookmarkDao!!.findBookmarkWithTitle(pattern)
    }

    fun findBookmarkWithUrl(url: String?): Bookmark? {
        val list = bookmarkDao!!.findBookmarksWithUrl(url)
        return if (list.isNullOrEmpty()) {
            return null
        } else {
            list.firstOrNull()
        }
    }

    fun findBookmarksWithUrl(url: String?): LiveData<List<Bookmark?>?>? {
        return bookmarkDao!!.findBookmarksWithUrl1(url)
    }

    fun findBookmarksWithShow(pattern: Boolean?): LiveData<List<Bookmark?>?>? {
        return bookmarkDao!!.findBookmarksWithShow(pattern)
    }

    internal class InsertAsyncTask(private val bookmarkDao: BookmarkDao?) :
        AsyncTask<Bookmark, Void?, Void?>() {
        protected override fun doInBackground(vararg params: Bookmark): Void? {
            bookmarkDao!!.insertBookmark(*params)
            return null
        }
    }

    internal class UpdateAsyncTask(private val bookmarkDao: BookmarkDao?) :
        AsyncTask<Bookmark, Void?, Void?>() {
        protected override fun doInBackground(vararg params: Bookmark): Void? {
            bookmarkDao!!.updateBookmark(*params)
            return null
        }
    }

    internal class DeleteAsyncTask(private val bookmarkDao: BookmarkDao?) :
        AsyncTask<Bookmark, Void?, Void?>() {
        protected override fun doInBackground(vararg params: Bookmark): Void? {
            bookmarkDao!!.deleteBookmark(*params)
            return null
        }
    }

    internal class DeleteAllAsyncTask(private val bookmarkDao: BookmarkDao?) :
        AsyncTask<Void?, Void?, Void?>() {
        protected override fun doInBackground(vararg params: Void?): Void? {
            bookmarkDao!!.deleteAllBookmark()
            return null
        }
    }
}