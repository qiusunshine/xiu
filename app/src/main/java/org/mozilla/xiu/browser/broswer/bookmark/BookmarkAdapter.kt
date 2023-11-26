package org.mozilla.xiu.browser.broswer.bookmark

import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.broswer.history.HistoryAdapter
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.databinding.ItemBookmarkBinding

class BookmarkAdapter : ListAdapter<Bookmark, BookmarkAdapter.ItemTestViewHolder>(
    BookmarkListCallback
) {
    lateinit var select: Select
    lateinit var popupSelect: PopupSelect

    inner class ItemTestViewHolder(private val binding: ItemBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bean: Bookmark, mContext: Context) {

            binding.textView9.text = bean.title
            binding.textView10.text = bean.url
            binding.bookmarkItem.setOnClickListener { bean.let { it1 -> select.onSelect(it1) } }
            binding.materialButton18.setOnClickListener {
                showMenu(it, bean, mContext)
            }
            if (!bean.isDir()) {
                binding.imageView13.visibility = View.GONE
                binding.textView10.visibility = View.VISIBLE
            } else {
                binding.imageView13.visibility = View.VISIBLE
                binding.textView10.visibility = View.GONE
            }
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
        fun onSelect(bean: Bookmark)
    }

    interface PopupSelect {
        fun onPopupSelect(bean: Bookmark, item: Int)
    }

    private fun showMenu(v: View, bean: Bookmark, context: Context) {
        val popup = PopupMenu(context!!, v)
        popup.menuInflater.inflate(R.menu.bookmark_item_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.menu_bookmark_item_update -> {
                    popupSelect.onPopupSelect(bean, HistoryAdapter.UPDATE)
                }
                R.id.menu_bookmark_item_delete -> {
                    popupSelect.onPopupSelect(bean, HistoryAdapter.DELETE)
                }

                R.id.menu_bookmark_item_add_home -> {
                    popupSelect.onPopupSelect(bean, HistoryAdapter.ADD_TO_HOMEPAGE)
                }
            }
            true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

}