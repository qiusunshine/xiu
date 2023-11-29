package org.mozilla.xiu.browser.utils

import android.content.ContentResolver
import android.net.Uri
import android.webkit.URLUtil
import androidx.annotation.NonNull
import org.mozilla.geckoview.WebResponse
import org.mozilla.xiu.browser.download.getDispositionFileName


object UriUtils {
    fun isUriContentScheme(@NonNull uri: Uri): Boolean {
        return uri.scheme == ContentResolver.SCHEME_CONTENT
    }

    fun isUriFileScheme(@NonNull uri: Uri): Boolean {
        return uri.scheme == ContentResolver.SCHEME_FILE
    }


    fun getFileName(url: String): String {
        var filename = URLUtil.guessFileName(url, null, null)
        filename = filename.substring(
            0,
            filename.lastIndexOf(".")
        ) + "_${System.currentTimeMillis()}" + filename.substring(filename.lastIndexOf("."))
        return filename
    }

    fun getFileName(response: WebResponse): String {
        return getFileName(response.uri, response.headers)
    }

    fun getFileName(uri: String, headers: Map<String, String>): String {
        var fileName: String? = null
        val contentDispositionHeader: String?
        contentDispositionHeader = if (headers.containsKey("content-disposition")) {
            headers["content-disposition"]
        } else {
            headers["Content-Disposition"]
        }
        if (contentDispositionHeader != null && !contentDispositionHeader.isEmpty()) {
            fileName = getDispositionFileName(contentDispositionHeader)
        }
        if (StringUtil.isEmpty(fileName)) {
            fileName = FileUtil.getResourceName(uri)
        }
        if (StringUtil.isEmpty(fileName)) {
            fileName = UUIDUtil.genUUID()
        }
        if (fileName?.contains(".") == false && uri.contains("edgeextension")) {
            fileName = "$fileName.crx"
        }
        if (fileName?.contains(".") == false) {
            val exts = ShareUtil.getExtensions()
            for (ext in exts) {
                if (uri.contains(".$ext")) {
                    fileName = "$fileName.$ext"
                    break
                }
            }
        }
        return fileName ?: ""
    }

}