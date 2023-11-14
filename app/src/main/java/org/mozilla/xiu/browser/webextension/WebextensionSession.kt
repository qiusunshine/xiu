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
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.session.GeckoViewModel
import org.mozilla.xiu.browser.utils.FileUtil
import org.mozilla.xiu.browser.utils.ThreadTool.async
import org.mozilla.xiu.browser.utils.ThreadTool.runOnUI
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.UriUtilsPro
import org.mozilla.xiu.browser.utils.ZipUtils
import java.io.File
import java.util.UUID


class WebextensionSession {
    var context: Activity
    private var webExtensionController: WebExtensionController
    private var webExtensions = ArrayList<WebExtension>()

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
        webExtensionController.list().accept {
            if (it != null) {
                for (i in it)
                    i.addDelegate(context)
            }
        }
    }

    private fun installCrx(uri: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle("温馨提示")
            .setMessage("即将安装crx扩展程序，注意因为本软件使用火狐Gecko内核，不能完全兼容谷歌crx格式，建议能安装原生xpi扩展的情况下优先安装xpi扩展，本软件只对crx提供有限的兼容，确定继续安装crx扩展吗？")
            .setPositiveButton("确定") { d, _ ->
                d.dismiss()
                async(Runnable {
                    try {
                        val name: String =
                            File(uri.replace("file://", "")).getName().replace(".crx", "")
                        val fileDirPath: String =
                            ((UriUtilsPro.getRootDir(context) + File.separator) + "_cache" + File.separator) + name
                        FileUtil.deleteDirs(fileDirPath)
                        File(fileDirPath).mkdirs()
                        ZipUtils.unzipFile(uri.replace("file://", ""), fileDirPath)
                        val m = File(fileDirPath + File.separator + "manifest.json")
                        if (m.exists()) {
                            val json: JSONObject =
                                JSON.parseObject(FileUtil.fileToString(m.getAbsolutePath()))
                            if (json.containsKey("options_page")) {
                                val optionsUi = JSONObject()
                                optionsUi.put("page", json.getString("options_page"))
                                optionsUi.put("open_in_tab", true)
                                //optionsUi.put("browser_style",  false);
                                json.put("options_ui", optionsUi)
                                json.remove("options_page")
                            }
                            if (json.containsKey("permissions")) {
                                val permissions: JSONArray = json.getJSONArray("permissions")
                                if (permissions.contains("webRequest") && !permissions.contains("webRequestBlocking")) {
                                    permissions.add(
                                        permissions.indexOf("webRequest") + 1,
                                        "webRequestBlocking"
                                    )
                                }
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
                            json.remove("update_url")
                            val gecko = JSONObject()
                            gecko.put("id", "{" + UUID.randomUUID().toString() + "}")
                            gecko.put("strict_min_version", "63.0")
                            val settings = JSONObject()
                            settings.put("gecko", gecko)
                            json.put("browser_specific_settings", settings)
                            json.remove("incognito")
                            json.remove("minimum_chrome_version")
                            FileUtil.stringToFile(
                                JSON.toJSONString(json, SerializerFeature.PrettyFormat),
                                m.getAbsolutePath()
                            )
                        }
                        File(fileDirPath + File.separator + "META-INF").mkdirs()
                        val xpi: String = uri.replace("file://", "").replace(".crx", ".xpi")
                        FileUtil.deleteFile(xpi)
                        ZipUtils.zipFile(fileDirPath, xpi)
                        if (!context.isFinishing) {
                            runOnUI { install("file://$xpi") }
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

    fun install(uri: String) {
        if (uri.startsWith("file://") && uri.endsWith(".crx")) {
            installCrx(uri)
            return
        }
        ToastMgr.shortBottomCenter(context, "稍等，解析文件中")
        webExtensionController.install(uri).accept({ it ->
            if (it != null) {
                Toast.makeText(context, it.metaData.name + "安装成功", Toast.LENGTH_LONG).show()
                EventBus.getDefault().post(WebExtensionsRefreshEvent())
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
                            EventBus.getDefault().post(BrowseEvent("about:config"))
                        }.setNegativeButton("取消") { d, _ ->
                            d.dismiss()
                        }.show()
                    return@accept
                }
            }
            Toast.makeText(context, "安装失败: $exception", Toast.LENGTH_LONG).show()
        })
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
            return GeckoResult.fromValue(session)
        }
    }
    this.setMessageDelegate(object : WebExtension.MessageDelegate {
        override fun onConnect(port: WebExtension.Port) {
            super.onConnect(port)
        }

        override fun onMessage(
            nativeApp: String,
            message: Any,
            sender: WebExtension.MessageSender
        ): GeckoResult<Any>? {
            //todo
            return super.onMessage(nativeApp, message, sender)
        }
    }, "browser")
}

fun WebExtension.removeDelegate() {
    this.tabDelegate = null
    this.setMessageDelegate(null, "browser")
}

fun newSession(session: GeckoSession, activity: Activity) {
    val geckoViewModel =
        activity?.let { ViewModelProvider(it as ViewModelStoreOwner)[GeckoViewModel::class.java] }!!
    geckoViewModel.changeSearch(session)
    HomeLivedata.getInstance().Value(false)
}