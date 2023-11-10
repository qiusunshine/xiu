package org.mozilla.xiu.browser.broswer.bookmark.shortcut

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import org.mozilla.xiu.browser.database.shortcut.Shortcut

object ShortcutListCallback : DiffUtil.ItemCallback<Shortcut>() {
    override fun areItemsTheSame(oldItem: Shortcut, newItem: Shortcut): Boolean {
        return oldItem ==newItem

    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Shortcut, newItem: Shortcut): Boolean {
        return oldItem ==newItem
    }

}