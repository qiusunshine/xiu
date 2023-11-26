package org.mozilla.xiu.browser.utils

import android.content.Context
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.broswer.bookmark.data.ChromeParser
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.database.bookmark.BookmarkRepository
import org.mozilla.xiu.browser.database.bookmark.BookmarkViewModel
import org.mozilla.xiu.browser.database.history.History
import java.util.Locale

class GroupUtils {
    var list: List<Bookmark?>
    private lateinit var list2: List<History?>
    private var mList = ArrayList<Bookmark>()
    private var group: String? = null
    var tags = ArrayList<String>()

    constructor(list: List<Bookmark?>) {
        this.list = list
        if (!list.isNullOrEmpty())
            retrieval()

    }

    fun groupBookmark(): List<Bookmark?> {
        return mList.toList()
    }

    fun groupTagBookmark(): ArrayList<String> {
        return tags
    }

    fun groupHistory(): List<History?> {
        return list2
    }

    fun retrieval() {
        group = list[0]?.file
        group?.let { tags.add(it) }
        for (i in list) {
            if (i != null) {
                if (i.file == group)
                    mList.add(i)
                if (list.lastIndexOf(i) == list.size - 1) {
                    list = list.toMutableList().apply { removeAll(mList) }
                    if (list.isNotEmpty())
                        retrieval()
                }
            }
        }
    }
}

fun initBookmarkParent(list: List<Bookmark?>) {
    val map: MutableMap<Int, Bookmark> = HashMap()
    for (bookmark in list) {
        if (bookmark == null) continue
        map[bookmark.id] = bookmark
    }
    for (bookmark in list) {
        if (bookmark == null) continue
        if (bookmark.parentId > 0) {
            bookmark.parent = map[bookmark.parentId]
        }
    }
}

fun filterList(list: List<Bookmark?>, groupSelected: String, key: String): List<Bookmark> {
    var lKey = if (StringUtil.isEmpty(key)) key else key.lowercase(Locale.getDefault())
    if ("离线页面" == key) {
        lKey = "offline_pages"
    }
    val bookmarks: MutableList<Bookmark> = java.util.ArrayList()
    for (bookmark in list) {
        if (bookmark == null) continue
        if (StringUtil.isNotEmpty(lKey)) {
            //搜索的时候不要文件夹
            if (bookmark.isDir()) {
                continue
            }
            val ok = bookmark.title.lowercase(Locale.getDefault())
                .contains(lKey) || StringUtil.isNotEmpty(
                bookmark.url
            ) && bookmark.url.lowercase(Locale.getDefault()).contains(lKey)
            if (!ok) {
                continue
            }
            bookmarks.add(bookmark)
        } else {
            //没搜索的时候
            val groupPath: String = getGroupPath(bookmark)
            if (StringUtil.isNotEmpty(groupSelected)) {
                if (!bookmark.isDir() && groupSelected == groupPath) {
                    //书签
                    bookmarks.add(bookmark)
                } else if (bookmark.isDir() && groupPath.contains(groupSelected)) {
                    //文件夹，只显示一层文件夹
                    if (groupPath.split("/").dropLastWhile { it.isEmpty() }
                            .toTypedArray().size == groupSelected.split("/")
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray().size + 1
                    ) {
                        bookmarks.add(bookmark)
                    }
                }
            } else {
                if (!bookmark.isDir() && groupPath.isEmpty()) {
                    //无文件夹的书签
                    bookmarks.add(bookmark)
                } else if (bookmark.isDir() && bookmark.parent == null) {
                    //第一层文件夹
                    bookmarks.add(bookmark)
                }
            }
        }
    }
    return bookmarks.sortedWith { o1, o2 ->
        if (o1?.isDir() == true || o2 == null) {
            -1
        } else if (o2.isDir() || o1 == null) {
            1
        } else {
            o1.id.compareTo(o2.id)
        }
    }
}

fun getGroupPaths(context: Context, list0: List<Bookmark?>? = null): List<String> {
    var list = list0
    if (list.isNullOrEmpty()) {
        list = BookmarkRepository(context).loadAllBookmarks() ?: arrayListOf()
        initBookmarkParent(list)
    }
    val paths = list.filterNotNull().filter { it.isDir() }
        .map {
            val path: String = getGroupPath(it)
            if (path.isEmpty()) {
                return@map "/"
            } else {
                return@map path
            }
        }.distinct().filter { it: String -> "/" != it }.toMutableList()
    paths.add(0, "/")
    paths.sort()
    return paths
}

fun getGroupPath(bookmark: Bookmark): String {
    var groupPath = ""
    if (bookmark.isDir()) {
        groupPath = bookmark.title
    }
    var parent: Bookmark? = bookmark.parent
    while (parent != null) {
        if (parent.isDir()) {
            if (groupPath.isEmpty()) {
                groupPath = parent.title
            } else {
                groupPath = parent.title + "/" + groupPath
            }
        }
        parent = parent.parent
    }
    return groupPath
}

