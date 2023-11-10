package org.mozilla.xiu.browser.download

import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.contentdisposition.ContentDispositionHolder
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