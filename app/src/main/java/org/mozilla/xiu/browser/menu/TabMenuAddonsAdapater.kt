package org.mozilla.xiu.browser.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.xiu.browser.componets.MenuAddonsListCallback
import org.mozilla.xiu.browser.databinding.ItemMenuAddonsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension

class TabMenuAddonsAdapater : ListAdapter<WebExtension, TabMenuAddonsAdapater.ItemTestViewHolder>(MenuAddonsListCallback) {
    lateinit var select: Select

    inner class ItemTestViewHolder(private val binding: ItemMenuAddonsBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(bean: WebExtension, mContext: Context){
            mContext as LifecycleOwner
            bean.setActionDelegate(object :WebExtension.ActionDelegate{
                override fun onBrowserAction(
                    extension: WebExtension,
                    session: GeckoSession?,
                    action: WebExtension.Action
                ) {
                    mContext.lifecycleScope.launch {
                        binding.addonsIcon.setImageBitmap(withContext(Dispatchers.IO) {
                            action.icon?.getBitmap(
                                128
                            )?.poll()
                        })
                    }
                    binding.addonsIcon.setOnClickListener { action.click() }
                }
                override fun onTogglePopup(
                    extension: WebExtension,
                    action: WebExtension.Action
                ): GeckoResult<GeckoSession>? {
                    val session=GeckoSession()
                    select.onSelect(session)
                    return GeckoResult.fromValue(session)
                }
                override fun onOpenPopup(
                    extension: WebExtension,
                    action: WebExtension.Action
                ): GeckoResult<GeckoSession>? {
                    val session=GeckoSession()
                    select.onSelect(session)
                    return GeckoResult.fromValue(session)
                }
            })


        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTestViewHolder {
        return ItemTestViewHolder(ItemMenuAddonsBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ItemTestViewHolder, position: Int) {
        //通过ListAdapter内部实现的getItem方法找到对应的Bean
        holder.bind(getItem(holder.adapterPosition),holder.itemView.context)

    }
    interface Select{
        fun onSelect(session: GeckoSession)
    }

}