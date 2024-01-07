package org.mozilla.xiu.browser.fxa.sync

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.BookmarkInfo
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.sync.SyncReason
import org.mozilla.xiu.browser.ActivityManager
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.fxa.AccountManagerCollection
import org.mozilla.xiu.browser.fxa.Fxa
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.getGroupPath

class BookmarkSync {
    private lateinit var accountManager: FxaAccountManager
    fun initAccountManager(context: Context) {
        val accountManagerCollection: AccountManagerCollection =
            ViewModelProvider(context as ViewModelStoreOwner)[AccountManagerCollection::class.java]
        lifecycleScope.launch {
            accountManagerCollection.data.collect {
                accountManager = it
            }
        }
    }

    private val lifecycleScope get() = (ActivityManager.instance.currentActivity as LifecycleOwner).lifecycleScope

    fun sync(bookmark: Bookmark, groupPath: String) {
        if (!::accountManager.isInitialized) {
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            sync0(bookmark, groupPath, true)
        }
    }

    fun syncList(bookmarks: List<Bookmark>) {
        if (!::accountManager.isInitialized) {
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            for (bookmark in bookmarks) {
                if (bookmark.isDir()) continue
                val groupPath = getGroupPath(bookmark)
                sync0(bookmark, groupPath, false)
            }
            accountManager.syncNow(SyncReason.User)
        }
    }

    fun delete(bookmarks: List<Bookmark>) {
        if (!::accountManager.isInitialized) {
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val root = Fxa.bookmarksStorage.value.getTree("mobile______", true)
            for (bookmark in bookmarks) {
                val exist = findNode(bookmark, root)
                if (exist != null) {
                    Fxa.bookmarksStorage.value.deleteNode(exist.guid)
                }
            }
            accountManager.syncNow(SyncReason.User)
        }
    }

    fun deleteAll() {
        if (!::accountManager.isInitialized) {
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val root = Fxa.bookmarksStorage.value.getTree("mobile______", false)
            if (root?.children != null) {
                for (node in root.children!!) {
                    Fxa.bookmarksStorage.value.deleteNode(node.guid)
                }
            }
            accountManager.syncNow(SyncReason.User)
        }
    }

    private fun findNode(bookmark: Bookmark, node: BookmarkNode?): BookmarkNode? {
        if (node == null) return null
        if (bookmark.isDir() && bookmark.title == node.title) {
            return node
        }
        if (node.url == bookmark.url) {
            return node
        }
        if (node.children == null) {
            return null
        }
        for (child in node.children!!) {
            val n = findNode(bookmark, child)
            if (n != null) {
                return n
            }
        }
        return null
    }

    private suspend fun sync0(bookmark: Bookmark, groupPath: String, syncNow: Boolean) {
        val root = Fxa.bookmarksStorage.value.getTree("mobile______", true)
        val list = root?.children
        var parentGuid = "mobile______"
        list?.let { arr ->
            //创建文件夹
            if ("/" != groupPath && !StringUtil.isEmpty(groupPath)) {
                val paths = groupPath.split("/").dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                var remoteList: List<BookmarkNode>? = arr
                for (dir in paths) {
                    var exist = false
                    remoteList?.let { li ->
                        for (node in li) {
                            if (node.title == dir && node.type.name == "FOLDER") {
                                exist = true
                                remoteList = node.children
                                parentGuid = node.guid
                                break
                            }
                        }
                    }
                    if (!exist) {
                        parentGuid = Fxa.bookmarksStorage.value.addFolder(parentGuid, dir, null)
                        remoteList = null
                    }
                }
            }
            //检查一下已经存在直接返回
            val exist = findNode(bookmark, root)
            if (exist != null) {
                val info = BookmarkInfo(
                    parentGuid,
                    if (parentGuid == exist.parentGuid) exist.position else null,
                    bookmark.title,
                    bookmark.url
                )
                Fxa.bookmarksStorage.value.updateNode(exist.guid, info)
                accountManager.syncNow(SyncReason.User)
                return
            }
        }
        Fxa.bookmarksStorage.value.addItem(parentGuid, bookmark.url, bookmark.title, null)
        if (syncNow) {
            accountManager.syncNow(SyncReason.User)
        }
    }
}