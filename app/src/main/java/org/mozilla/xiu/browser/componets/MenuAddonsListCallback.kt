package org.mozilla.xiu.browser.componets

import android.annotation.SuppressLint
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.recyclerview.widget.DiffUtil
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.geckoview.WebExtension

object MenuAddonsListCallback : DiffUtil.ItemCallback<WebExtension>() {
    override fun areItemsTheSame(oldItem: WebExtension, newItem: WebExtension): Boolean {
        return oldItem == newItem


    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: WebExtension, newItem: WebExtension): Boolean {
        return oldItem == newItem

    }

}