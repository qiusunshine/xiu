package org.mozilla.xiu.browser.webextension

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.serializer.SerializerFeature
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtension.InstallException
import org.mozilla.geckoview.WebExtensionController
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.session.DelegateLivedata
import org.mozilla.xiu.browser.session.GeckoViewModel
import org.mozilla.xiu.browser.session.SeRuSettings
import org.mozilla.xiu.browser.tab.DelegateListLiveData
import org.mozilla.xiu.browser.tab.RemoveTabLiveData
import org.mozilla.xiu.browser.utils.FileUtil
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.ThreadTool.async
import org.mozilla.xiu.browser.utils.ThreadTool.runOnUI
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.UriUtilsPro
import org.mozilla.xiu.browser.utils.ZipUtils
import rxhttp.toDownloadFlow
import rxhttp.wrapper.param.RxHttp
import java.io.File
import java.util.UUID


class WebextensionSession {
    var context: Activity
    private var webExtensionController: WebExtensionController

    constructor(context: Activity) {
        this.context = context
        webExtensionController = GeckoRuntime.getDefault(context).webExtensionController
        webExtensionController.promptDelegate = object : WebExtensionController.PromptDelegate {
            override fun onInstallPrompt(extension: WebExtension): GeckoResult<AllowOrDeny>? {
                val dlg = org.mozilla.xiu.browser.componets.PermissionDialog(context, extension)
                if (dlg.showDialog() === 1) {
                    extension.addDelegate(context)
                    return GeckoResult.allow()
                } else return GeckoResult.deny()
            }

            override fun onUpdatePrompt(
                currentlyInstalled: WebExtension,
                updatedExtension: WebExtension,
                newPermissions: Array<out String>,
                newOrigins: Array<out String>
            ): GeckoResult<AllowOrDeny>? {
                return GeckoResult.allow()
            }

            override fun onOptionalPrompt(
                extension: WebExtension,
                permissions: Array<out String>,
                origins: Array<out String>
            ): GeckoResult<AllowOrDeny>? {
                return GeckoResult.allow()
            }
        }
//        webExtensionController.setAddonManagerDelegate(object :
//            WebExtensionController.AddonManagerDelegate {
//            override fun onReady(extension: WebExtension) {
//                WebExtensionRuntimeManager.onReady(extension)
//            }
//        })
        webExtensionController.list().accept {
            if (it != null) {
                for (i in it)
                    i.addDelegate(context)
            }
        }
    }

