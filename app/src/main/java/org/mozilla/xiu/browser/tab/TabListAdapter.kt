package org.mozilla.xiu.browser.tab

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.databinding.ItemTablistPhoneBinding
import org.mozilla.xiu.browser.session.DelegateLivedata
import org.mozilla.xiu.browser.session.SessionDelegate

class TabListAdapter :
    ListAdapter<SessionDelegate, TabListAdapter.ItemTestViewHolder>(TabListCallback) {
    lateinit var select: Select

    inner class ItemTestViewHolder(private val binding: ItemTablistPhoneBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bean: SessionDelegate, mContext: Context) {
            binding.user = getItem(bindingAdapterPosition)
            binding.wholeTab?.setOnClickListener {
                DelegateLivedata.getInstance().Value(getItem(bindingAdapterPosition))
                HomeLivedata.getInstance().Value(false)
                select.onSelect()
            }
            binding.materialButton?.setOnClickListener {
                RemoveTabLiveData.getInstance().Value(bindingAdapterPosition)
            }
            binding.deleteButton?.setOnClickListener {
                RemoveTabLiveData.getInstance().Value(bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTestViewHolder {
        return ItemTestViewHolder(
            ItemTablistPhoneBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemTestViewHolder, position: Int) {
        //通过ListAdapter内部实现的getItem方法找到对应的Bean
        holder.bind(getItem(holder.adapterPosition), holder.itemView.context)
        holder.itemView.context
    }

    interface Select {
        fun onSelect()
    }

}