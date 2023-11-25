package org.mozilla.xiu.browser.componets

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.database.bookmark.BookmarkViewModel
import org.mozilla.xiu.browser.database.shortcut.Shortcut
import org.mozilla.xiu.browser.database.shortcut.ShortcutViewModel
import org.mozilla.xiu.browser.databinding.DiaBookmarkBinding
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ToastMgr


class BookmarkDialog(
    private var context1: Activity,
    title: String?,
    url: String?
) :
    MyDialog(context1) {
    var isCheckSelected: Boolean = false
    var diaBookmarkBinding: DiaBookmarkBinding =
        DiaBookmarkBinding.inflate(LayoutInflater.from(context))
    var shortcutViewModel: ShortcutViewModel =
        ViewModelProvider(context1 as ViewModelStoreOwner).get(ShortcutViewModel::class.java)
    var bookmarkViewModel: BookmarkViewModel =
        ViewModelProvider(context1 as ViewModelStoreOwner).get(BookmarkViewModel::class.java)

    init {
        diaBookmarkBinding.textView5.setText(R.string.dia_add_bookmark_title)
        diaBookmarkBinding.diaBookmarkTitle.setText(title)
        diaBookmarkBinding.diaBookmarkUrl.setText(url)
        setView(diaBookmarkBinding.root)
        diaBookmarkBinding.radioButton.setOnClickListener {
            isCheckSelected = !isCheckSelected
            diaBookmarkBinding.radioButton.isChecked = isCheckSelected
        }
        setButton(
            DialogInterface.BUTTON_POSITIVE, context.getString(R.string.confirm)
        ) { _, i ->
            val bookmark = Bookmark(
                diaBookmarkBinding.diaBookmarkUrl.getText().toString(),
                diaBookmarkBinding.diaBookmarkTitle.getText().toString(),
                "默认",
                isCheckSelected
            )
            val shortcut = Shortcut(
                diaBookmarkBinding.diaBookmarkUrl.getText().toString(),
                diaBookmarkBinding.diaBookmarkTitle.getText().toString(),
                0
            )
            ThreadTool.async {
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