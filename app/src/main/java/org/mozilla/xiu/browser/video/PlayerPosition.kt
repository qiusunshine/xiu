package org.mozilla.xiu.browser.video

import android.content.Context
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.annotation.JSONCreator
import org.mozilla.xiu.browser.utils.FileUtil
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.UriUtilsPro
import java.io.File

/**
 * 作者：By 15968
 * 日期：On 2023/9/1
 * 时间：At 16:15
 */
data class PlayerPosition(
    var url: String = "",
    var position: Long = 0
) {
    @JSONCreator
    constructor() : this("", 0L) {

    }
}

fun loadPlayPosList(context: Context): MutableList<PlayerPosition> {
    try {
        val path = UriUtilsPro.getRootDir(context) + File.separator + "position.json"
        if (File(path).exists()) {
            val json = FileUtil.fileToString(path)
            if (!json.isNullOrEmpty()) {
                return JSON.parseArray(json, PlayerPosition::class.java)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return mutableListOf()
}

fun getPlayPos(context: Context, url: String): Long {
    try {
        val arr = loadPlayPosList(context)
        for (position in arr) {
            if (position.url == url) {
                return position.position
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return 0L
}

fun addPlayerPosition(context: Context, url: String, position: Long, callback: () -> Unit = {}) {
    if (url.isEmpty()) {
        return
    }
    ThreadTool.async {
        val list = loadPlayPosList(context)
        for (item in list.withIndex()) {
            if (item.value.url == url) {
                list.removeAt(item.index)
                break
            }
        }
        list.add(PlayerPosition(url, position))
        if (list.size > 100) {
            list.removeAt(0)
        }
        val path = UriUtilsPro.getRootDir(context) + File.separator + "position.json"
        FileUtil.stringToFile(JSON.toJSONString(list), path)
        callback()
    }
}