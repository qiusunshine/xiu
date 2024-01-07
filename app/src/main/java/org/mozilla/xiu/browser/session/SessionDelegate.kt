package org.mozilla.xiu.browser.session

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alibaba.fastjson.JSON
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.interfaces.OnBindView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import org.mozilla.gecko.InputMethods
import org.mozilla.gecko.InputMethods.getInputMethodManager
import org.mozilla.gecko.util.ThreadUtils
import org.mozilla.geckoview.*
import org.mozilla.geckoview.GeckoSession.HistoryDelegate
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSession.ProgressDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate.*
import org.mozilla.geckoview.GeckoSession.TextInputDelegate
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.BR
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.base.VarHolder
import org.mozilla.xiu.browser.base.async
import org.mozilla.xiu.browser.componets.ContextMenuDialog
import org.mozilla.xiu.browser.componets.popup.IntentPopup
import org.mozilla.xiu.browser.database.history.History
import org.mozilla.xiu.browser.database.history.HistoryViewModel
import org.mozilla.xiu.browser.download.DownloadTask
import org.mozilla.xiu.browser.download.downloadBlob
import org.mozilla.xiu.browser.download.getUri
import org.mozilla.xiu.browser.download.openUriBeforePop
import org.mozilla.xiu.browser.download.startDownload
import org.mozilla.xiu.browser.fxa.Fxa.Companion.historySync
import org.mozilla.xiu.browser.tab.RemoveTabLiveData
import org.mozilla.xiu.browser.utils.FilesInAppUtil
import org.mozilla.xiu.browser.utils.PreferenceMgr
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.UriUtils
import org.mozilla.xiu.browser.utils.copyToDownloadDir
import org.mozilla.xiu.browser.utils.filePicker.FilePicker
import org.mozilla.xiu.browser.utils.filePicker.getFileFromPicker
import org.mozilla.xiu.browser.webextension.Detector
import org.mozilla.xiu.browser.webextension.DetectorListener
import org.mozilla.xiu.browser.webextension.EvalJSEvent
import org.mozilla.xiu.browser.webextension.InputHeightListenPostEvent
import org.mozilla.xiu.browser.webextension.TabRequest
import org.mozilla.xiu.browser.webextension.WebExtensionRuntimeManager
import org.mozilla.xiu.browser.webextension.WebextensionSession
import org.mozilla.xiu.browser.webextension.addSessionTabDelegate
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class SessionDelegate() : BaseObservable() {


    lateinit var session: GeckoSession
    private lateinit var mContext: FragmentActivity
    lateinit var login: Login
    lateinit var setpic: Setpic
    lateinit var pageError: PageError
    val CONFIG_URL = "https://accounts.firefox.com"

    @get:Bindable
    var u: String = ""

    @get:Bindable
    var icon: String = ""

    @get:Bindable
    lateinit var bitmap: Bitmap

    @get:Bindable
    var mTitle: String = ""

    @get:Bindable
    var active: Boolean = false
        set(value) {
            field = value
            // 只刷新当前属性
            notifyPropertyChanged(BR.active)
        }

    @get:Bindable
    val privacy: Boolean
        get() = PrivacyModeLivedata.getInstance().value ?: false

    @get:Bindable
    var mProgress: Int = 0

    @get:Bindable
    var canBack: Boolean = false

    @get:Bindable
    var canForward: Boolean = false

    @get:Bindable
    var isFull: Boolean = false

    @get:Bindable
    var isSecure: Boolean = false

    @get:Bindable
    var secureHost: String = ""

    var oldY: Int = 0

    @get:Bindable
    var y: Int = 0

    var downloadTasks = ArrayList<DownloadTask>()
    lateinit var historyViewModel: HistoryViewModel
    lateinit var intentPopup: IntentPopup
    private lateinit var filePicker: FilePicker
    private var sessionState: GeckoSession.SessionState? = null
    private lateinit var fullscreenCall: (full: Boolean) -> Unit
    private lateinit var onCrashCall: () -> Unit

    var statusBarColor: Int = 0xffffff
    var navigationBarColor: Int = 0xffffff
    private lateinit var onPageStopCall: (session: GeckoSession, sessionDelegate: SessionDelegate) -> Unit
    private lateinit var onUrlChange: (url: String, sessionDelegate: SessionDelegate) -> Unit

    val requests: ArrayList<TabRequest> = ArrayList()
    private val detector: Detector = Detector()

    var activeMediaSession: MediaSession? = null

    var mExpectedTranslate = false
    var mTranslateRestore = false
    var mDetectedLanguage: String? = null

    constructor(
        mContext: FragmentActivity,
        session: GeckoSession,
        filePicker: FilePicker,
        fullscreenCall: (full: Boolean) -> Unit,
        onPageStopCall1: (session: GeckoSession, sessionDelegate: SessionDelegate) -> Unit = { se, de -> },
        onUrlChange: (url: String, sessionDelegate: SessionDelegate) -> Unit = { u, se -> },
        onCrashCall: () -> Unit = {},
        detectorListener: DetectorListener? = null
    ) : this() {
        this.mContext = mContext
        statusBarColor = getDefaultThemeColor(mContext)
        navigationBarColor = getDefaultThemeColor(mContext)
        this.session = session
        this.filePicker = filePicker
        this.fullscreenCall = fullscreenCall
        this.onPageStopCall = onPageStopCall1
        this.onUrlChange = onUrlChange
        this.onCrashCall = onCrashCall
        detector.detectorListener = detectorListener
        notifyPropertyChanged(BR.privacy)


        val geckoViewModel: GeckoViewModel =
            ViewModelProvider(mContext).get(GeckoViewModel::class.java)
        historyViewModel = ViewModelProvider(mContext).get(HistoryViewModel::class.java)
        bitmap = mContext.getDrawable(R.drawable.logo72)?.toBitmap()!!
        intentPopup = IntentPopup(mContext)
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onContextMenu(
                session: GeckoSession,
                screenX: Int,
                screenY: Int,
                element: GeckoSession.ContentDelegate.ContextElement
            ) {
                super.onContextMenu(session, screenX, screenY, element)
                ContextMenuDialog(mContext, element) {
                    val h = findResponseHeaders(it) ?: hashMapOf()
                    val name = UriUtils.getFileName(it, h)
                    addDownloadTask(name, it)
                    ToastMgr.shortBottomCenter(mContext, "已加入下载")
                }.open()
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                var uri = response.uri
                Log.d("test", "onExternalResponse: $uri")
                val name = UriUtils.getFileName(response)
                if (uri.startsWith("blob:")) {
                    if (name.endsWith("xpi")) {
                        downloadBlob(mContext, name, response) { path, e ->
                            withContext(Dispatchers.Main) {
                                if (e != null) {
                                    ToastMgr.shortBottomCenter(mContext, "出错：" + e.message)
                                } else {
                                    WebextensionSession(mContext).install("file://$path")
                                }
                            }
                        }
                    } else {
                        val holder = VarHolder(false)
                        MaterialAlertDialogBuilder(mContext)
                            .setTitle(mContext.getString(R.string.notify))
                            .setMessage(mContext.getString(R.string.download_request))
                            .setNegativeButton(mContext.getString(R.string.cancel)) { _, _ -> }
                            .setPositiveButton(mContext.getString(R.string.confirm)) { dialog, which ->
                                holder.data = true
                            }
                            .setOnDismissListener {
                                if (holder.data) {
                                    downloadBlob(mContext, name, response) { path, e ->
                                        if (e != null) {
                                            ToastMgr.shortBottomCenter(
                                                mContext,
                                                "出错：" + e.message
                                            )
                                        } else {
                                            val o = copyToDownloadDir(mContext, path)
                                            if (o != null) {
                                                val na = File(o).name
                                                getUri(mContext, na)?.let { u ->
                                                    withContext(Dispatchers.Main) {
                                                        openUriBeforePop(mContext, u)
                                                    }
                                                }
                                            } else {
                                                ToastMgr.shortBottomCenter(
                                                    mContext,
                                                    "下载出错，拷贝到下载目录失败"
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    //没有下载，关闭response
                                    try {
                                        response.body?.close()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            .show()
                    }
                    return
                }
                //没有下载，关闭response
                try {
                    response.body?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (uri.endsWith("xpi") && (!uri.contains("xiaoshu-") && !uri.contains("floccus"))) {
                    WebextensionSession(mContext).install(uri)
                    if (u == "about:blank" && mTitle == "" && !canBack && !canForward) {
                        RemoveTabLiveData.getInstance().Value(this@SessionDelegate)
                    }
                } else {
                    PopTip.build()
                        .setCustomView(object :
                            OnBindView<PopTip?>(R.layout.pop_mytip) {
                            override fun onBind(dialog: PopTip?, v: View) {
                                v.findViewById<TextView>(R.id.textView17).text =
                                    mContext.getString(R.string.download_request)
                                v.findViewById<MaterialButton>(R.id.materialButton7)
                                    .setOnClickListener {
                                        addDownloadTask(name, uri)
                                        dialog?.dismiss()
                                        if (u == "about:blank" && mTitle == "" && !canBack && !canForward) {
                                            RemoveTabLiveData.getInstance()
                                                .Value(this@SessionDelegate)
                                        }
                                    }
                                v.findViewById<MaterialButton>(R.id.btnCancel)
                                    .setOnClickListener {
                                        dialog?.dismiss()
                                    }
                            }
                        })
                        .showLong()
                }
                Log.d("ExternalResponse", uri)


            }

            override fun onPaintStatusReset(session: GeckoSession) {
                // setpic.onSetPic()
                notifyPropertyChanged(BR.bitmap)
            }

            override fun onCrash(session: GeckoSession) {
                super.onCrash(session)
                Log.d("test", "onCrash")
                if (sessionState != null) {
                    onCrashCall()
                }
                if (!session.isOpen) {
                    session.open(GeckoRuntime.getDefault(mContext))
                }
                sessionState?.let {
                    session.restoreState(it)
                }
                ThreadTool.postUIDelayed(100) {
                    pageFinish(session)
                }
            }

            override fun onKill(session: GeckoSession) {
                super.onKill(session)
                Log.d("test", "onCrash kill")
                if (sessionState != null) {
                    onCrashCall()
                }
                if (!session.isOpen) {
                    session.open(GeckoRuntime.getDefault(mContext))
                }
                sessionState?.let {
                    session.restoreState(it)
                }
                ThreadTool.postUIDelayed(100) {
                    pageFinish(session)
                }
            }

            override fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {
                super.onWebAppManifest(session, manifest)
                try {
                    val array = manifest.optJSONArray("icons")
                    if (array != null) {
                        val iconObj = array.getJSONObject(array.length() - 1)
                        icon = iconObj.optString("src")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFirstComposite(session: GeckoSession) {
                setpic.onSetPic()
                notifyPropertyChanged(BR.bitmap)
            }

            override fun onTitleChange(session: GeckoSession, title: String?) {
                if (title != null) {
                    mTitle = title
                }
                addHistory()
                notifyPropertyChanged(BR.mTitle)
                historySync?.sync(mTitle, u)
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                super.onFullScreen(session, fullScreen)
                isFull = fullScreen
                fullscreenCall(fullScreen)
                notifyPropertyChanged(BR.full)
            }
        }
        session.mediaSessionDelegate = object : MediaSession.Delegate {
            override fun onFullscreen(
                session: GeckoSession,
                mediaSession: MediaSession,
                enabled: Boolean,
                meta: MediaSession.ElementMetadata?
            ) {
                if (!enabled) {
                    mContext.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                    return
                }
                if (meta == null) {
                    return
                }
                if (meta.width > meta.height) {
                    mContext.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    mContext.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
                super.onActivated(session, mediaSession)
                activeMediaSession = mediaSession
            }

            override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
                super.onDeactivated(session, mediaSession)
                activeMediaSession = null
            }
        }

        session.progressDelegate = object : ProgressDelegate {
            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: ProgressDelegate.SecurityInformation
            ) {
                super.onSecurityChange(session, securityInfo)
                isSecure = securityInfo.isSecure
                secureHost = securityInfo.host + ""
                notifyPropertyChanged(BR.secure)
                notifyPropertyChanged(BR.secureHost)

            }

            override fun onSessionStateChange(
                session: GeckoSession,
                sessionState: GeckoSession.SessionState
            ) {
                super.onSessionStateChange(session, sessionState)
                this@SessionDelegate.sessionState = sessionState
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                super.onProgressChange(session, progress)
                //Log.d("test", "onProgressChange: $progress")
                EventBus.getDefault().post(ProgressEvent(progress))
                if (progress != 100)
                    mProgress = progress
                else mProgress = 0


                notifyPropertyChanged(BR.mProgress)
            }

            override fun onPageStart(session: GeckoSession, url: String) {
                if (url.startsWith("$CONFIG_URL/oauth/success/3c49430b43dfba77")) {
                    val uri = Uri.parse(url)
                    val code = uri.getQueryParameter("code")
                    val state = uri.getQueryParameter("state")
                    val action = uri.getQueryParameter("action")
                    if (code != null && state != null && action != null) {
                        //listener?.onLoginComplete(code, state, mContext)
                        //Toast.makeText(mContext,code+"**"+state,Toast.LENGTH_SHORT).show()
                        login.onLogin(code, state, action)
                    }
                }
                mTranslateRestore = false
                mExpectedTranslate = false
                notifyPropertyChanged(BR.y)
                requests.clear()
                try {
                    if (softInputShowing) {
                        val view = session.textInput.view
                        val imm = getInputMethodManager(mContext)
                        imm?.hideSoftInputFromWindow(view!!.windowToken, 0)
                        mContext.window
                            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                        softInputShowing = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                pageFinish(session)
                if (u.startsWith("https://microsoftedge.microsoft.com/addons")) {
                    val js = FilesInAppUtil.getAssetsString(mContext, "edge.js")
                    EventBus.getDefault().post(EvalJSEvent(js))
                }
            }
        }
        session.historyDelegate = object : HistoryDelegate {

        }

        session.navigationDelegate = object : NavigationDelegate {
            override fun onSubframeLoadRequest(
                session: GeckoSession,
                request: NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val uri = Uri.parse(request.uri)
                val url = request.uri
                var intent: Intent? = null;
                if (uri.scheme != null) {
                    if (!uri.scheme!!.contains("https") && !uri.scheme!!.contains("http") && !uri.scheme!!.contains(
                            "about"
                        )
                    ) {
                        if (url.startsWith("android-app://")) {
                            intent = Intent.parseUri(url, Intent.URI_ANDROID_APP_SCHEME);
                        } else if (url.startsWith("intent://")) {
                            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        } else {
                            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        }
                        if (intent != null) {
                            if (intent.resolveActivity(mContext.packageManager) != null) {
                                val result = GeckoResult<AllowOrDeny>()
                                EventBus.getDefault().post(OpenAppIntentEvent(intent, result))
                                return result
                            }
                        }
                    }
                    Log.d("scheme2", uri.scheme!!)
                }
                return GeckoResult.allow()
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val uri = Uri.parse(request.uri)
                val url = request.uri
                if (uri.scheme != null) {
                    if (!uri.scheme!!.contains("https") && !uri.scheme!!.contains("http") && !uri.scheme!!.contains(
                            "about"
                        )
                    ) {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (intent.resolveActivity(mContext.packageManager) != null) {
                            val result = GeckoResult<AllowOrDeny>()
                            EventBus.getDefault().post(OpenAppIntentEvent(intent, result))
                            return result
                        }
                    }
                }
                return GeckoResult.allow()
            }

            override fun onLoadError(
                session: GeckoSession,
                uri: String?,
                error: WebRequestError
            ): GeckoResult<String>? {
                return pageError.onPageError(session, uri, error)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                super.onCanGoForward(session, canGoForward)
                canForward = canGoForward
                notifyPropertyChanged(BR.canForward)
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                super.onCanGoBack(session, canGoBack)
                canBack = canGoBack
                notifyPropertyChanged(BR.canBack)
            }

            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>
            ) {
                if (!url.isNullOrEmpty() && "about:blank" != url) {
                    if (WebExtensionRuntimeManager.hasBlockDom(url)) {
                        WebExtensionRuntimeManager.disableDangerousExtensions()
                    } else {
                        WebExtensionRuntimeManager.enableTemp()
                    }
                }
                super.onLocationChange(session, url, perms)
                if (url != null) {
                    Log.d("可以？", url)
                }
                if (url != null) {
                    u = url
                }
                pageError.onPageChange()
                notifyPropertyChanged(org.mozilla.xiu.browser.BR.u)
                onUrlChange(u, this@SessionDelegate)
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession>? {
                try {
                    if (uri.startsWith("moz-extension://")) {
                        val u = DelegateLivedata.getInstance().value?.u
                        if (u?.startsWith("moz-extension://") == true) {
                            if (Uri.parse(u).host == Uri.parse(uri).host) {
                                //扩展程序打开新窗口，不能直接返回，否则会空白
                                createSession(uri, mContext)
                                return null
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val newSession = GeckoSession()
                val sessionSettings = newSession.settings
                SeRuSettings(sessionSettings, mContext)
                geckoViewModel.changeSearch(newSession)
                return GeckoResult.fromValue(newSession)
            }
        }

        /*
        session.setAutofillDelegate(new Autofill.Delegate() {
            @Override
            public void onAutofill(@NonNull GeckoSession session, int notification, @Nullable Autofill.Node node) {
                AutofillManager afm = context.getSystemService(AutofillManager.class);
                if(afm!=null){

                }
            }
        });*/

        session.promptDelegate = object : GeckoSession.PromptDelegate {
            override fun onAddressSave(
                session: GeckoSession,
                request: AutocompleteRequest<Autocomplete.AddressSaveOption>
            ): GeckoResult<PromptResponse>? {
                Log.d("BeforeUnload", "its me")
                return null
            }

            override fun onSharePrompt(
                session: GeckoSession,
                prompt: SharePrompt
            ): GeckoResult<PromptResponse>? {
                Log.d("onSharePrompt", "its me")

                return null
            }

            override fun onBeforeUnloadPrompt(
                session: GeckoSession,
                prompt: BeforeUnloadPrompt
            ): GeckoResult<PromptResponse>? {
                val result = GeckoResult<PromptResponse>()
                MaterialAlertDialogBuilder(mContext)
                    .setTitle(
                        ContextCompat.getString(
                            mContext,
                            R.string.before_unload_prompt_title
                        )
                    )
                    .setMessage(
                        ContextCompat.getString(
                            mContext,
                            R.string.before_unload_prompt_message
                        )
                    )
                    .setPositiveButton("确定") { d, _ ->
                        d.dismiss()
                        result.complete(prompt.confirm(AllowOrDeny.ALLOW))
                    }.setNegativeButton("取消") { d, _ ->
                        d.dismiss()
                        result.complete(prompt.confirm(AllowOrDeny.DENY))
                    }.show()
                return result
            }

            override fun onButtonPrompt(
                session: GeckoSession,
                prompt: ButtonPrompt
            ): GeckoResult<PromptResponse>? {
                val result = GeckoResult<PromptResponse>()
                val buttonDialog = org.mozilla.xiu.browser.broswer.dialog.ButtonDialog(
                    mContext,
                    prompt,
                    result
                )
                buttonDialog.show()
                Log.d("ButtonPrompt", "its me")

                return result
            }

            override fun onPopupPrompt(
                session: GeckoSession,
                prompt: PopupPrompt
            ): GeckoResult<PromptResponse>? {
                Log.d("Popup", "its me")

                return null
            }

            override fun onAuthPrompt(
                session: GeckoSession,
                prompt: AuthPrompt
            ): GeckoResult<PromptResponse>? {
                Log.d("AuthPrompt", "its me")

                return null
            }

            override fun onTextPrompt(
                session: GeckoSession,
                prompt: TextPrompt
            ): GeckoResult<PromptResponse>? {
                val result = GeckoResult<PromptResponse>()
                val alertDialog =
                    org.mozilla.xiu.browser.broswer.dialog.TextDialog(mContext, prompt, result)
                alertDialog.show()
                Log.d("TextPrompt", "its me")
                return result
            }

            override fun onRepostConfirmPrompt(
                session: GeckoSession,
                prompt: RepostConfirmPrompt
            ): GeckoResult<PromptResponse>? {
                val result = GeckoResult<PromptResponse>()
                //prompt.
                val confirmPrompt =
                    org.mozilla.xiu.browser.broswer.dialog.ConfirmDialog(
                        mContext,
                        prompt,
                        result
                    )
                confirmPrompt.show()
                return result
            }

            override fun onFilePrompt(
                session: GeckoSession,
                prompt: FilePrompt
            ): GeckoResult<PromptResponse>? {
                val result = GeckoResult<PromptResponse>()
                XXPermissions.with(mContext)
                    .permission(Permission.READ_MEDIA_IMAGES)
                    .permission(Permission.READ_MEDIA_AUDIO)
                    .permission(Permission.READ_MEDIA_VIDEO)
                    .permission(Permission.WRITE_EXTERNAL_STORAGE)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(
                            permissions: MutableList<String>,
                            allGranted: Boolean
                        ) {
                            getFileFromPicker(mContext, filePicker, prompt, result)
                        }

                        override fun onDenied(
                            permissions: MutableList<String>,
                            doNotAskAgain: Boolean
                        ) {
                            try {
                                result.cancel()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (doNotAskAgain) {
                                ToastMgr.shortBottomCenter(
                                    mContext,
                                    "被永久拒绝授权，请手动授予存储权限"
                                )
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(mContext, permissions)
                            } else {
                                ToastMgr.shortBottomCenter(mContext, "获取存储权限失败")
                            }
                        }
                    })
                return result
            }


            override fun onChoicePrompt(
                session: GeckoSession,
                prompt: ChoicePrompt
            ): GeckoResult<PromptResponse>? {
                val result = GeckoResult<PromptResponse>()
                //prompt.
                val jsChoiceDialog =
                    org.mozilla.xiu.browser.broswer.dialog.JsChoiceDialog(
                        mContext,
                        prompt,
                        result
                    )
                jsChoiceDialog.show()
                return result
            }

            override fun onAlertPrompt(
                session: GeckoSession,
                prompt: AlertPrompt
            ): GeckoResult<PromptResponse>? {
                val result = GeckoResult<PromptResponse>()
                val alertDialog =
                    org.mozilla.xiu.browser.broswer.dialog.AlertDialog(mContext, prompt, result)
                alertDialog.show()
                Log.d("ButtonPrompt", "its me")
                return result
            }
        }
        session.permissionDelegate = ExamplePermissionDelegate(mContext)
        addSessionTabDelegate(mContext, session, WebExtensionRuntimeManager.extensions)

        session.textInput.setDelegate(object : TextInputDelegate {
            private fun getInputMethodManager(view: View?): InputMethodManager? {
                return if (view == null) {
                    null
                } else view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            }

            override fun restartInput(session: GeckoSession, reason: Int) {
                ThreadUtils.assertOnUiThread()
                val view = session.textInput.view
                val imm = getInputMethodManager(view) ?: return

                // InputMethodManager has internal logic to detect if we are restarting input
                // in an already focused View, which is the case here because all content text
                // fields are inside one LayerView. When this happens, InputMethodManager will
                // tell the input method to soft reset instead of hard reset. Stock latin IME
                // on Android 4.2+ has a quirk that when it soft resets, it does not clear the
                // composition. The following workaround tricks the IME into clearing the
                // composition when soft resetting.
                if (InputMethods.needsSoftResetWorkaround(
                        InputMethods.getCurrentInputMethod(view!!.context)
                    )
                ) {
                    // Fake a selection change, because the IME clears the composition when
                    // the selection changes, even if soft-resetting. Offsets here must be
                    // different from the previous selection offsets, and -1 seems to be a
                    // reasonable, deterministic value
                    imm.updateSelection(view, -1, -1, -1, -1)
                }
                try {
                    imm.restartInput(view)
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }
            }

            override fun showSoftInput(session: GeckoSession) {
                ThreadUtils.assertOnUiThread()
                val view = session.textInput.view
                view?.let {
                    Log.d("test", "showSoftInput: start")
                    val now = System.currentTimeMillis()
                    if (now - softOutputShowTime < 200) {
                        //有的网站会同时调用多次，导致后面几次返回所需时间超过1秒
                        return
                    }
                    if (softInputShowing && now - softOutputShowTime < 1000) {
                        return
                    }
                    softOutputShowTime = now
                    val imm = getInputMethodManager(view)
                    if (imm != null) {
                        if (view.hasFocus() && !imm.isActive(view)) {
                            // Marshmallow workaround: The view has focus but it is not the active
                            // view for the input method. (Bug 1211848)
                            view.clearFocus()
                            view.requestFocus()
                        }
                        val holder = VarHolder(false)
                        ThreadTool.async {
                            val countDownLatch = CountDownLatch(1)
                            val event = InputHeightListenPostEvent(countDownLatch, holder)
                            try {
                                EventBus.getDefault().post(event)
                                countDownLatch.await(1500, TimeUnit.MILLISECONDS)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            withContext(Dispatchers.Main) {
                                if (holder.data) {
                                    mContext.window
                                        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                                } else {
                                    mContext.window
                                        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                                }
                                Log.d("test", "showSoftInput: " + holder.data)
                                imm.showSoftInput(view, 0)
                                softInputShowing = true
                            }
                        }
                    }
                }
            }

            override fun hideSoftInput(session: GeckoSession) {
                try {
                    ThreadUtils.assertOnUiThread()
                    val view = session.textInput.view
                    val imm = getInputMethodManager(view)
                    imm?.hideSoftInputFromWindow(view!!.windowToken, 0)
                    mContext.window
                        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    softInputShowing = false
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun updateSelection(
                session: GeckoSession,
                selStart: Int,
                selEnd: Int,
                compositionStart: Int,
                compositionEnd: Int
            ) {
                ThreadUtils.assertOnUiThread()
                val view = session.textInput.view
                val imm = getInputMethodManager(view)
                imm?.updateSelection(view, selStart, selEnd, compositionStart, compositionEnd)
            }

            override fun updateExtractedText(
                session: GeckoSession,
                request: ExtractedTextRequest,
                text: ExtractedText
            ) {
                ThreadUtils.assertOnUiThread()
                val view = session.textInput.view
                val imm = getInputMethodManager(view)
                imm?.updateExtractedText(view, request.token, text)
            }

            override fun updateCursorAnchorInfo(
                session: GeckoSession, info: CursorAnchorInfo
            ) {
                ThreadUtils.assertOnUiThread()
                val view = session.textInput.view
                val imm = getInputMethodManager(view)
                imm?.updateCursorAnchorInfo(view, info)
            }
        })
        session.translationsSessionDelegate = ExampleTranslationsSessionDelegate()
    }

    fun getDefaultThemeColor(context: Context?): Int {
        val eye: String? = PreferenceMgr.getString(context ?: App.application, "eye", null)
        return if (StringUtil.isNotEmpty(eye)) {
            Color.parseColor(eye)
        } else {
            ContextCompat.getColor(context ?: App.application!!, R.color.surface)
        }
    }

    fun close() {
        session.close()
        bitmap.recycle()
        if (mProgress in 1..99) {
            Log.d("test", "onProgressChange: 101")
            EventBus.getDefault().post(ProgressEvent(100))
        }
    }

    fun open() {
        if (!session.isOpen)
            session.open(GeckoRuntime.getDefault(mContext))
    }

    @SuppressLint("SuspiciousIndentation")
    fun resume() {
        if (!session.isOpen) {
            session.open(GeckoRuntime.getDefault(mContext))
        }
//        sessionState?.let {
//            session.restoreState(it)
//        }
        session.setActive(true)
    }

    fun pause() {
        session.setActive(false)
    }

    private fun pageFinish(session: GeckoSession) {
        session.loadAppBarColors(mContext, false, getDefaultThemeColor(mContext)) { arr ->
            statusBarColor = arr[0]
            navigationBarColor = arr[1]
            onPageStopCall(session, this@SessionDelegate)
        }
    }

    interface Login {
        fun onLogin(code: String, state: String, action: String)
    }

    interface Setpic {
        fun onSetPic()
    }

    interface PageError {
        fun onPageError(
            session: GeckoSession,
            uri: String?,
            error: WebRequestError
        ): GeckoResult<String>?

        fun onPageChange()
    }

    fun setDetectorListener(listener: DetectorListener?) {
        detector.detectorListener = listener
    }

    fun onRequest(request: TabRequest) {
        detector.generateTypeAndSize(this, request)
        if (StringUtil.isNotEmpty(request.url) && request.url.contains("m3u8") && request.statusCode != 0) {
            for (indexedValue in requests.withIndex()) {
                val req = indexedValue.value
                if (request.url == req.url && req.statusCode == 0) {
                    requests.removeAt(indexedValue.index)
                    break
                }
            }
        }
        requests.add(request)
    }

    private fun addDownloadTask(name: String, u: String?) {
        if (u.isNullOrEmpty()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                addDownloadTask1(name, u)
                return
            }
        }
        XXPermissions.with(mContext)
            .permission(Permission.READ_MEDIA_IMAGES)
            .permission(Permission.READ_MEDIA_AUDIO)
            .permission(Permission.READ_MEDIA_VIDEO)
            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    addDownloadTask1(name, u)
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        ToastMgr.shortBottomCenter(mContext, "被永久拒绝授权，请手动授予存储权限")
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(mContext, permissions)
                    } else {
                        ToastMgr.shortBottomCenter(mContext, "获取存储权限失败")
                    }
                }
            })
    }

    private fun addDownloadTask1(name: String, u: String?) {
        if (u.isNullOrEmpty()) {
            return
        }
        addDownloadTask0(name, u, 0)
    }

    private fun addDownloadTask0(name: String, url: String, count: Int) {
        val headers = findRequestHeaders(url)
        if (count <= 10 && headers == null) {
            mContext.lifecycleScope.launch {
                delay(50)
                addDownloadTask0(name, url, count + 1)
            }
            return
        }
        Log.d(
            "sessionDelegate",
            "addDownloadTask0: " + url + ", headers: " + JSON.toJSONString(headers)
        )
        mContext.lifecycleScope.launch {
            startDownload(mContext, name, url, headers)
        }
    }

    private fun findRequestHeaders(u: String): MutableMap<String, String?>? {
        for (request in requests) {
            if (request.url == u) {
                val h = request.requestHeaderMap
//                h.remove("Accept")
//                h.remove("accept")
//                h.remove("Accept-Encoding")
//                h.remove("accept-encoding")
//                h.remove("Accept-Language")
//                h.remove("accept-language")
//                h.remove("Connection")
//                h.remove("connection")
                return h
            }
        }
        return null
    }

    private fun findResponseHeaders(u: String): MutableMap<String, String>? {
        for (request in requests) {
            if (request.url == u) {
                return request.responseHeaderMap
            }
        }
        return null
    }

    private fun addHistory() {
        if (!privacy && u.startsWith("http") && mTitle.isNotEmpty()) {
            val url = u
            val history = History(url, mTitle, 0, icon)
            mContext.async {
                historyViewModel.deleteHistory(url)
                historyViewModel.insertHistories(history)
            }
        }
    }

    fun updateIcon(ic: String) {
        icon = ic
        addHistory()
    }

    private inner class ExampleTranslationsSessionDelegate : TranslationsController.SessionTranslation.Delegate {
        override fun onOfferTranslate(session: GeckoSession) {
            Log.i("test", "onOfferTranslate")
        }

        override fun onExpectedTranslate(session: GeckoSession) {
            Log.i("test", "onExpectedTranslate")
            mExpectedTranslate = true
        }

        override fun onTranslationStateChange(
            session: GeckoSession,
            translationState: TranslationsController.SessionTranslation.TranslationState?
        ) {
            Log.i("test", "onTranslationStateChange")
            if (translationState!!.detectedLanguages != null) {
                mDetectedLanguage = translationState.detectedLanguages!!.docLangTag
            }
        }
    }

    companion object {
        var softInputShowing = false
        var softOutputShowTime = 0L
    }
}






