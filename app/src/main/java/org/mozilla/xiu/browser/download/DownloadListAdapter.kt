package org.mozilla.xiu.browser.download

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.databinding.ItemDownloadBinding
import org.mozilla.xiu.browser.utils.deleteFileByPath


class DownloadListAdapter(
    var refresh: () -> Unit
) :
    ListAdapter<DownloadTask, DownloadListAdapter.ItemTestViewHolder>(DownListCallback) {

    inner class ItemTestViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.Q)
        fun bind(bean: DownloadTask, mContext: Context) {
            binding.task = getItem(adapterPosition)
            val type = DownloadChooser.smartFilm(bean.filename, true)
            var id = R.drawable.icon_unknown
            when (type) {
                "安装包" -> {
                    id = R.drawable.icon_app3
                }

                "压缩包" -> {
                    id = R.drawable.icon_zip2
                }

                "音乐/音频" -> {
                    id = R.drawable.icon_music3
                }

                "文档/电子书" -> {
                    id = R.drawable.icon_txt2
                }

                "其它格式" -> {
                    //id = R.drawable.icon_unknown;
                }

                "图片" -> {
                    id = R.drawable.icon_pic3
                }

                "视频" -> {
                    id = R.drawable.icon_video2
                }
            }
            Glide.with(mContext)
                .load(id)
                .into(binding.imageView4)
            binding.downloadButton2.setOnClickListener {
                when (bean.state) {
                    0 -> {
                        bean.open()
                    }

                    2 -> {
                        getUri(mContext, bean.filename)
                            ?.let { it1 -> openUri(mContext, bean.filename, it1) }
                    }

                    else -> bean.pause()
                }
            }
            binding.materialCardView5.setOnClickListener {
                if (bean.state == 2) {
                    getUri(mContext, bean.filename)
                        ?.let { it1 -> openUri(mContext, bean.filename, it1) }
                }
            }
            binding.materialCardView5.setOnLongClickListener {
                if (bean.state == 2) {
                    MaterialAlertDialogBuilder(mContext)
                        .setTitle(mContext.getString(R.string.notify))
                        .setMessage(
                            "确定删除${bean.filename}文件"
                        )
                        .setPositiveButton(mContext.getString(R.string.confirm)) { d, _ ->
                            d.dismiss()
                            DownloadTaskLiveData.getInstance().remove(bean)
                            getUri(mContext, bean.filename)
                                ?.let { it1 ->
                                    deleteFileByPath(mContext, it1.toString())
                                    refresh()
                                }
                        }.setNegativeButton(mContext.getString(R.string.cancel)) { d, _ ->
                            d.dismiss()
                        }.show()
                } else {
                    MaterialAlertDialogBuilder(mContext)
                        .setTitle(mContext.getString(R.string.notify))
                        .setMessage(
                            "确定取消${bean.filename}下载任务吗"
                        )
                        .setPositiveButton(mContext.getString(R.string.confirm)) { d, _ ->
                            d.dismiss()
                            bean.pause()
                            DownloadTaskLiveData.getInstance().remove(bean)
                            getUri(mContext, bean.filename)
                                ?.let { it1 ->
                                    deleteFileByPath(mContext, it1.toString())
                                    refresh()
                                }
                        }.setNegativeButton(mContext.getString(R.string.cancel)) { d, _ ->
                            d.dismiss()
                        }.show()
                }
                true
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTestViewHolder {
        return ItemTestViewHolder(
            ItemDownloadBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onBindViewHolder(holder: ItemTestViewHolder, position: Int) {
        //通过ListAdapter内部实现的getItem方法找到对应的Bean
        holder.bind(getItem(holder.adapterPosition), holder.itemView.context)
    }
}