package org.mozilla.xiu.browser.componets.popup

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.greenrobot.eventbus.EventBus
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.base.toActivity
import org.mozilla.xiu.browser.databinding.ItemNetworkBinding
import org.mozilla.xiu.browser.session.createSession
import org.mozilla.xiu.browser.utils.ClipboardUtil
import org.mozilla.xiu.browser.video.event.FloatPlayEvent
import org.mozilla.xiu.browser.webextension.TabRequest

class NetworkAdapter : ListAdapter<TabRequest, NetworkAdapter.ItemTestViewHolder>(NetworkCallback) {
    lateinit var select: Select

    inner class ItemTestViewHolder(private val binding: ItemNetworkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bean: TabRequest, mContext: Context) {
            var text = bean.url
            if (bean.type == TabRequest.VIDEO || bean.type == TabRequest.AUDIO) {
                if (!bean.size.isNullOrEmpty()) {
                    text = "[" + bean.size + "] " + text
                }
            }
            binding.textView.text = text
            if (bean.type == TabRequest.IMAGE) {
                binding.imageViewContainer.visibility = View.VISIBLE
                Glide.with(mContext)
                    .load(bean.url)
                    .placeholder(R.drawable.icon_load_failed)
                    .into(binding.imageView)
            } else {
                binding.imageViewContainer.visibility = View.GONE
            }
            binding.item.setOnClickListener { showMenu(it, bean, mContext) }
            binding.item.setOnLongClickListener {
                showMenu(it, bean, mContext)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTestViewHolder {
        return ItemTestViewHolder(
            ItemNetworkBinding.inflate(
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
        fun onSelect(tabRequest: TabRequest)
    }

    private fun showMenu(v: View, bean: TabRequest, context: Context) {
        val popup = PopupMenu(context!!, v)
        popup.menuInflater.inflate(R.menu.network_item_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.network_copy -> {
                    ClipboardUtil.copyToClipboard(context, bean.url)
                }

                R.id.network_browse -> {
                    context.toActivity()?.let {
                        createSession(bean.url, it)
                        select.onSelect(bean)
                    }
                }

                R.id.network_float_play -> {
                    select.onSelect(bean)
                    EventBus.getDefault().post(FloatPlayEvent(bean))
                }
            }
            false
        }
        popup.setOnDismissListener {
        }
        popup.show()
    }
}

object NetworkCallback : DiffUtil.ItemCallback<TabRequest>() {
    override fun areItemsTheSame(oldItem: TabRequest, newItem: TabRequest): Boolean {
        return oldItem == newItem
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: TabRequest, newItem: TabRequest): Boolean {
        return oldItem == newItem
    }
}
