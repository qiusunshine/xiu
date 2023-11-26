package org.mozilla.xiu.browser.componets

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.database.bookmark.BookmarkViewModel
import org.mozilla.xiu.browser.databinding.DiaBookmarkDirBinding
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.findDirIdByPath


class BookmarkDirDialog(
    private var context1: Activity,
    bookmarks: MutableList<Bookmark?>,
    groupSelected: String = "",
    title: String? = null,
    oldBookmark: Bookmark? = null
) :
    MyDialog(context1) {
    var diaBookmarkBinding: DiaBookmarkDirBinding =
        DiaBookmarkDirBinding.inflate(LayoutInflater.from(context))
    var bookmarkViewModel: BookmarkViewModel =
        ViewModelProvider(context1 as ViewModelStoreOwner).get(BookmarkViewModel::class.java)

    init {
        diaBookmarkBinding.textView5.setText(if (oldBookmark != null) R.string.rename_dir else R.string.create_dir)
        diaBookmarkBinding.diaBookmarkTitle.setText(oldBookmark?.title ?: title)
        setView(diaBookmarkBinding.root)
        setButton(
            DialogInterface.BUTTON_POSITIVE, context.getString(R.string.confirm)
        ) { _, i ->
            val t = diaBookmarkBinding.diaBookmarkTitle.text.toString()
            if (t.isEmpty()) {
                ToastMgr.shortBottomCenter(context1, context1.getString(R.string.cannot_be_empty))
                return@setButton
            }
            ThreadTool.async {
                if (oldBookmark != null) {
                    //更新
                    oldBookmark.title = t
                    val exist = bookmarkViewModel.findBookmarkWithTitle(oldBookmark.title)
                    if (exist.isNullOrEmpty() || exist.filter { it?.isDir() == true && it.id != oldBookmark.id }
                            .isEmpty()) {
                        bookmarkViewModel.updateBookmarks(oldBookmark)
                        ToastMgr.shortBottomCenter(
                            context1,
                            context1.getString(R.string.update_success)
                        )
                    } else {
                        ToastMgr.shortBottomCenter(
                            context1,
                            context1.getString(R.string.dir_exist)
                        )
                    }
                } else {
                    //新增
                    val bookmark = Bookmark(
                        title = t,
                        dir = 1
                    )
                    if (StringUtil.isNotEmpty(groupSelected)) {
                        bookmark.parentId = findDirIdByPath(bookmarks, groupSelected)
                    }
                    val exist = bookmarkViewModel.findBookmarkWithTitle(bookmark.title)
                    if (exist.isNullOrEmpty()) {
                        bookmarkViewModel.insertBookmarks(bookmark)
                        ToastMgr.shortBottomCenter(
                            context1,
                            context1.getString(R.string.dia_add_bookmark_success)
                        )
                    } else {
                        ToastMgr.shortBottomCenter(
                            context1,
                            context1.getString(R.string.dir_exist)
                        )
                    }
                }
            }
        }
        setButton(
            DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel)
        )
        { dialogInterface, i -> dialogInterface.dismiss() }
    }

    fun open() {
        super.show()
    }
}