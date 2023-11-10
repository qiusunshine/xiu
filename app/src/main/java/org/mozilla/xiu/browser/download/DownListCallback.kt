package org.mozilla.xiu.browser.download

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil

object DownListCallback : DiffUtil.ItemCallback<DownloadTask>() {
    override fun areItemsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
        return oldItem.currentSize == newItem.currentSize
                && oldItem.title == newItem.title
                && oldItem.progress == newItem.progress
                && oldItem.totalSize == newItem.totalSize
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
        return oldItem.currentSize == newItem.currentSize
                && oldItem.title == newItem.title
                && oldItem.progress == newItem.progress
                && oldItem.totalSize == newItem.totalSize
    }

}