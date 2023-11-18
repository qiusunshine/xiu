package org.mozilla.xiu.browser.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.databinding.ItemDownloadBinding
import org.mozilla.xiu.browser.utils.ShareUtil
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.UriUtilsPro
import org.mozilla.xiu.browser.utils.deleteFile
import org.mozilla.xiu.browser.webextension.WebExtensionsAddEvent
import rxhttp.wrapper.utils.query
import java.io.File


class DownloadListAdapter(
    var refresh: () -> Unit
) :
    ListAdapter<DownloadTask, DownloadListAdapter.ItemTestViewHolder>(DownListCallback) {
    private val relativePath: String =
        Environment.DIRECTORY_DOWNLOADS + File.separator + ContextCompat.getString(
            App.application!!,
            R.string.app_name
        )

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getInsertUri() = MediaStore.Downloads.EXTERNAL_CONTENT_URI

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
                        getInsertUri().query(mContext, bean.filename, relativePath)
                            ?.let { it1 -> open(mContext, bean.filename, it1) }
                    }
                    else -> bean.pause()
                }
            }
            binding.materialCardView5.setOnClickListener {
                if (bean.state == 2) {
                    getInsertUri().query(mContext, bean.filename, relativePath)
                        ?.let { it1 -> open(mContext, bean.filename, it1) }
                }
            }
            binding.materialCardView5.setOnLongClickListener {
                if (bean.state == 2) {
                    MaterialAlertDialogBuilder(mContext)
                        .setTitle("温馨提示")
                        .setMessage(
                            "确定删除${bean.filename}文件"
                        )
                        .setPositiveButton("确定") { d, _ ->
                            d.dismiss()
                            val list =
                                ArrayList(DownloadTaskLiveData.getInstance().value ?: arrayListOf())
                            val ok = list.remove(bean)
                            if (ok) {
                                DownloadTaskLiveData.getInstance().Value(list)
                            }
                            getInsertUri().query(mContext, bean.filename, relativePath)
                                ?.let { it1 ->
                                    deleteFile(mContext, it1.toString())
                                    refresh()
                                }
                        }.setNegativeButton("取消") { d, _ ->
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

    fun open(context: Context, name: String, uri: Uri) {
        if (name.contains(".crx") || name.contains(".xpi")) {
            val path: String =
                UriUtilsPro.getRootDir(context) + File.separator + "_cache" + File.separator + name
            if (File(path).exists()) {
                File(path).delete()
            }
            UriUtilsPro.getFilePathFromURI(
                context,
                uri,
                path,
                object : UriUtilsPro.LoadListener {
                    override fun success(s: String) {
                        ThreadTool.runOnUI {
                            EventBus.getDefault().post(WebExtensionsAddEvent(s))
                        }
                    }

                    override fun failed(msg: String) {
                        ToastMgr.shortBottomCenter(
                            context,
                            "出错：$msg"
                        )
                    }
                })
            return
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.setDataAndType(uri, "*/*");
        ShareUtil.findChooser(context, intent)
    }
}