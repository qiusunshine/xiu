package org.mozilla.xiu.browser.webextension

import android.content.Context
import com.annimon.stream.Stream
import org.apache.commons.lang3.StringUtils
import org.greenrobot.eventbus.EventBus
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.session.DelegateLivedata
import org.mozilla.xiu.browser.session.createSession
import org.mozilla.xiu.browser.tab.DelegateListLiveData
import org.mozilla.xiu.browser.utils.CollectionUtil
import org.mozilla.xiu.browser.utils.PreferenceMgr
import org.mozilla.xiu.browser.utils.PreferenceMgr2
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.ThreadTool.runOnUI
import java.io.File
import java.util.Locale

/**
 * 扩展程序风险管控
 *
 * 作者：By 15968
 * 日期：On 2023/11/16
 * 时间：At 8:55
 */
object WebExtensionRuntimeManager {
    var extensions = ArrayList<WebExtension>()
    var newtabs = HashMap<String, String>()

    init {
        val newtabUrl0 = getHomePageUrlOrNewTabUrl()
        if (!newtabUrl0.isNullOrEmpty()) {
            createNewTabUrlSession(newtabUrl0)
        }
        GeckoRuntime.getDefault(App.application!!).webExtensionController.list().accept {
            if (it != null) {
                extensions.addAll(it)
                initNewTabRecord(newtabUrl0.isNullOrEmpty())
                //启用那些被临时禁用的扩展程序
                runOnUI {
                    val t =
                        PreferenceMgr.getString(App.application, "tempDisabledExtensions", "")
                    if (StringUtil.isNotEmpty(t)) {
                        PreferenceMgr.remove(App.application, "tempDisabledExtensions")
                        val ids =
                            t.split("&&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        for (id in ids) {
                            for (extension in extensions) {
                                if (StringUtils.equals(
                                        extension.id,
                                        id
                                    ) && !extension.metaData.enabled
                                ) {
                                    GeckoRuntime.getDefault(App.application!!).webExtensionController
                                        .enable(extension, WebExtensionController.EnableSource.USER)
                                        .accept({ ext: WebExtension? ->
                                            if (ext != null) {
                                                EventBus.getDefault()
                                                    .post(
                                                        WebExtensionsEnableEvent(
                                                            extension,
                                                            false
                                                        )
                                                    )
                                                EventBus.getDefault()
                                                    .post(WebExtensionsEnableEvent(ext, true))
                                                replaceWebExtension(extension, ext)
                                            }
                                        }, { e: Throwable? ->
                                            e?.printStackTrace()
                                        })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun refresh(withNewTabUrl: Boolean = false) {
        extensions.clear()
        GeckoRuntime.getDefault(App.application!!).webExtensionController.list().accept {
            if (it != null) {
                extensions.addAll(it)
                tempDisabledExtensions.clear()
                val temp = Stream.of(tempDisabledExtensions).map { it1: WebExtension -> it1.id }
                    .toList()
                for (extension in extensions) {
                    if (temp.contains(extension.id)) {
                        tempDisabledExtensions.add(extension)
                    }
                }
                if (withNewTabUrl) {
                    refreshNewTabUrl()
                }
            }
        }
    }

    /**
     * 这个实时查找
     */
    fun findHomePageUrl(): String? {
        val homePageUrl = getHomePageUrl()
        if (!homePageUrl.isNullOrEmpty()) {
            return homePageUrl
        }
        return findNewTabUrl()
    }

    /**
     * 这个取自缓存，有可能不准确
     */
    fun getHomePageUrlOrNewTabUrl(): String? {
        var newtabUrl0 = getHomePageUrl()
        if (newtabUrl0.isNullOrEmpty()) {
            newtabUrl0 = PreferenceMgr2.getString(App.getContext(), "newtabUrl", null)
        }
        return newtabUrl0
    }

    private fun getHomePageUrl(): String? {
        val url = PreferenceMgr2.getString(App.getContext(), "home_page_url", null)
        if (url.isNullOrEmpty()) {
            return null
        }
        if (url.startsWith("/") && File(url).exists()) {
            return "file://$url"
        }
        if (!url.startsWith("http") && !url.startsWith("file://")) {
            return null
        }
        return url
    }

    fun findNewTabUrl(): String? {
        for (extension in extensions) {
            if (extension.metaData.enabled && newtabs.containsKey(extension.id)) {
                val u = newtabs[extension.id]
                if (u.isNullOrEmpty()) {
                    continue
                }
                //从121开始刚install的可能为空，需监听这个获得WebExtensionController.AddonManagerDelegate.onReady
                if (extension.metaData.baseUrl.isNullOrEmpty()) {
                    continue
                }
                return extension.metaData.baseUrl + u
            }
        }
        return null
    }

    private fun initNewTabRecord(initNewTabUrlSession: Boolean) {
        val exist = PreferenceMgr.getString(App.getContext(), "newtabs", "")
        val list = exist.split("$$$").filter { it.isNotEmpty() }.toMutableList()
        for (s in list) {
            val a = s.split("@@")
            if (a.size == 2) {
                newtabs[a[0]] = a[1]
            }
        }
        if (initNewTabUrlSession) {
            createNewTabUrlSession(findNewTabUrl())
        }
    }

    fun createNewTabUrlSession(newTabUrl: String?) {
        if (!newTabUrl.isNullOrEmpty()) {
            App.getHomeActivity()?.let {
                val list1 = DelegateListLiveData.getInstance().value ?: emptyList()
                for (sessionDelegate in list1) {
                    if (sessionDelegate.u == newTabUrl) {
                        if (DelegateLivedata.getInstance().value != sessionDelegate) {
                            DelegateLivedata.getInstance().Value(sessionDelegate)
                        }
                        return@let
                    }
                }
                createSession(newTabUrl, it)
            }
        }
    }

    fun addNewTabRecord(extension: WebExtension, url: String) {
        val exist = PreferenceMgr.getString(App.getContext(), "newtabs", "")
        val list = exist.split("$$$").filter { it.isNotEmpty() }.toMutableList()
        for (s in list.withIndex()) {
            if (s.value.startsWith(extension.id + "@@")) {
                list.removeAt(s.index)
                list.add(extension.id + "@@" + url)
                newtabs[extension.id] = url
                PreferenceMgr.put(App.getContext(), "newtabs", list.joinToString("$$$"))
                refreshNewTabUrl()
                return
            }
        }
        list.add(extension.id + "@@" + url)
        newtabs[extension.id] = url
        PreferenceMgr.put(App.getContext(), "newtabs", list.joinToString("$$$"))
        refreshNewTabUrl()
    }

    fun removeNewTabRecord(id: String) {
        val exist = PreferenceMgr.getString(App.getContext(), "newtabs", "")
        val list = exist.split("$$$").filter { it.isNotEmpty() }.toMutableList()
        for (s in list.withIndex()) {
            if (s.value.startsWith("$id@@")) {
                list.removeAt(s.index)
                newtabs.remove(id)
                PreferenceMgr.put(App.getContext(), "newtabs", list.joinToString("$$$"))
                refreshNewTabUrl()
                return
            }
        }
    }

    fun refreshNewTabUrl() {
        val newTabUrl = findNewTabUrl()
        if (!newTabUrl.isNullOrEmpty()) {
            PreferenceMgr2.put(App.getContext(), "newtabUrl", newTabUrl)
        } else {
            PreferenceMgr2.remove(App.getContext(), "newtabUrl")
        }
        EventBus.getDefault().post(NewTabUrlChangeEvent())
    }

    fun checkOrRefresh(extension: WebExtension) {
        for (webExtension in extensions) {
            if (webExtension.id == extension.id && webExtension.metaData.enabled == extension.metaData.enabled) {
                return
            }
        }
        refresh()
    }

    var tempDisabledExtensions: MutableList<WebExtension> = ArrayList()

    var blockDoms = CollectionUtil.asList(
        "youku.com",
        "iqiyi.com",
        ".qq.com",
        "mgtv.com",
        "acfun.",
        "tudou.com",
        ".sohu.com",
        //"baidu.com",
        "bilibili.com",
        ".ximalaya.",
        "douyin.com",
        "ixigua.com"
    )     //悬浮嗅探黑名单，包含blockDoms
    private val floatVideoBlockDoms = CollectionUtil.asList("bing.com", "bing.cn")
    private val blackExtensionKeys = CollectionUtil.asList("block", "adguard", "广告")


    fun isFloatVideoBlockDom(url0: String?): Boolean {
        var url = url0
        if (StringUtil.isEmpty(url)) {
            return false
        }
        url = StringUtil.getDom(url)
        for (blockDom in floatVideoBlockDoms) {
            if (url.contains(blockDom!!)) {
                return true
            }
        }
        return false
    }

    fun hasBlockDom(url0: String?): Boolean {
        if (url0.isNullOrEmpty()) {
            return false
        }
        val url = StringUtil.getDom(url0)
        for (blockDom in blockDoms) {
            if (url.contains(blockDom!!)) {
                return true
            }
        }
        return false
    }

    fun isTempExtension(extension: WebExtension): Boolean {
        for (disabledExtension in tempDisabledExtensions) {
            if (StringUtils.equals(extension.id, disabledExtension.id)) {
                return true
            }
        }
        return false
    }

    private fun lowerContains(word: String?, key: String): Boolean {
        return if (StringUtil.isEmpty(word) || StringUtil.isEmpty(key)) {
            false
        } else word!!.lowercase(Locale.getDefault()).contains(key)
    }

    private fun replaceWebExtension(old: WebExtension, newExtension: WebExtension) {
        val i = extensions.indexOf(old)
        if (i >= 0 && i < extensions.size) {
            extensions[i] = newExtension
        }
    }

    fun onReady(newExtension: WebExtension) {
        for (item in extensions.withIndex()) {
            if (item.value.id == newExtension.id) {
                val i = item.index
                if (i >= 0 && i < extensions.size) {
                    extensions[i] = newExtension
                }
                if (newtabs.containsKey(newExtension.id)) {
                    refreshNewTabUrl()
                }
                break
            }
        }
    }

    fun clearSessions(context: Context) {
        if (tempDisabledExtensions.isNotEmpty()) {
            for (extension in tempDisabledExtensions) {
                if (!extension.metaData.enabled) {
                    GeckoRuntime.getDefault(context).webExtensionController
                        .enable(extension, WebExtensionController.EnableSource.USER)
                        .accept({ ext: WebExtension? ->
                            if (ext != null) {
                                EventBus.getDefault()
                                    .post(WebExtensionsEnableEvent(extension, false))
                                EventBus.getDefault().post(WebExtensionsEnableEvent(ext, true))
                                replaceWebExtension(extension, ext)
                            }
                        }, { e: Throwable? ->
                            e?.printStackTrace()
                        })
                }
            }
            tempDisabledExtensions.clear()
        }
    }

    fun disableDangerousExtensions() {
        for (extension in extensions) {
            if (extension.metaData.enabled) {
                //在关键词黑名单的才禁用
                for (key in blackExtensionKeys) {
                    if (lowerContains(
                            extension.metaData.name,
                            key
                        ) || lowerContains(extension.metaData.description, key)
                    ) {
                        GeckoRuntime.getDefault(App.application!!).webExtensionController
                            .disable(extension, WebExtensionController.EnableSource.USER)
                            .accept({ ext: WebExtension? ->
                                if (ext != null) {
                                    EventBus.getDefault()
                                        .post(WebExtensionsEnableEvent(extension, false))
                                    EventBus.getDefault().post(WebExtensionsEnableEvent(ext, true))
                                    replaceWebExtension(extension, ext)
                                    if (!isTempExtension(ext)) {
                                        tempDisabledExtensions.add(ext)
                                    }
                                }
                            }, { e: Throwable? ->
                                e?.printStackTrace()
                            })
                        break
                    }
                }
            }
        }
        saveTempHistory()
    }

    private fun saveTempHistory() {
        val temp = Stream.of(tempDisabledExtensions).map { it: WebExtension -> it.id }
            .toList()
        if (CollectionUtil.isEmpty(temp)) {
            PreferenceMgr.remove(App.application, "tempDisabledExtensions")
        } else {
            val t = CollectionUtil.listToString(temp, "&&&")
            PreferenceMgr.put(App.application, "tempDisabledExtensions", t)
        }
    }

    fun enableTemp() {
        if (tempDisabledExtensions.isEmpty()) {
            return
        }
        val iterator: MutableIterator<WebExtension> = tempDisabledExtensions.iterator()
        while (iterator.hasNext()) {
            val extension = iterator.next()
            if (!extension.metaData.enabled) {
                GeckoRuntime.getDefault(App.application!!).webExtensionController
                    .enable(extension, WebExtensionController.EnableSource.USER)
                    .accept({ ext: WebExtension? ->
                        if (ext != null) {
                            EventBus.getDefault()
                                .post(WebExtensionsEnableEvent(extension, false))
                            EventBus.getDefault().post(WebExtensionsEnableEvent(ext, true))
                            replaceWebExtension(extension, ext)
                        }
                    }, { e: Throwable? ->
                        e?.printStackTrace()
                    })
            }
            iterator.remove()
        }
        PreferenceMgr.remove(App.application, "tempDisabledExtensions")
    }
}