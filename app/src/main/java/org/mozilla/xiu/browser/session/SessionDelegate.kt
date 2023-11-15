package org.mozilla.xiu.browser.session

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.interfaces.OnBindView
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.mozilla.geckoview.*
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSession.ProgressDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate.*
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.BR
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.componets.ContextMenuDialog
import org.mozilla.xiu.browser.componets.popup.IntentPopup
import org.mozilla.xiu.browser.database.history.History
import org.mozilla.xiu.browser.database.history.HistoryViewModel
import org.mozilla.xiu.browser.download.DownloadTask
import org.mozilla.xiu.browser.download.DownloadTaskLiveData
import org.mozilla.xiu.browser.utils.PreferenceMgr
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.UriUtils
import org.mozilla.xiu.browser.utils.filePicker.FilePicker
import org.mozilla.xiu.browser.webextension.WebextensionSession
import java.io.IOException


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
    var privacy: Boolean = false

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
    var uri: Uri? = null
    private lateinit var filePicker: FilePicker
    lateinit var sessionState: GeckoSession.SessionState
    private var sessionStateMap = HashMap<Int, GeckoSession.SessionState>()
    private lateinit var fullscreenCall: (full: Boolean) -> Unit
    //private lateinit var historySync: HistorySync

    var statusBarColor: Int = 0xffffff
    var navigationBarColor: Int = 0xffffff
    private lateinit var onPageStopCall: (session: GeckoSession, sessionDelegate: SessionDelegate) -> Unit

    constructor(
        mContext: FragmentActivity,
        session: GeckoSession,
        filePicker: FilePicker,
        privacy: Boolean,
        fullscreenCall: (full: Boolean) -> Unit,
        onPageStopCall1: (session: GeckoSession, sessionDelegate: SessionDelegate) -> Unit = { se, de -> }
    ) : this() {
        this.mContext = mContext
        statusBarColor = getDefaultThemeColor(mContext)
        navigationBarColor = getDefaultThemeColor(mContext)
        this.session = session
        this.filePicker = filePicker
        this.privacy = privacy
        this.fullscreenCall = fullscreenCall
        this.onPageStopCall = onPageStopCall1
        notifyPropertyChanged(BR.privacy)


        val geckoViewModel: GeckoViewModel =
            ViewModelProvider(mContext).get(GeckoViewModel::class.java)
        historyViewModel = ViewModelProvider(mContext).get(HistoryViewModel::class.java)
        bitmap = mContext.getDrawable(R.drawable.logo72)?.toBitmap()!!
        intentPopup = IntentPopup(mContext)
        //historySync = HistorySync(mContext)


        DownloadTaskLiveData.getInstance().observe(mContext) {
            downloadTasks = it
        }
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onContextMenu(
                session: GeckoSession,
                screenX: Int,
                screenY: Int,
                element: GeckoSession.ContentDelegate.ContextElement
            ) {
                super.onContextMenu(session, screenX, screenY, element)
                ContextMenuDialog(mContext, element).open()
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                var uri = response.uri
                Log.d("test", "onExternalResponse: $uri")
                val name = UriUtils.getFileName(response)
                //没有下载，关闭response
                try {
                    response.body?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (uri.endsWith("xpi")) {
                    WebextensionSession(mContext).install(uri)
                } else {
                    PopTip.build()
                        .setCustomView(object :
                            OnBindView<PopTip?>(org.mozilla.xiu.browser.R.layout.pop_mytip) {
                            override fun onBind(dialog: PopTip?, v: View) {
                                v.findViewById<TextView>(R.id.textView17).text = "网页希望下载文件"
                                v.findViewById<MaterialButton>(R.id.materialButton7)
                                    .setOnClickListener {
                                        mContext.lifecycleScope.launch {
                                            var downloadTask = DownloadTask(
                                                mContext,
                                                uri,
                                                name
                                            )
                                            downloadTask.open()
                                            downloadTasks.add(downloadTask)
                                            DownloadTaskLiveData.getInstance().Value(downloadTasks)
                                        }
                                    }
                            }
                        })
                        .show()

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
                if (!session.isOpen) {
                    session.open(GeckoRuntime.getDefault(mContext))
                }
                session.restoreState(sessionStateMap[session.hashCode()] ?: sessionState)
                ThreadTool.postUIDelayed(100) {
                    pageFinish(session)
                }
            }

            override fun onKill(session: GeckoSession) {
                super.onKill(session)
                Log.d("test", "onCrash kill")
                if (!session.isOpen) {
                    session.open(GeckoRuntime.getDefault(mContext))
                }
                session.restoreState(sessionStateMap[session.hashCode()] ?: sessionState)
                ThreadTool.postUIDelayed(100) {
                    pageFinish(session)
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
                if (!privacy) {
                    var history = title?.let { History(u, it, 0) }
                    historyViewModel.insertHistories(history)
                    //historySync.sync(u)

                }


                notifyPropertyChanged(BR.mTitle)

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
                sessionStateMap[session.hashCode()] = sessionState
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                super.onProgressChange(session, progress)
                Log.d("test", "onProgressChange: $progress")
                EventBus.getDefault().post(ProgressEvent(progress))
                if (progress != 100)
                    mProgress = progress
                else mProgress = 0


                notifyPropertyChanged(BR.mProgress)
            }

            override fun onPageStart(session: GeckoSession, url: String) {
//                if (url.startsWith("$CONFIG_URL/oauth/success/3c49430b43dfba77")) {
//                    val uri = Uri.parse(url)
//                    val code = uri.getQueryParameter("code")
//                    val state = uri.getQueryParameter("state")
//                    val action = uri.getQueryParameter("action")
//                    if (code != null && state != null && action != null) {
//                        //listener?.onLoginComplete(code, state, mContext)
//                        //Toast.makeText(mContext,code+"**"+state,Toast.LENGTH_SHORT).show()
//                        login.onLogin(code, state, action)
//                    }
//                }
                notifyPropertyChanged(BR.y)

            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                pageFinish(session)
            }
        }



        session.navigationDelegate = object : NavigationDelegate {
            override fun onSubframeLoadRequest(
                session: GeckoSession,
                request: NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val uri = Uri.parse(request.uri)
                var url = request.uri
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
                                PopTip.build()
                                    .setCustomView(object :
                                        OnBindView<PopTip?>(org.mozilla.xiu.browser.R.layout.pop_mytip) {
                                        override fun onBind(dialog: PopTip?, v: View) {
                                            v.findViewById<TextView>(R.id.textView17).text =
                                                mContext.getText(R.string.intent_message)
                                            v.findViewById<MaterialButton>(R.id.materialButton7)
                                                .setOnClickListener {
                                                    mContext.startActivity(intent)
                                                }
                                        }
                                    })
                                    .show()
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
                var url = request.uri
                if (uri.scheme != null) {
                    if (!uri.scheme!!.contains("https") && !uri.scheme!!.contains("http") && !uri.scheme!!.contains(
                            "about"
                        )
                    ) {

                        var intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (intent.resolveActivity(mContext.packageManager) != null) {
                            PopTip.build()
                                .setCustomView(object :
                                    OnBindView<PopTip?>(org.mozilla.xiu.browser.R.layout.pop_mytip) {
                                    override fun onBind(dialog: PopTip?, v: View) {
                                        v.findViewById<TextView>(R.id.textView17).text =
                                            mContext.getText(R.string.intent_message)
                                        v.findViewById<MaterialButton>(R.id.materialButton7)
                                            .setOnClickListener {
                                                mContext.startActivity(intent)
                                            }
                                    }
                                })
                                .show()
                        }

                    }

                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onLoadError(
                session: GeckoSession,
                uri: String?,
                error: WebRequestError
            ): GeckoResult<String>? {
                pageError.onPageError(session, uri, error)
                return super.onLoadError(session, uri, error)
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

                super.onLocationChange(session, url, perms)
                if (url != null) {
                    Log.d("可以？", url)
                }
                if (url != null) {
                    u = url
                }
                pageError.onPageChange()
                notifyPropertyChanged(org.mozilla.xiu.browser.BR.u)
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession>? {
                val newSession = GeckoSession()
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
                val buttonDialog = org.mozilla.xiu.browser.broswer.dialog.ButtonDialog(
                    mContext,
                    prompt
                )
                buttonDialog.showDialog()
                Log.d("ButtonPrompt", "its me")

                return GeckoResult.fromValue(buttonDialog.dialogResult)
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
                val alertDialog =
                    org.mozilla.xiu.browser.broswer.dialog.TextDialog(mContext, prompt)
                alertDialog.showDialog()
                Log.d("TextPrompt", "its me")
                return GeckoResult.fromValue(alertDialog.dialogResult)

            }

            override fun onRepostConfirmPrompt(
                session: GeckoSession,
                prompt: RepostConfirmPrompt
            ): GeckoResult<PromptResponse>? {
                val confirmPrompt =
                    org.mozilla.xiu.browser.broswer.dialog.ConfirmDialog(
                        mContext,
                        prompt
                    )
                confirmPrompt.showDialog()
                Log.d("RepostConfirm", "its me")

                return GeckoResult.fromValue(confirmPrompt.dialogResult)
            }

            override fun onFilePrompt(
                session: GeckoSession,
                prompt: FilePrompt
            ): GeckoResult<PromptResponse>? {
                val getFile = org.mozilla.xiu.browser.utils.filePicker.GetFile(mContext, filePicker)
                getFile.open(mContext, prompt.mimeTypes)
                Log.d("onFilePrompt", getFile.uri.toString())
                return GeckoResult.fromValue(prompt.confirm(mContext, getFile.uri))
            }


            override fun onChoicePrompt(
                session: GeckoSession,
                prompt: ChoicePrompt
            ): GeckoResult<PromptResponse>? {
                //prompt.
                val jsChoiceDialog =
                    org.mozilla.xiu.browser.broswer.dialog.JsChoiceDialog(
                        mContext,
                        prompt
                    )
                jsChoiceDialog.showDialog()
                Log.d("ButtonPrompt", "its me")

                return GeckoResult.fromValue(prompt.confirm(jsChoiceDialog.dialogResult.toString()))
            }

            override fun onAlertPrompt(
                session: GeckoSession,
                prompt: AlertPrompt
            ): GeckoResult<PromptResponse>? {
                val alertDialog =
                    org.mozilla.xiu.browser.broswer.dialog.AlertDialog(mContext, prompt)
                alertDialog.showDialog()
                Log.d("ButtonPrompt", "its me")

                return GeckoResult.fromValue(alertDialog.dialogResult)
            }
        }
        session.permissionDelegate = ExamplePermissionDelegate(mContext)
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
        sessionStateMap.remove(session.hashCode())
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
        if (!session.isOpen)
            session.open(GeckoRuntime.getDefault(mContext))
        session.restoreState(sessionStateMap[session.hashCode()] ?: sessionState)
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
        )

        fun onPageChange()
    }


}






