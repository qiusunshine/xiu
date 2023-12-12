package org.mozilla.xiu.browser.componets

import android.animation.Animator
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ContextElement
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.databinding.DiaContextmenuBinding
import org.mozilla.xiu.browser.session.createSession
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.copyToDownloadDir
import timber.log.Timber
import java.io.File

class ContextMenuDialog(
    var context: FragmentActivity,
    element: ContextElement,
    val download: (src: String) -> Unit
) :
    AlertDialog(context) {
    var binding: DiaContextmenuBinding
    var type: String? = null

    init {
        binding = DiaContextmenuBinding.inflate(LayoutInflater.from(context))
        if (element.type == ContextElement.TYPE_IMAGE) {
            Glide.with(context).load(element.srcUri).into(binding.imageView19)
            binding.diaContextmenuDownloadButton.setOnClickListener {
                if (!element.srcUri.isNullOrEmpty()) {
                    if (element.srcUri!!.startsWith("data")) {
                        ThreadTool.executeNewTask {
                            try {
                                val file: File =
                                    Glide.with(getContext()).downloadOnly().load(element.srcUri)
                                        .submit().get()
                                if (!file.exists()) {
                                    Timber.d("File exists: %s", file.absolutePath)
                                    ToastMgr.shortBottomCenter(context, "出错：文件不存在")
                                    return@executeNewTask
                                }
                                var file2 = File(file.parent, file.name + ".png")
                                if (!file.renameTo(file2)) {
                                    file2 = file
                                }
                                val result = copyToDownloadDir(context, file2.absolutePath)
                                if (StringUtil.isNotEmpty(result)) {
                                    ToastMgr.shortBottomCenter(context, "下载成功")
                                } else {
                                    ToastMgr.shortBottomCenter(context, "下载失败")
                                }
                                file2.delete()
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        download(element.srcUri!!)
                    }
                    dismiss()
                }
            }
        } else {
            binding.diaContextmenuDownloadButton.visibility = View.GONE
            binding.imageView19.visibility = View.GONE
        }
        binding.diaContextmenuNewTabButton.setOnClickListener {
            val link = element.linkUri ?: element.srcUri
            if (link.isNullOrEmpty()) {
                ToastMgr.shortBottomCenter(context, context.getString(R.string.url_not_found))
            } else {
                dismiss()
                createSession(link, context)
            }
        }

        val link = element.srcUri ?: element.linkUri
        if (link.isNullOrEmpty()) {
            binding.diaContextmenuCopyButton.visibility = View.GONE
            binding.diaContextmenuOpenButton.visibility = View.GONE
        } else {
            binding.diaContextmenuCopyButton.setOnClickListener {
                copyToClipboard(context, link)
                dismiss()
            }
            binding.diaContextmenuOpenButton.setOnClickListener {
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                type = when (element.type) {
                    ContextElement.TYPE_IMAGE -> {
                        "image/*"
                    }

                    ContextElement.TYPE_VIDEO -> {
                        "video/*"
                    }

                    ContextElement.TYPE_AUDIO -> {
                        "audio/*"
                    }

                    else -> {
                        "*/*"
                    }
                }
                val uri = Uri.parse(link)
                intent.setDataAndType(uri, type)
                context.startActivity(intent)
            }
        }
        if(element.textContent.isNullOrEmpty()) {
            binding.copyTextButton.visibility = View.GONE
        } else {
            binding.copyTextButton.visibility = View.VISIBLE
            binding.copyTextButton.setOnClickListener {
                binding.diaContextmenuNewTabButton.animate().alpha(0f).setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {

                    }

                    override fun onAnimationEnd(animation: Animator) {
                        binding.textContentView.visibility = View.VISIBLE
                        binding.textContentView.text = element.textContent
                        binding.textContentView.animate().alpha(1f).start()
                        binding.diaContextmenuNewTabButton.visibility = View.GONE
                        binding.copyTextButton.visibility = View.GONE
                        binding.diaContextmenuCopyButton.visibility = View.GONE
                        binding.diaContextmenuOpenButton.visibility = View.GONE
                    }

                    override fun onAnimationCancel(animation: Animator) {

                    }

                    override fun onAnimationRepeat(animation: Animator) {

                    }

                }).start()
                binding.copyTextButton.animate().alpha(0f).start()
                binding.diaContextmenuCopyButton.animate().alpha(0f).start()
                binding.diaContextmenuOpenButton.animate().alpha(0f).start()
            }
        }
        setView(binding.root)
    }

    fun open() {
        window!!.setBackgroundDrawable(context.getDrawable(R.drawable.bg_dialog))
        show()
    }

    companion object {
        fun copyToClipboard(context: Context, content: String?) {
            // 从 API11 开始 android 推荐使用 android.content.ClipboardManager
            // 为了兼容低版本我们这里使用旧版的 android.text.ClipboardManager，虽然提示 deprecated，但不影响使用。
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // 将文本内容放到系统剪贴板里。
            cm.text = content
            Toast.makeText(context, "已复制到剪切板", Toast.LENGTH_SHORT).show()
        }
    }
}