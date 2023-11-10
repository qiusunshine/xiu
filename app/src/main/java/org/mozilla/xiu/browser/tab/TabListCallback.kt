package org.mozilla.xiu.browser.tab

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import org.mozilla.xiu.browser.session.SessionDelegate

object TabListCallback : DiffUtil.ItemCallback<SessionDelegate>() {
    override fun areItemsTheSame(oldItem: SessionDelegate, newItem: SessionDelegate): Boolean {
        return oldItem.u == newItem.u
                && oldItem.session == newItem.session
                && oldItem.mTitle == newItem.mTitle
                && oldItem.bitmap == newItem.bitmap
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: SessionDelegate, newItem: SessionDelegate): Boolean {
        return oldItem.u == newItem.u
                && oldItem.session == newItem.session
                && oldItem.mTitle == newItem.mTitle
                && oldItem.bitmap == newItem.bitmap
    }

}