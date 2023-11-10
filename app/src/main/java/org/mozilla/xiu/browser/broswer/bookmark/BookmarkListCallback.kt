package org.mozilla.xiu.browser.broswer.bookmark

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import org.mozilla.xiu.browser.database.bookmark.Bookmark

object BookmarkListCallback : DiffUtil.ItemCallback<Bookmark>() {
    override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
        return oldItem ==newItem

    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
        return oldItem ==newItem
    }

}