fun getTree(groupPaths: List<String>): Array<String> {
    val showTitles: MutableList<String> = java.util.ArrayList(groupPaths)
    for (i in showTitles.indices) {
        if ("/" != showTitles[i]) {
            val ss = showTitles[i].split("/").dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (ss.size > 1) {
                for (i1 in 0 until ss.size - 1) {
                    ss[i1] = "    "
                }
                showTitles[i] = StringUtil.arrayToString(ss, 0, "")
            }
        }
    }
    return showTitles.toTypedArray()
}

fun addParentId(bookmark: Bookmark, list: List<Bookmark?>, group1: String) {
    var pid: Int = findDirIdByPath(list, group1)
    if (pid == -1 && StringUtil.isNotEmpty(group1)) {
        //自动创建文件夹
        val bookmark12 = Bookmark(title = "")
        bookmark12.setDir(true)
        val ts = group1.split("/".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (ts.isEmpty()) {
            bookmark.parentId = -1
            return
        }
        val g = ts[ts.size - 1]
        if (g.isEmpty()) {
            bookmark.parentId = -1
            return
        }
        bookmark12.title = g
        BookmarkRepository(App.getContext()).bookmarkDao!!.insertBookmark(bookmark12)
        pid = bookmark12.id
    }
    bookmark.parentId = pid
}

fun findDirIdByPath(list: List<Bookmark?>, path: String): Int {
    val dir = findDirByPath(list, path)
    return dir?.id ?: -1
}

fun findDirByPath(list: List<Bookmark?>, path: String): Bookmark? {
    if ("/" == path || StringUtil.isEmpty(path)) {
        return null
    }
    val paths = path.split("/").dropLastWhile { it.isEmpty() }
        .toTypedArray()
    val dirs: List<Bookmark> = list.filterNotNull().filter { it.isDir() }
    var parent: Bookmark? = null
    for (s in paths) {
        var find = false
        for (dir in dirs) {
            if (parent == null) {
                //第一层只根据名字找
                if (dir.parent == null && s == dir.title) {
                    parent = dir
                    find = true
                }
            } else {
                //非第一层根据父级文件夹+名字找
                if (dir.parent != null && parent.id == dir.parent?.id && s == dir.title
                ) {
                    parent = dir
                    find = true
                }
            }
        }
        if (parent == null) {
            //第一圈下来找不到最上层，那就再也找不到了
            return null
        }
        if (!find) {
            //每一层都要找到才算
            return null
        }
    }
    return parent
}


fun addByList(
    context: Context,
    bookmarkViewModel: BookmarkViewModel,
    bookmarks: List<Bookmark>
): Int {
    var count = 0
    for (bookmark in bookmarks) {
        if (StringUtil.isEmpty(bookmark.url)) {
            continue
        }
        if (StringUtil.isNotEmpty(bookmark.group)) {
            val groups: List<String> = bookmark.group!!.split("@@@")
            var parentId: Int = -1
            for (group0 in groups) {
                var group = group0
                group = group.replace("/", "-")
                var dir: Bookmark?
                dir = if (parentId > 0) {
                    val list0 = bookmarkViewModel.findBookmarkWithTitle(group)
                    if (!list0.isNullOrEmpty()) {
                        list0.filter { it != null && it.parentId == parentId && it.isDir() }
                            .firstOrNull()
                    } else {
                        null
                    }
                } else {
                    val list0 = bookmarkViewModel.findBookmarkWithTitle(group)
                    if (!list0.isNullOrEmpty()) {
                        list0.filter { it != null && it.parentId <= 0 && it.isDir() }.firstOrNull()
                    } else {
                        null
                    }
                }
                if (dir == null) {
                    dir = Bookmark(
                        title = group,
                        dir = 1,
                        parentId = if (parentId > 0) parentId else 0
                    )
                    val ids = bookmarkViewModel.insertBookmarksSync(dir)
                    if (!ids.isNullOrEmpty()) {
                        dir.id = ids.first()
                    }
                }
                parentId = dir.id
            }
            if (parentId > 0) {
                bookmark.parentId = parentId
            }
        }
        val ids = bookmarkViewModel.insertBookmarksSync(bookmark)
        if (!ids.isNullOrEmpty()) {
            bookmark.id = ids.first()
        }
        count++
    }
    return count
}

fun deleteByGroupNode(
    bookmarkViewModel: BookmarkViewModel,
    groupNode: ChromeParser.BookmarkGroupNode
) {
    if (CollectionUtil.isNotEmpty(groupNode.bookmarks)) {
        bookmarkViewModel.deleteBookmarks(*groupNode.bookmarks.toTypedArray())
    }
    if (groupNode.bookmark != null) {
        bookmarkViewModel.deleteBookmarks(groupNode.bookmark)
    }
    if (CollectionUtil.isNotEmpty(groupNode.childGroups)) {
        for (childGroup in groupNode.childGroups) {
            deleteByGroupNode(bookmarkViewModel, childGroup)
        }
    }
}

