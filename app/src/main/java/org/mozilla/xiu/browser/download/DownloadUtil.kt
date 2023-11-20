package org.mozilla.xiu.browser.download

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.interfaces.OnBindView
import kotlinx.coroutines.CoroutineScope
import org.greenrobot.eventbus.EventBus
import org.mozilla.geckoview.WebResponse
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.utils.ComponentNameFilter
import org.mozilla.xiu.browser.utils.FileUtil
import org.mozilla.xiu.browser.utils.ShareUtil
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.UriTool.getIntentChooser
import org.mozilla.xiu.browser.utils.UriUtilsPro
import org.mozilla.xiu.browser.utils.contentdisposition.ContentDispositionHolder
import org.mozilla.xiu.browser.webextension.WebExtensionsAddEvent
import rxhttp.wrapper.utils.query
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

/**
 * 作者：By 15968
 * 日期：On 2023/10/15
 * 时间：At 21:06
 */

/**
 * 解析文件名
 *
 * @param dispositionHeader
 * @return
 */
fun getDispositionFileName(dispositionHeader: String?): String {
    var dispositionHeader = dispositionHeader
    try {
        val holder = ContentDispositionHolder(dispositionHeader)
        var name: String = holder.filename
        if (holder.parseException == null && StringUtil.isNotEmpty(name)) {
            name = decodeUrl(name, "UTF-8")!!.trim { it <= ' ' }
            return if (name.contains("UTF-8''")) {
                if (name.endsWith("UTF-8''")) {
                    name.split("UTF-8''".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                } else {
                    name.split("UTF-8''".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                }
            } else name
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    if (!dispositionHeader.isNullOrEmpty()) {
        dispositionHeader =
            dispositionHeader.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$".toRegex(), "$1")
        val strings = dispositionHeader.split(";".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (strings.size > 1) {
            dispositionHeader = strings[0]
        }
        if (dispositionHeader.contains(".") && !dispositionHeader.contains("filename=")) {
            return decodeUrl(dispositionHeader, "UTF-8")!!.trim { it <= ' ' }
        }
    }
    return ""
}

fun decodeUrl(str: String?, code: String): String? { //url解码
    var str = str
    try {
        str = str!!.replace("%(?![0-9a-fA-F]{2})".toRegex(), "%25")
        str = str.replace("\\+".toRegex(), "%2B")
        str = URLDecoder.decode(str, code)
    } catch (e: UnsupportedEncodingException) {
        e.printStackTrace()
    }
    return str
}

fun downloadBlob(
    context: Context,
    name: String,
    response: WebResponse,
    callback: suspend CoroutineScope.(path: String, e: Throwable?) -> Unit
) {
    ThreadTool.async {
        val dir: String = UriUtilsPro.getRootDir(context) + File.separator + "_cache"
        val downloadsPath = dir + File.separator + name
        if (File(downloadsPath).exists()) {
            File(downloadsPath).delete()
        }
        FileUtil.makeSureDirExist(downloadsPath)
        Timber.i("Downloading to: %s", downloadsPath)
        val bufferSize = 1024 // to read in 1Mb increments
        val buffer = ByteArray(bufferSize)
        try {
            response.body?.use { body ->
                BufferedOutputStream(FileOutputStream(downloadsPath)).use { out ->
                    var len: Int
                    while (body.read(buffer).also { len = it } != -1) {
                        out.write(buffer, 0, len)
                    }
                    callback(downloadsPath, null)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            callback(downloadsPath, e)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun getInsertUri() = MediaStore.Downloads.EXTERNAL_CONTENT_URI

fun getFilePath(context: Context, filename: String): String {
    val relativePath: String =
        Environment.DIRECTORY_DOWNLOADS + File.separator + ContextCompat.getString(
            context,
            R.string.app_name
        )
    return relativePath + File.separator + filename
}

fun getUri(context: Context, filename: String): Uri? {
    val relativePath: String =
        Environment.DIRECTORY_DOWNLOADS + File.separator + ContextCompat.getString(
            context,
            R.string.app_name
        )
    return getUri(context, filename, relativePath)
}

fun getUri(context: Context, filename: String, relativePath: String): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getInsertUri().query(context, filename, relativePath)
    } else {
        Uri.fromFile(File(relativePath, filename))
    }
}

fun openUri(context: Context, name: String, uri: Uri) {
    if (name.contains(".crx") || name.contains(".xpi")) {
        openCrxOrXpi(context, name, uri)
        return
    }
    openIntent(context, name, uri)
}

fun openUriBeforePop(context: Context, uri: Uri) {
    val name = UriUtilsPro.getFileName(uri)
    if (name.contains(".crx") || name.contains(".xpi")) {
        openCrxOrXpi(context, name, uri)
        ToastMgr.shortBottomCenter(
            App.application,
            context.getString(R.string.download_finished, name)
        )
    } else {
        PopTip.build()
            .setCustomView(object :
                OnBindView<PopTip?>(R.layout.pop_mytip) {
                override fun onBind(dialog: PopTip?, v: View) {
                    v.findViewById<TextView>(R.id.textView17).text =
                        context.getString(R.string.download_finished, name)
                    val btn = v.findViewById<MaterialButton>(R.id.materialButton7)
                    btn.text = ContextCompat.getString(context, R.string.open)
                    btn.setOnClickListener {
                        openIntent(context, name, uri)
                        dialog?.dismiss()
                    }
                }
            })
            .showLong()
    }
}

private fun openCrxOrXpi(context: Context, name: String, uri: Uri) {
    if (uri.scheme == "file") {
        ThreadTool.runOnUI {
            EventBus.getDefault().post(WebExtensionsAddEvent(uri.toString().replace("file://", "")))
        }
        return
    }
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
}

fun openIntent(context: Context, name: String, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    //模拟一个url出来
    val url = "file:///$name"
    val type = ShareUtil.getMIMEType(url) ?: "*/*"
    intent.setDataAndType(uri, type)
    val share = getIntentChooser(context, intent, "请选择应用",
        object : ComponentNameFilter {
            override fun shouldBeFilteredOut(componentName: ComponentName): Boolean {
                return context.packageName == componentName.packageName
            }
        })
    if (share == null) {
        context.startActivity(Intent.createChooser(intent, "请选择应用"))
    } else {
        context.startActivity(share)
    }
}