    private fun installCrx(uri: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.notify))
            .setMessage("即将安装crx扩展程序，注意因为本软件使用火狐Gecko内核，不能完全兼容谷歌crx格式，建议能安装原生xpi扩展的情况下优先安装xpi扩展，本软件只对crx提供有限的兼容，确定继续安装crx扩展吗？")
            .setPositiveButton("确定") { d, _ ->
                d.dismiss()
                async(Runnable {
                    try {
                        val name: String =
                            File(uri.replace("file://", "")).name.replace(".crx", "")
                        val fileDirPath: String =
                            ((UriUtilsPro.getRootDir(context) + File.separator) + "_cache" + File.separator) + name
                        FileUtil.deleteDirs(fileDirPath)
                        File(fileDirPath).mkdirs()
                        ZipUtils.unzipFile(uri.replace("file://", ""), fileDirPath)
                        val m = File(fileDirPath + File.separator + "manifest.json")
                        if (m.exists()) {
                            val json: JSONObject =
                                JSON.parseObject(FileUtil.fileToString(m.absolutePath))
                            if (json.containsKey("options_page")) {
                                val optionsUi = JSONObject()
                                optionsUi.put("page", json.getString("options_page"))
                                optionsUi.put("open_in_tab", true)
                                //optionsUi.put("browser_style",  false);
                                json.put("options_ui", optionsUi)
                                json.remove("options_page")
                            }
                            if (!json.containsKey("permissions")) {
                                json["permissions"] = JSONArray()
                            }
                            val permissions: JSONArray = json.getJSONArray("permissions")
                            if (json.containsKey("optional_permissions")) {
                                val optionalPermissions: JSONArray =
                                    json.getJSONArray("optional_permissions")
                                for (optionalPermission in optionalPermissions) {
                                    if (!permissions.contains(optionalPermission)) {
                                        permissions.add(optionalPermission)
                                    }
                                }
                            }
                            if (permissions.contains("webRequest") && !permissions.contains("webRequestBlocking")) {
                                permissions.add(
                                    permissions.indexOf("webRequest") + 1,
                                    "webRequestBlocking"
                                )
                            }
                            if (json.containsKey("background")) {
                                val background: JSONObject = json.getJSONObject("background")
                                if (background.containsKey("service_worker")) {
                                    val scripts = JSONArray()
                                    scripts.add(background.getString("service_worker"))
                                    background.put("scripts", scripts)
                                    background.remove("service_worker")
                                }
                            }
                            //解决青柠起始页等扩展无权限的问题
                            if (json.containsKey("content_scripts")) {
                                val contentScripts: JSONArray? =
                                    json.getJSONArray("content_scripts")
                                if (contentScripts != null) {
                                    for (index in 0 until contentScripts.size) {
                                        val script = contentScripts.getJSONObject(index)
                                        if (script?.containsKey("matches") == true) {
                                            if (!permissions.contains("<all_urls>")) {
                                                permissions.add("<all_urls>")
                                            }
                                            break
                                        }
                                    }
                                }
                            }
                            json.remove("update_url")
                            val gecko = JSONObject()
                            var id = "{" + UUID.randomUUID().toString() + "}"
                            var name1 = json.getString("name")
                            if (name1?.startsWith("__MSG_") == true) {
                                val key = name1.replace("__MSG_", "").replace("__", "")
                                var f =
                                    File(fileDirPath + File.separator + "_locales" + File.separator + "zh_CN" + File.separator + "messages.json")
                                if (!f.exists()) {
                                    f =
                                        File(fileDirPath + File.separator + "_locales" + File.separator + "zh_TW" + File.separator + "messages.json")
                                }
                                if (!f.exists()) {
                                    f =
                                        File(fileDirPath + File.separator + "_locales" + File.separator + "en" + File.separator + "messages.json")
                                }
                                if (f.exists()) {
                                    val messages: JSONObject? =
                                        JSON.parseObject(FileUtil.fileToString(f.absolutePath))
                                    if (messages != null && messages.containsKey(key)) {
                                        val msg = messages.getJSONObject(key)
                                        val b = msg?.getString("message")
                                        if (!b.isNullOrEmpty()) {
                                            name1 = b
                                        }
                                    }
                                }
                            }
                            for (extension in WebExtensionRuntimeManager.extensions) {
                                if (extension.metaData.name == name1) {
                                    id = if (extension.id.contains("@")) {
                                        extension.id
                                    } else if (extension.id.contains("{")) {
                                        extension.id
                                    } else {
                                        "{" + extension.id + "}"
                                    }
                                    break
                                }
                            }
                            gecko["id"] = id
                            gecko["strict_min_version"] = "63.0"
                            val settings = JSONObject()
                            settings["gecko"] = gecko
                            json["browser_specific_settings"] = settings
                            json.remove("incognito")
                            json.remove("minimum_chrome_version")
                            FileUtil.stringToFile(
                                JSON.toJSONString(json, SerializerFeature.PrettyFormat),
                                m.absolutePath
                            )
                        }
                        File(fileDirPath + File.separator + "META-INF").mkdirs()
                        val xpi: String = uri.replace("file://", "").replace(".crx", ".xpi")
                        FileUtil.deleteFile(xpi)
                        ZipUtils.zipFile(fileDirPath, xpi)
                        if (!context.isFinishing) {
                            runOnUI { install0("file://$xpi") }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ToastMgr.shortBottomCenter(context, "出错：" + e.message)
                    }
                })
            }.setNegativeButton("取消") { d, _ ->
                d.dismiss()
            }.show()
    }

    private fun installXpi(uri: String) {
        async(Runnable {
            try {
                val name: String =
                    File(uri.replace("file://", "")).name.replace(".xpi", "")
                val fileDirPath: String =
                    ((UriUtilsPro.getRootDir(context) + File.separator) + "_cache" + File.separator) + name
                FileUtil.deleteDirs(fileDirPath)
                File(fileDirPath).mkdirs()
                ZipUtils.unzipFile(uri.replace("file://", ""), fileDirPath)
                val m = File(fileDirPath + File.separator + "manifest.json")
                var converted = false
                if (m.exists()) {
                    val json: JSONObject =
                        JSON.parseObject(FileUtil.fileToString(m.absolutePath))
                    if (!json.containsKey("permissions")) {
                        json["permissions"] = JSONArray()
                        converted = true
                    }
                    val permissions: JSONArray = json.getJSONArray("permissions")
                    //解决青柠起始页等扩展无权限的问题
                    if (json.containsKey("content_scripts")) {
                        val contentScripts: JSONArray? =
                            json.getJSONArray("content_scripts")
                        if (contentScripts != null) {
                            for (index in 0 until contentScripts.size) {
                                val script = contentScripts.getJSONObject(index)
                                if (script?.containsKey("matches") == true) {
                                    if (!permissions.contains("<all_urls>")) {
                                        permissions.add("<all_urls>")
                                        converted = true
                                    }
                                    break
                                }
                            }
                        }
                    }
                    if (!converted) {
                        runOnUI { install0(uri) }
                        return@Runnable
                    }
                    FileUtil.stringToFile(
                        JSON.toJSONString(json, SerializerFeature.PrettyFormat),
                        m.absolutePath
                    )
                } else {
                    runOnUI { install0(uri) }
                    return@Runnable
                }
                File(fileDirPath + File.separator + "META-INF").mkdirs()
                val xpi: String = uri.replace("file://", "").replace(".xpi", "_convert.xpi")
                FileUtil.deleteFile(xpi)
                ZipUtils.zipFile(fileDirPath, xpi)
                if (!context.isFinishing) {
                    runOnUI { install0("file://$xpi") }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ToastMgr.shortBottomCenter(context, "出错：" + e.message)
            }
        })
    }

    fun install(uri: String) {
        if (uri.startsWith("file://") && uri.endsWith(".crx")) {
            installCrx(uri)
            return
        }
        if (uri.startsWith("file://") && uri.endsWith(".xpi")) {
            installXpi(uri)
            return
        }
        install0(uri)
    }

    private fun install0(uri: String) {
        ToastMgr.shortBottomCenter(context, "稍等，解析文件中")
        webExtensionController.install(uri).accept({ it ->
            if (it != null) {
                loadNewTabInfo(it, uri)
                Toast.makeText(context, it.metaData.name + "安装成功", Toast.LENGTH_LONG).show()
                EventBus.getDefault().post(WebExtensionsRefreshEvent())
                WebExtensionRuntimeManager.refresh()
            } else {
                Toast.makeText(context, "安装失败", Toast.LENGTH_LONG).show()
            }
        }, { exception: Throwable? ->
            exception?.printStackTrace()
            if (exception is InstallException) {
                if (exception.code == InstallException.ErrorCodes.ERROR_SIGNEDSTATE_REQUIRED) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("安装失败")
                        .setMessage(
                            "检测到您安装的扩展程序没有正确的签名，点击下方确定按钮打开about:config页面，" +
                                    "请搜索sign滑动到最后将xpinstall.signatures.required切换为false，然后再重新安装"
                        )
                        .setPositiveButton("确定") { d, _ ->
                            d.dismiss()
                            EventBus.getDefault().post(BrowseEvent("about:config"))
                        }.setNegativeButton("取消") { d, _ ->
                            d.dismiss()
                        }.show()
                    return@accept
                } else if (exception.code == InstallException.ErrorCodes.ERROR_CORRUPT_FILE) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("安装失败")
                        .setMessage("文件无法识别，请确认是xpi或crx扩展程序安装包文件")
                        .setPositiveButton("确定") { d, _ ->
                            d.dismiss()
                        }.setNegativeButton("取消") { d, _ ->
                            d.dismiss()
                        }.show()
                    return@accept
                }
            }
            Toast.makeText(context, "安装失败: $exception", Toast.LENGTH_LONG).show()
        })
    }

    private fun loadNewTabInfo(it: WebExtension, uri: String) {
        val tabKeyWords = arrayOf("tab", "标签", "主页", "起始")
        var hasKey = false
        for (word in tabKeyWords) {
            if (it.metaData.name?.lowercase()?.contains(word) == true) {
                hasKey = true
                break
            }
        }
        if (!hasKey) {
            return
        }
        async {
            try {
                if (uri.startsWith("file://")) {
                    loadNewTabInfo0(it, uri.replace("file://", ""))
                } else {
                    val path =
                        UriUtilsPro.getRootDir(App.getContext()) + File.separator + "_cache" + File.separator + it.metaData.name + ".zip"
                    FileUtil.makeSureDirExist(path)
                    val t = RxHttp.get(uri)
                        .tag(uri)
                    t.toDownloadFlow(path).collect { p ->
                        if (File(p).exists()) {
                            loadNewTabInfo0(it, p)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadNewTabInfo0(extension: WebExtension, path: String) {
        async {
            try {
                val file = File(path)
                if (file.exists()) {
                    val fileDirPath: String =
                        ((UriUtilsPro.getRootDir(context) + File.separator) + "_cache" + File.separator) + file.name.replace(
                            ".zip",
                            ""
                        ).replace(
                            ".xpi",
                            ""
                        ).replace(
                            ".crx",
                            ""
                        )
                    FileUtil.deleteDirs(fileDirPath)
                    File(fileDirPath).mkdirs()
                    ZipUtils.unzipFile(path.replace("file://", ""), fileDirPath)
                    val m = File(fileDirPath + File.separator + "manifest.json")
                    if (m.exists()) {
                        val json: JSONObject =
                            JSON.parseObject(FileUtil.fileToString(m.absolutePath))
                        if (json.containsKey("chrome_url_overrides")) {
                            val a = json["chrome_url_overrides"] as JSONObject
                            if (a.containsKey("newtab")) {
                                val newTab = a.getString("newtab")
                                if (!newTab.isNullOrEmpty()) {
                                    runOnUI {
                                        WebExtensionRuntimeManager.addNewTabRecord(
                                            extension,
                                            newTab
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun WebExtension.addDelegate(context: Activity) {
    this.tabDelegate = object : WebExtension.TabDelegate {
        override fun onNewTab(
            source: WebExtension,
            createDetails: WebExtension.CreateTabDetails
        ): GeckoResult<GeckoSession>? {
            val session = GeckoSession()
            //Log.d("onNewTab",session.)
            newSession(session, context)
            if (createDetails.active == true) {
                EventBus.getDefault().post(WebExtensionAddTabEvent(source.id))
            }
            return GeckoResult.fromValue(session)
        }
    }
    val id = this.id
    this.setMessageDelegate(object : WebExtension.MessageDelegate {
        override fun onConnect(port: WebExtension.Port) {
            super.onConnect(port)
            if (id == "xiutan@xiu.com") {
                EventBus.getDefault().post(WebExtensionConnectPortEvent(port))
                port.setDelegate(object : WebExtension.PortDelegate {
                    override fun onDisconnect(port: WebExtension.Port) {
                        super.onDisconnect(port)
                        EventBus.getDefault().post(WebExtensionConnectPortEvent(null))
                    }
                })
            }
        }

        override fun onMessage(
            nativeApp: String,
            message: Any,
            sender: WebExtension.MessageSender
        ): GeckoResult<Any>? {
            if (message is org.json.JSONObject) {
                val json = message
                if ("xiu" == json.optString("type")) {
                    EventBus.getDefault().post(TabRequestEvent(json))
                }
            }
            return super.onMessage(nativeApp, message, sender)
        }
    }, "browser")
    val sessionDelegates = DelegateListLiveData.getInstance().value ?: arrayListOf()
    for (sessionDelegate in sessionDelegates) {
        addSessionTabDelegate(context, sessionDelegate.session, arrayListOf(this))
    }
}

fun WebExtension.removeDelegate() {
    this.tabDelegate = null
    this.setMessageDelegate(null, "browser")
}

fun newSession(session: GeckoSession, activity: Activity) {
    val sessionSettings = session.settings
    SeRuSettings(sessionSettings, activity)
    val geckoViewModel =
        activity?.let { ViewModelProvider(it as ViewModelStoreOwner)[GeckoViewModel::class.java] }!!
    geckoViewModel.changeSearch(session)
    HomeLivedata.getInstance().Value(false)
}

fun addSessionTabDelegate(
    context: Activity,
    session: GeckoSession,
    extensions: List<WebExtension>
) {
    for (extension in extensions) {
        session.webExtensionController.setTabDelegate(
            extension,
            object : WebExtension.SessionTabDelegate {
                override fun onCloseTab(
                    source: WebExtension?,
                    session: GeckoSession
                ): GeckoResult<AllowOrDeny> {
                    val list = DelegateListLiveData.getInstance().value ?: arrayListOf()
                    for (indexedValue in list.withIndex()) {
                        if (indexedValue.value.session == session) {
                            RemoveTabLiveData.getInstance().Value(indexedValue.index)
                            break
                        }
                    }
                    return GeckoResult.allow()
                }

                override fun onUpdateTab(
                    extension: WebExtension,
                    session: GeckoSession,
                    details: WebExtension.UpdateTabDetails
                ): GeckoResult<AllowOrDeny> {
                    val delegates =
                        DelegateListLiveData.getInstance().value ?: arrayListOf()
                    for (delegate in delegates) {
                        if (delegate.session == session) {
                            if (!session.isOpen) {
                                // Session's process was previously killed; reopen
                                session.open(GeckoRuntime.getDefault(context))
                                val u = if (StringUtil.isNotEmpty(details.url)) {
                                    details.url
                                } else {
                                    delegate.u
                                }
                                if (!u.isNullOrEmpty()) {
                                    session.loadUri(u)
                                }
                            } else {
                                if (StringUtil.isNotEmpty(details.url) && details.url != delegate.u) {
                                    session.loadUri(details.url!!)
                                }
                            }
                            if (details.active == true && DelegateLivedata.getInstance().value?.session != session) {
                                DelegateLivedata.getInstance().Value(delegate)
                            }
                            if (details.active == true) {
                                EventBus.getDefault().post(WebExtensionAddTabEvent(extension.id))
                            }
                            break
                        }
                    }
                    return GeckoResult.allow()
                }
            })
    }
}

data class WebExtensionAddTabEvent(val id: String)