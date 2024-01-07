package org.mozilla.xiu.browser.broswer.bookmark.sync

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.xiu.browser.databinding.ItemBookmarkBinding

class SyncBookmarkFolderAdapter :
    ListAdapter<BookmarkNode, SyncBookmarkFolderAdapter.ItemTestViewHolder>(
        SyncBookmarkListCallback
    ) {
    lateinit var select: Select

    inner class ItemTestViewHolder(private val binding: ItemBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bean: BookmarkNode, mContext: Context) {
            binding.textView9.text =
                bean.title
                    ?.replace("toolbar", "书签工具栏")
                    ?.replace("menu", "书签菜单")
                    ?.replace("mobile", "移动设备书签")
                    ?.replace("root", "所有书签")
            //?.replace("unfiled","所有书签")
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

}