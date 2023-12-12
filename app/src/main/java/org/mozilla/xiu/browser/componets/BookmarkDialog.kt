package org.mozilla.xiu.browser.componets

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.util.KeyboardUtils
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.database.bookmark.BookmarkViewModel
import org.mozilla.xiu.browser.database.shortcut.Shortcut
import org.mozilla.xiu.browser.database.shortcut.ShortcutViewModel
import org.mozilla.xiu.browser.databinding.DiaBookmarkBinding
import org.mozilla.xiu.browser.utils.CollectionUtil
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.addParentId
import org.mozilla.xiu.browser.utils.getGroupPath
import org.mozilla.xiu.browser.utils.getGroupPaths
import org.mozilla.xiu.browser.utils.getTree
import org.mozilla.xiu.browser.utils.initBookmarkParent
import org.mozilla.xiu.browser.view.CustomCenterRecyclerViewPopup


class BookmarkDialog(
    private var context1: Activity,
    title: String?,
    url: String?,
    icon: String?,
    oldBookmark: Bookmark? = null
) :
    MyDialog(context1) {
    var isCheckSelected: Boolean = false
    var diaBookmarkBinding: DiaBookmarkBinding =
        DiaBookmarkBinding.inflate(LayoutInflater.from(context))
    var shortcutViewModel: ShortcutViewModel =
        ViewModelProvider(context1 as ViewModelStoreOwner).get(ShortcutViewModel::class.java)
    var bookmarkViewModel: BookmarkViewModel =
        ViewModelProvider(context1 as ViewModelStoreOwner).get(BookmarkViewModel::class.java)
    var group1 = ""
    var all: List<Bookmark?>? = null

    fun loadDirs() {
        if (all == null) {
            all = bookmarkViewModel.loadAllBookmarks()
            all?.let {
                initBookmarkParent(it)
            }
        }
        val groups = getGroupPaths(context1, all)
        if (CollectionUtil.isEmpty(groups)) {
            ToastMgr.shortCenter(context, "还没有文件夹，先新建一个吧")
        } else {
            KeyboardUtils.hideSoftInput(context1.window)
            val popup: CustomCenterRecyclerViewPopup = CustomCenterRecyclerViewPopup(context)
                .withTitle("选择文件夹")
                .useCenter(false)
                .with(
                    getTree(groups),
                    1,
                    object : CustomCenterRecyclerViewPopup.ClickListener {
                        override fun click(url: String?, position: Int) {
                            group1 = groups[position]
                            diaBookmarkBinding.dirButton.text = groups[position]
                        }

                        override fun onLongClick(url: String?, position: Int) {}
                    })
            XPopup.Builder(context)
                .asCustom(popup)
                .show()
        }
    }

    init {
        diaBookmarkBinding.textView5.setText(if (oldBookmark != null) R.string.edit_bookmark_title else R.string.dia_add_bookmark_title)
        diaBookmarkBinding.diaBookmarkTitle.setText(oldBookmark?.title ?: title)
        diaBookmarkBinding.diaBookmarkUrl.setText(oldBookmark?.url ?: url)
        if (oldBookmark != null) {
            group1 = getGroupPath(oldBookmark)
            if (group1.isNotEmpty()) {
                diaBookmarkBinding.dirButton.text = group1
            }
        }
        setView(diaBookmarkBinding.root)
        diaBookmarkBinding.radioButton.setOnClickListener {
            isCheckSelected = !isCheckSelected
            diaBookmarkBinding.radioButton.isChecked = isCheckSelected
        }
        diaBookmarkBinding.dirButton.setOnClickListener {
            loadDirs()
        }
        setButton(
            DialogInterface.BUTTON_POSITIVE, context.getString(R.string.confirm)
        ) { _, i ->
            if (StringUtil.isEmpty(group1) || "/" == group1) {
                group1 = ""
            }
            if (oldBookmark != null) {
                //更新
                ThreadTool.async {
                    oldBookmark.title = diaBookmarkBinding.diaBookmarkTitle.text.toString()
                    oldBookmark.url = diaBookmarkBinding.diaBookmarkUrl.text.toString()
                    if (group1.isNotEmpty()) {
                        if (all == null) {
                            all = bookmarkViewModel.loadAllBookmarks()
                            all?.let {
                                initBookmarkParent(it)
                            }
                        }
                        addParentId(oldBookmark, all ?: arrayListOf(), group1)
                    }
                    val shortcut = Shortcut(
                        diaBookmarkBinding.diaBookmarkUrl.text.toString(),
                        diaBookmarkBinding.diaBookmarkTitle.text.toString(),
                        0,
                        oldBookmark.icon
                    )
                    if (diaBookmarkBinding.radioButton.isChecked) {
                        val exist = shortcutViewModel.findShortcutWithUrl(shortcut.url)
                        if (exist == null) {
                            shortcutViewModel.insertShortcuts(shortcut)
                        }
                    }
                    val exist = bookmarkViewModel.findBookmarkWithUrl(oldBookmark.url)
                    if (exist == null || exist.id == oldBookmark.id) {
                        bookmarkViewModel.updateBookmarks(oldBookmark)
                        ToastMgr.shortBottomCenter(
                            context1,
                            context1.getString(R.string.update_success)
                        )
                    } else {
                        ToastMgr.shortBottomCenter(
                            context1,
                            context1.getString(R.string.dia_add_bookmark_exist)
                        )
                    }
                }
            } else {
                //新建
                ThreadTool.async {
                    val bookmark = Bookmark(
                        diaBookmarkBinding.diaBookmarkUrl.text.toString(),
                        diaBookmarkBinding.diaBookmarkTitle.text.toString(),
                        "默认",
                        isCheckSelected,
                        icon = icon ?: ""
                    )
                    if (group1.isNotEmpty()) {
                        if (all == null) {
                            all = bookmarkViewModel.loadAllBookmarks()
                            all?.let {
                                initBookmarkParent(it)
                            }
                        }
                        addParentId(bookmark, all ?: arrayListOf(), group1)
                    }
                    val shortcut = Shortcut(
                        diaBookmarkBinding.diaBookmarkUrl.text.toString(),
                        diaBookmarkBinding.diaBookmarkTitle.text.toString(),
                        0,
                        icon ?: ""
                    )
                    if (diaBookmarkBinding.radioButton.isChecked) {
                        val exist = shortcutViewModel.findShortcutWithUrl(shortcut.url)
                        if (exist == null) {
                            shortcutViewModel.insertShortcuts(shortcut)
                        }
                    }
                    val exist = bookmarkViewModel.findBookmarkWithUrl(bookmark.url)
                    if (exist == null) {
                        bookmarkViewModel.insertBookmarks(bookmark)
                        ToastMgr.shortBottomCenter(
                            context1,
                            context1.getString(R.string.dia_add_bookmark_success)
                        )
                    } else {
                        ToastMgr.shortBottomCenter(
                            context1,
                            context1.getString(R.string.dia_add_bookmark_exist)
                        )
                    }
                }
            }
            //BookmarkSync(context1).sync(diaBookmarkBinding.diaBookmarkUrl.getText().toString(),diaBookmarkBinding.diaBookmarkTitle.getText().toString())
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