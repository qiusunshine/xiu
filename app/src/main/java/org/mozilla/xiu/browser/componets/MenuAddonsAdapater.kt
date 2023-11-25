package org.mozilla.xiu.browser.componets

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import org.mozilla.xiu.browser.componets.popup.AddonsPopup
import org.mozilla.xiu.browser.componets.popup.NetworkPopup
import org.mozilla.xiu.browser.databinding.ItemMenuAddonsBinding
import org.mozilla.xiu.browser.session.DelegateLivedata

class MenuAddonsAdapater(
    val dismissCall: () -> Unit
) :
    ListAdapter<WebExtension, MenuAddonsAdapater.ItemTestViewHolder>(MenuAddonsListCallback) {
    lateinit var select: Select

    inner class ItemTestViewHolder(private val binding: ItemMenuAddonsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bean: WebExtension, mContext: Context) {
            mContext as LifecycleOwner
            bean.setActionDelegate(object : WebExtension.ActionDelegate {
                override fun onBrowserAction(
                    extension: WebExtension,
                    session: GeckoSession?,
                    action: WebExtension.Action
                ) {
                    mContext.lifecycleScope.launch {
                        binding.addonsIcon.setImageBitmap(withContext(Dispatchers.IO) {
                            action.icon?.getBitmap(
                                72
                            )?.poll()
                        })
                    }
                    binding.addonsIcon.setOnClickListener {
                        action.click()
                        dismissCall()
                    }
                }

                override fun onTogglePopup(
                    extension: WebExtension,
                    action: WebExtension.Action
                ): GeckoResult<GeckoSession>? {
                    val session = GeckoSession()
                    val addonsPopup = AddonsPopup(mContext)
                    addonsPopup.show(session, extension)
                    dismissCall()
                    return GeckoResult.fromValue(session)
                }

                override fun onOpenPopup(
                    extension: WebExtension,
                    action: WebExtension.Action
                ): GeckoResult<GeckoSession>? {
                    val session = GeckoSession()
                    val addonsPopup = AddonsPopup(mContext)
                    addonsPopup.show(session, extension)
                    dismissCall()
                    return GeckoResult.fromValue(session)
                }
            })
            if (bean.id == "xiutan@xiu.com") {
                binding.addonsIcon.setOnClickListener {
                    dismissCall()
                    DelegateLivedata.getInstance().value?.let {
                        NetworkPopup(mContext, it).show()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTestViewHolder {
        return ItemTestViewHolder(
            ItemMenuAddonsBinding.inflate(
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
        fun onSelect()
    }
}