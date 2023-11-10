package org.mozilla.xiu.browser.broswer.bookmark.sync

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.databinding.ItemBookmarkBinding
import mozilla.components.concept.storage.BookmarkNode

class SyncBookmarkItemAdapter: ListAdapter<BookmarkNode, SyncBookmarkItemAdapter.ItemTestViewHolder>(
    SyncBookmarkListCallback
) {
    lateinit var select: Select
    lateinit var popupSelect: PopupSelect

    inner class ItemTestViewHolder(private val binding: ItemBookmarkBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(bean: BookmarkNode, mContext: Context){

            binding.textView9.text=bean.title
            bean.parentGuid?.let { Log.d("BookmarkNode1", it) }

            binding.textView10.text=bean.url
            binding.bookmarkItem.setOnClickListener { bean.url?.let { it1 -> select.onSelect(it1) } }
            binding.materialButton18.visibility = View.GONE



        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTestViewHolder {
        return ItemTestViewHolder(ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ItemTestViewHolder, position: Int) {
        //通过ListAdapter内部实现的getItem方法找到对应的Bean
        holder.bind(getItem(holder.adapterPosition),holder.itemView.context)

    }
    interface Select{
        fun onSelect(url: String)
    }
    interface PopupSelect{
        fun onPopupSelect(bean: Bookmark, item:Int)
    }



}