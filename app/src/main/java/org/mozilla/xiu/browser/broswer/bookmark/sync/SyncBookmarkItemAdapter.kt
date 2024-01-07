package org.mozilla.xiu.browser.broswer.bookmark.sync

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.databinding.ItemBookmarkBinding

class SyncBookmarkItemAdapter :
    ListAdapter<BookmarkNode, SyncBookmarkItemAdapter.ItemTestViewHolder>(
        SyncBookmarkListCallback
    ) {
    lateinit var select: Select
    lateinit var popupSelect: PopupSelect

    inner class ItemTestViewHolder(private val binding: ItemBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bean: BookmarkNode, mContext: Context) {
            binding.textView9.text = bean.title
            bean.parentGuid?.let { Log.d("BookmarkNode1", it) }
            if (bean.type.name == "FOLDER") {
                binding.imageView13.visibility = View.VISIBLE
                binding.textView10.visibility = View.GONE
            } else {
                binding.imageView13.visibility = View.GONE
                binding.textView10.text = bean.url
                binding.textView10.visibility = View.VISIBLE
            }
            binding.bookmarkItem.setOnClickListener { select.onSelect(bean) }
            binding.materialButton18.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTestViewHolder {
        return ItemTestViewHolder(
            ItemBookmarkBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemTestViewHolder, position: Int) {
        //通过ListAdapter内部实现的getItem方法找到对应的Bean
        holder.bind(getItem(holder.adapterPosition), holder.itemView.context)

    }

    interface Select {
        fun onSelect(bean: BookmarkNode)
    }

    interface PopupSelect {
        fun onPopupSelect(bean: Bookmark, item: Int)
    }


}