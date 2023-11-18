package org.mozilla.xiu.browser.componets

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import org.mozilla.xiu.browser.databinding.ItemAddonsManagerBinding
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.webextension.WebExtensionRuntimeManager
import org.mozilla.xiu.browser.webextension.WebExtensionWrapper
import org.mozilla.xiu.browser.webextension.WebExtensionsEnableEvent

class AddonsAdapter :
    ListAdapter<WebExtensionWrapper, AddonsAdapter.ItemTestViewHolder>(MenuAddonsListCallback) {
    lateinit var select: Select

    inner class ItemTestViewHolder(private val binding: ItemAddonsManagerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bean: WebExtensionWrapper, mContext: Context) {
            mContext as LifecycleOwner
            val webExtensionController = GeckoRuntime.getDefault(mContext).webExtensionController
            mContext.lifecycleScope.launch {
                binding.textView8.text = bean.name
            }
            bean.extension.metaData.icon.getBitmap(72)
                .accept { binding.imageView3.setImageBitmap(it) }

            binding.materialCardView4.setOnClickListener { select.onSelect(bean.extension) }
            //RecyclerView复用问题
            binding.switch1.setOnCheckedChangeListener { compoundButton, b ->

            }
            binding.switch1.isChecked = bean.enabled
            binding.switch1.setOnCheckedChangeListener { compoundButton, b ->
                if (b && bean.enabled) {
                    return@setOnCheckedChangeListener
                }
                if (!b && !bean.enabled) {
                    return@setOnCheckedChangeListener
                }
                if (b) {
                    if (WebExtensionRuntimeManager.isTempExtension(bean.extension)) {
                        compoundButton.isChecked = false
                        ToastMgr.shortBottomCenter(
                            mContext,
                            "当前扩展程序无法启用，可试试换个网页"
                        )
                        return@setOnCheckedChangeListener
                    }
                    bean.enabled = true
                    webExtensionController.enable(
                        bean.extension,
                        WebExtensionController.EnableSource.USER
                    ).accept({ ext ->
                        if (ext != null) {
                            //对象会发生改变
                            ToastMgr.shortBottomCenter(mContext, "已启用" + ext.metaData.name)
                            EventBus.getDefault()
                                .post(WebExtensionsEnableEvent(bean.extension, false))
                            EventBus.getDefault().post(WebExtensionsEnableEvent(ext, true))
                            bean.extension = ext
                            WebExtensionRuntimeManager.refresh()
                        }
                    }) { e ->
                        //操作失败了，复原数据
                        bean.enabled = false
                        ToastMgr.shortCenter(mContext, "出错：$e")
                    }
                } else {
                    bean.enabled = false
                    webExtensionController.disable(
                        bean.extension,
                        WebExtensionController.EnableSource.USER
                    ).accept({ ext ->
                        ToastMgr.shortBottomCenter(mContext, "已禁用" + bean.name)
                        if (ext != null) {
                            //对象会发生改变
                            EventBus.getDefault()
                                .post(WebExtensionsEnableEvent(bean.extension, false))
                            bean.extension = ext
                        }
                        WebExtensionRuntimeManager.refresh()
                    }) { e ->
                        //操作失败了，复原数据
                        bean.enabled = true
                        ToastMgr.shortCenter(mContext, "出错：$e")
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTestViewHolder {
        return ItemTestViewHolder(
            ItemAddonsManagerBinding.inflate(
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
        fun onSelect(bean: WebExtension)
    }

    object MenuAddonsListCallback : DiffUtil.ItemCallback<WebExtensionWrapper>() {
        override fun areItemsTheSame(
            oldItem: WebExtensionWrapper,
            newItem: WebExtensionWrapper
        ): Boolean {
            return oldItem == newItem


        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: WebExtensionWrapper,
            newItem: WebExtensionWrapper
        ): Boolean {
            return oldItem == newItem

        }

    }
}