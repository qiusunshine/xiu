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


class BookmarkDialog(
    private var context1: Activity,
    title: String?,
    url: String?
) :
    MyDialog(context1) {
    var diaBookmarkBinding: DiaBookmarkBinding = DiaBookmarkBinding.inflate(LayoutInflater.from(context))
    var shortcutViewModel: ShortcutViewModel = ViewModelProvider(context1 as ViewModelStoreOwner).get<ShortcutViewModel>(ShortcutViewModel::class.java)
    var bookmarkViewModel: BookmarkViewModel = ViewModelProvider(context1 as ViewModelStoreOwner).get<BookmarkViewModel>(BookmarkViewModel::class.java)

    init {
        diaBookmarkBinding.textView5.setText(R.string.dia_add_bookmark_title)
        diaBookmarkBinding.diaBookmarkTitle.setText(title)
        diaBookmarkBinding.diaBookmarkUrl.setText(url)
        setView(diaBookmarkBinding.getRoot())
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.confirm),
            DialogInterface.OnClickListener { dialogInterface, i ->
                val bookmark = Bookmark(
                    diaBookmarkBinding.diaBookmarkUrl.getText().toString(),
                    diaBookmarkBinding.diaBookmarkTitle.getText().toString(),
                    "默认",
                    diaBookmarkBinding.radioButton.isChecked
                )
                val shortcut = Shortcut(
                    diaBookmarkBinding.diaBookmarkUrl.getText().toString(),
                    diaBookmarkBinding.diaBookmarkTitle.getText().toString(),
                    0
                )
                if (diaBookmarkBinding.radioButton.isChecked)
                    shortcutViewModel.insertShortcuts(shortcut)
                bookmarkViewModel.insertBookmarks(bookmark)
                //BookmarkSync(context1).sync(diaBookmarkBinding.diaBookmarkUrl.getText().toString(),diaBookmarkBinding.diaBookmarkTitle.getText().toString())
            })
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel),
            DialogInterface.OnClickListener { dialogInterface, i -> dialogInterface.dismiss() })
    }

    fun open() {
        super.show()
    }
}