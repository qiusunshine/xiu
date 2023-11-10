package org.mozilla.xiu.browser.broswer.history

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import org.mozilla.xiu.browser.database.history.History

object HistoryListCallback : DiffUtil.ItemCallback<History>() {
    override fun areItemsTheSame(oldItem: History, newItem: History): Boolean {
        return oldItem ==newItem

    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: History, newItem: History): Boolean {
        return oldItem ==newItem
    }

}