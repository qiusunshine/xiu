package org.mozilla.xiu.browser.broswer.bookmark.shortcut

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.database.shortcut.Shortcut
import org.mozilla.xiu.browser.databinding.ItemShortcutBinding
import org.mozilla.xiu.browser.utils.DisplayUtil
import org.mozilla.xiu.browser.view.BaseDividerItem
import java.net.URI

class ShortcutAdapter(
    var context: Context?
) : ListAdapter<Shortcut, ShortcutAdapter.ItemTestViewHolder>(
    ShortcutListCallback
) {
    lateinit var select: Select
    lateinit var longClick: LongClick
    var dividerItem = MyDividerItem()

    inner class ItemTestViewHolder(private val binding: ItemShortcutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bean: Shortcut, mContext: Context) {
            val lp = binding.root.layoutParams as GridLayoutManager.LayoutParams
            val dp_5 = DisplayUtil.dpToPx(context, 5)
            val manager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val outMetrics = DisplayMetrics()
            manager.defaultDisplay.getMetrics(outMetrics)
            var screen = outMetrics.widthPixels
            if (outMetrics.widthPixels > outMetrics.heightPixels) {
                //横屏
                screen -= DisplayUtil.dp2px(context, 215f)
            }

            lp.width = (screen - dp_5 * 3 * 5) / 4
            lp.setMargins(0, 0, 0, dp_5 * 3)
            val w = minOf(DisplayUtil.dp2px(context, 75f), lp.width)
            val lp2 = binding.materialCardView8.layoutParams as ConstraintLayout.LayoutParams
            lp2.width = w
            lp2.height = w
            binding.materialCardView8.layoutParams = lp2
            binding.root.layoutParams = lp
            binding.textView21.text = bean.title
            when (bean.url) {
                "hiker://bookmark" -> {
                    Glide.with(mContext)
                        .load(R.drawable.icon1)
                        .apply(RequestOptions().transform(CircleCrop()))
                        .into(binding.imageView10)
                }

                "hiker://download" -> {
                    Glide.with(mContext)
                        .load(R.drawable.home_download)
                        .apply(RequestOptions().transform(CircleCrop()))
                        .into(binding.imageView10)
                }

                "hiker://history" -> {
                    Glide.with(mContext)
                        .load(R.drawable.icon3)
                        .apply(RequestOptions().transform(CircleCrop()))
                        .into(binding.imageView10)
                }

                else -> {
                    val uri = URI.create(bean.url)
                    val faviconUrl = uri.scheme + "://" + uri.host + "/favicon.ico"
                    Glide.with(mContext)
                        .load(faviconUrl)
                        .apply(
                            RequestOptions().placeholder(R.drawable.globe).transform(CircleCrop())
                        )
                        .into(binding.imageView10)
                }
            }
            binding.materialCardView8.setOnClickListener {
                bean.url?.let { it1 ->
                    select.onSelect(
                        it1
                    )
                }
            }
            binding.materialCardView8.setOnLongClickListener {
                dialog(mContext, bean)
                false
            }


        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTestViewHolder {
        return ItemTestViewHolder(
            ItemShortcutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemTestViewHolder, position: Int) {
        holder.bind(getItem(holder.adapterPosition), holder.itemView.context)
    }

    interface Select {
        fun onSelect(url: String)
    }

    interface LongClick {
        fun onLongClick(bean: Shortcut)
    }

    private fun dialog(context: Context, bean: Shortcut) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.dialog_shortcut_title))
            .setNegativeButton(context.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(context.getString(R.string.confirm)) { dialog, which ->
                longClick.onLongClick(bean)
            }
            .show()
    }

    inner class MyDividerItem : BaseDividerItem() {
        override fun getLeftRight(itemViewType: Int): Int {
            return DisplayUtil.dpToPx(
                context,
                15
            )
        }
    }

}