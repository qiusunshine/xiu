package org.mozilla.xiu.browser.database.bookmark

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import org.mozilla.xiu.browser.fxa.Fxa

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {
    var bookmarkRepository: BookmarkRepository

    init {
        bookmarkRepository = BookmarkRepository(application)
    }

    val allBookmarksLive: LiveData<List<Bookmark?>?>?
        get() = bookmarkRepository.allBookmarkLive


    fun loadAllBookmarks(): List<Bookmark?>? {
        return bookmarkRepository.loadAllBookmarks()
    }

    fun findBookmarksWithPattern(pattern: String): LiveData<List<Bookmark?>?>? {
        return bookmarkRepository.findBookmarksWithPattern(pattern)
    }

    fun findBookmarksWithTitle(pattern: String?): LiveData<List<Bookmark?>?>? {
        return bookmarkRepository.findBookmarksWithTitle(pattern)
    }

    fun findBookmarkWithTitle(pattern: String?): List<Bookmark?>? {
        return bookmarkRepository.findBookmarkWithTitle(pattern)
    }

    fun findBookmarkWithUrl(url: String?): Bookmark? {
        return bookmarkRepository.findBookmarkWithUrl(url)
    }

    fun findBookmarksWithUrl(url: String?): LiveData<List<Bookmark?>?>? {
        return bookmarkRepository.findBookmarksWithUrl(url)
    }

    fun findBookmarksWithShow(pattern: Boolean?): LiveData<List<Bookmark?>?>? {
        return bookmarkRepository.findBookmarksWithShow(pattern)
    }

    fun insertBookmarks(vararg bookmarks: Bookmark) {
        bookmarkRepository.insertBookmark(*bookmarks)
    }

    fun insertBookmarksSync(vararg bookmarks: Bookmark): Array<Int> {
        return bookmarkRepository.insertBookmarkSync(*bookmarks)
    }

    fun updateBookmarks(vararg bookmarks: Bookmark) {
        bookmarkRepository.updateBookmark(*bookmarks)
    }

    fun deleteBookmarks(vararg bookmarks: Bookmark) {
        bookmarkRepository.deleteBookmark(*bookmarks)
        Fxa.bookmarkSync?.delete(listOf(*bookmarks))
    }

    fun deleteAllBookmarks() {
        bookmarkRepository.deleteAllbookmarks()
        Fxa.bookmarkSync?.deleteAll()
    }
}