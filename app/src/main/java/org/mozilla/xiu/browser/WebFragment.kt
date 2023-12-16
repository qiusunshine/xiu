package org.mozilla.xiu.browser

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.renderscript.RSRuntimeException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import jp.wasabeef.glide.transformations.internal.FastBlur
import jp.wasabeef.glide.transformations.internal.RSBlur
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoResult.OnValueListener
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.TranslationsController
import org.mozilla.geckoview.TranslationsController.RuntimeTranslation.LanguageModel
import org.mozilla.geckoview.TranslationsController.RuntimeTranslation.ModelManagementOptions
import org.mozilla.geckoview.TranslationsController.RuntimeTranslation.TranslationSupport
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebRequestError.ERROR_BAD_HSTS_CERT
import org.mozilla.geckoview.WebRequestError.ERROR_CATEGORY_CONTENT
import org.mozilla.geckoview.WebRequestError.ERROR_CATEGORY_PROXY
import org.mozilla.geckoview.WebRequestError.ERROR_MALFORMED_URI
import org.mozilla.geckoview.WebRequestError.ERROR_SECURITY_BAD_CERT
import org.mozilla.geckoview.WebRequestError.ERROR_SECURITY_SSL
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.databinding.FragmentSecondBinding
import org.mozilla.xiu.browser.session.DelegateLivedata
import org.mozilla.xiu.browser.session.GeckoViewModel
import org.mozilla.xiu.browser.session.SessionDelegate
import org.mozilla.xiu.browser.session.TranslateEvent
import org.mozilla.xiu.browser.tab.AddTabLiveData
import org.mozilla.xiu.browser.tab.DelegateListLiveData
import org.mozilla.xiu.browser.tab.RemoveTabLiveData
import org.mozilla.xiu.browser.utils.StatusUtils
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.UriUtilsPro
import org.mozilla.xiu.browser.utils.filePicker.FilePicker
import org.mozilla.xiu.browser.webextension.DetectorListener
import org.mozilla.xiu.browser.webextension.WebExtensionRuntimeManager
import org.mozilla.xiu.browser.webextension.WebExtensionsEnableEvent
import java.io.File
import java.util.Arrays


/**
 * 2023.1.4创建，1.21除夕
 * 2023.2.11 19:10 正月廿一 记录
 * thallo
 **/
class WebFragment(
    var fullscreenCall: (full: Boolean) -> Unit,
    var onUrlChange: (url: String, sessionDelegate: SessionDelegate) -> Unit = { u, se -> },
    var detectorListener: DetectorListener? = null
) : Fragment() {

    constructor() : this({ full -> })

    private var _binding: FragmentSecondBinding? = null
    lateinit var geckoViewModel: GeckoViewModel
    lateinit var delegate: ArrayList<SessionDelegate>
    lateinit var uri: Uri
    lateinit var sessiondelegate: SessionDelegate
    var mPosX: Float = 0f
    var mPosY: Float = 0f
    var mCurPosX: Float = 0f
    var mCurPosY: Float = 0f

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    var active: Int = -1
    lateinit var filePicker: FilePicker
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        geckoViewModel = ViewModelProvider(requireActivity())[GeckoViewModel::class.java]
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcher: ActivityResultLauncher<Boolean> =
            registerForActivityResult(
                org.mozilla.xiu.browser.utils.filePicker.ResultContract(),
                object : ActivityResultCallback<Intent?> {
                    override fun onActivityResult(result: Intent?) {
                        if (result == null) {
                            return
                        }
                        val uri = result.data
                        val path: String =
                            UriUtilsPro.getRootDir(context) + File.separator + "_cache" + File.separator + UriUtilsPro.getFileName(
                                uri
                            )
                        UriUtilsPro.getFilePathFromURI(
                            context,
                            uri,
                            path,
                            object : UriUtilsPro.LoadListener {
                                override fun success(s: String) {
                                    ThreadTool.runOnUI {
                                        filePicker.putUri(Uri.parse("file://$s"))
                                    }
                                }

                                override fun failed(msg: String) {
                                    ThreadTool.runOnUI {
                                        filePicker.putUri(uri)
                                    }
                                    ToastMgr.shortBottomCenter(
                                        context,
                                        "出错：$msg"
                                    )
                                }
                            })
                    }
                })


        filePicker = FilePicker(launcher, requireActivity())
    }

    fun removeSessionDelegates(exclude: SessionDelegate?) {
        val iterator = delegate.iterator()
        while (iterator.hasNext()) {
            val sessionDelegate = iterator.next()
            if (sessionDelegate == exclude) {
                continue
            }
            sessionDelegate.close()
            iterator.remove()
        }
        DelegateListLiveData.getInstance().Value(delegate)
        if (exclude == null) {
            //全部移除
            HomeLivedata.getInstance().Value(true)
        }
    }

    @SuppressLint("ClickableViewAccessibility", "WrongThread")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        delegate = ArrayList<SessionDelegate>()
        lifecycleScope.launch {
            geckoViewModel.data.collect { value: GeckoSession ->
                openSession(value)
                //Toast.makeText(context,"ok",Toast.LENGTH_SHORT).show()
            }
        }

        DelegateListLiveData.getInstance().observe(viewLifecycleOwner) {
            delegate = it
        }
        RemoveTabLiveData.getInstance().observe(viewLifecycleOwner) { sessionDelegate0 ->
            val it = delegate.indexOf(sessionDelegate0)
            sessionDelegate0.close()
            if (it >= 0 && delegate.size > it) {
                val removed = delegate.removeAt(it)
                if (removed.active) {
                    if (delegate.getOrNull(it) == null) {
                        if (delegate.getOrNull(it - 1) == null) {
                            HomeLivedata.getInstance().Value(true)
                        } else {
                            DelegateLivedata.getInstance().Value(delegate[it - 1])
                        }
                    } else {
                        DelegateLivedata.getInstance().Value(delegate[it])
                    }
                }
                DelegateListLiveData.getInstance().Value(delegate)
            }
        }
        DelegateLivedata.getInstance().observe(viewLifecycleOwner) {
            binding.geckoview.session?.setActive(false)
            for (i in delegate) {
                if (it != i) {
                    i.active = false
                }
            }
            it.active = true
            active = delegate.indexOf(it)
            AddTabLiveData.getInstance().Value(active)
            it.session.setActive(true)
            GeckoRuntime.getDefault(requireContext()).webExtensionController.setTabActive(
                it.session,
                true
            )
            binding.geckoview.setSession(it.session)
            StatusUtils.setStatusBarColor(
                requireActivity(),
                it.statusBarColor,
                binding.root
            )
            sessiondelegate = it
            hideErrorPage()
        }
        binding.geckoview.activityContextDelegate = GeckoView.ActivityContextDelegate { activity }

        GeckoRuntime.getDefault(requireContext()).webExtensionController
            .ensureBuiltIn("resource://android/assets/extensions/xiutan/", "xiutan@xiu.com")
            .then { extension: WebExtension? ->
                if (extension != null) {
                    GeckoRuntime.getDefault(requireContext()).webExtensionController
                        .setAllowedInPrivateBrowsing(
                            extension, true
                        )
                } else {
                    null
                }
            }.accept({ extension: WebExtension? ->
                if (extension == null) {
                    return@accept
                }
                EventBus.getDefault().post(WebExtensionsEnableEvent(extension, true))
                WebExtensionRuntimeManager.checkOrRefresh(extension)
            }, { exception: Throwable? ->
                exception?.printStackTrace()
            })
    }

    fun openSession(session: GeckoSession) {
        binding.geckoview.releaseSession()
        val sessionDelegate: SessionDelegate? =
            activity?.let {
                SessionDelegate(
                    it,
                    session,
                    filePicker,
                    fullscreenCall = { full ->
                        fullscreenCall(full)
                    },
                    onPageStopCall1 = { se, de ->
                        if (binding.geckoview.session == se) {
                            StatusUtils.setStatusBarColor(
                                requireActivity(),
                                de.statusBarColor,
                                binding.root
                            )
                        }
                    },
                    onUrlChange = onUrlChange,
                    onCrashCall = {
                        binding.geckoview.alpha = 0f
                        binding.geckoview.animate().setDuration(300).alpha(1f).start()
                    },
                    detectorListener = detectorListener
                )
            }
        if (sessionDelegate != null) {
            sessionDelegate.setpic = object : SessionDelegate.Setpic {
                override fun onSetPic() {
                    try {
                        binding.geckoview.capturePixels().accept {
                            if (it != null) {
                                if (sessionDelegate.privacy) {
                                    //默认缩小为1/4
                                    val scaledBitmap = Bitmap.createScaledBitmap(
                                        it,
                                        it.width / 4,
                                        it.height / 4,
                                        false
                                    )
                                    val bitmap = try {
                                        RSBlur.blur(context, scaledBitmap, 25)
                                    } catch (e: RSRuntimeException) {
                                        FastBlur.blur(scaledBitmap, 25, true)
                                    } catch (e: Exception) {
                                        ContextCompat.getDrawable(
                                            requireContext(),
                                            R.drawable.close_outline
                                        )?.toBitmap()!!
                                    }
                                    sessionDelegate.bitmap = bitmap
                                } else {
                                    sessionDelegate.bitmap = it
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            }
            sessionDelegate.pageError = object : SessionDelegate.PageError {
                override fun onPageChange() {
                    hideErrorPage()
                }

                override fun onPageError(
                    session: GeckoSession,
                    uri: String?,
                    error: WebRequestError
                ): GeckoResult<String>? {
                    val result = GeckoResult<String>()
                    binding.errorpage.visibility = View.VISIBLE
                    binding.errorCodeText.text =
                        "Error:${error.code}\n" + getErrorMessage(error.code)
                    binding.errorRetryButton.setOnClickListener { session.reload() }
                    if (error.code == ERROR_SECURITY_BAD_CERT || error.code == ERROR_BAD_HSTS_CERT) {
                        binding.ignoreButton.visibility = View.VISIBLE
                        binding.ignoreButton.setOnClickListener {
                            try {
                                result.complete(
                                    "data:text/html,<!DOCTYPE html>\n" +
                                            "<html>\n" +
                                            "  <head>\n" +
                                            "<script type=\"application/javascript\">\n" +
                                            "async function ok() {\n" +
                                            "   await document.addCertException(false); \n" +
                                            "   location.reload();\n" +
                                            "}\n" +
                                            "ok();\n" +
                                            "</script>\n" +
                                            "  </head>\n" +
                                            "</html>"
                                )
                                onPageChange()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        binding.ignoreButton.visibility = View.GONE
                    }
                    return result
                }
            }
        }
        if (delegate.size == 0)
            sessionDelegate?.let { delegate.add(it) }
        else
            sessionDelegate?.let {
                if (active >= delegate.size) {
                    active = delegate.size - 1
                }
                delegate.add(active + 1, it)
            }
        sessionDelegate?.let { DelegateLivedata.getInstance().Value(it) }
        DelegateListLiveData.getInstance().Value(delegate)
        binding.geckoview.coverUntilFirstPaint(
            ContextCompat.getColor(
                requireContext(),
                R.color.surface
            )
        )
        //binding.geckoview.setSession(session)
    }

    private fun hideErrorPage() {
        binding.errorpage.visibility = View.GONE
    }

    private fun getErrorMessage(code: Int): String {
        return when (code) {
            ERROR_MALFORMED_URI -> "无效的地址\nERROR_MALFORMED_URI"
            ERROR_SECURITY_SSL -> "无效的SSL\nERROR_SECURITY_SSL"
            ERROR_BAD_HSTS_CERT -> "证书校验错误\nERROR_BAD_HSTS_CERT"
            ERROR_SECURITY_BAD_CERT -> "SSL证书不受信任或无效\nERROR_SECURITY_BAD_CERT"
            ERROR_CATEGORY_PROXY -> "ERROR_CATEGORY_PROXY"
            ERROR_CATEGORY_CONTENT -> "ERROR_CATEGORY_CONTENT"
            WebRequestError.ERROR_CONNECTION_REFUSED -> "连接被拒绝\nERROR_CONNECTION_REFUSED"
            WebRequestError.ERROR_NET_TIMEOUT -> "连接超时\nERROR_NET_TIMEOUT"
            WebRequestError.ERROR_NET_INTERRUPT -> "连接中断\nERROR_NET_INTERRUPT"
            WebRequestError.ERROR_NET_RESET -> "连接被重置\nERROR_NET_RESET"
            WebRequestError.ERROR_UNKNOWN_HOST -> "域名解析失败\nERROR_UNKNOWN_HOST"
            WebRequestError.ERROR_FILE_NOT_FOUND -> "找不到文件\nERROR_FILE_NOT_FOUND"
            WebRequestError.ERROR_FILE_ACCESS_DENIED -> "无权限访问文件\nERROR_FILE_ACCESS_DENIED"
            WebRequestError.ERROR_PROXY_CONNECTION_REFUSED -> "代理服务器拒绝了请求\nERROR_PROXY_CONNECTION_REFUSED"
            else -> code.toString()
        }
    }

    fun isErrorShown(): Boolean {
        return binding.errorpage.visibility == View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
    }

    fun getCoordinatorLayout(): View {
        return binding.coordinatorLayout
    }

    fun translate(event: TranslateEvent) {
        if (event.translate) {
            translate(sessiondelegate.session)
        } else {
            translateRestore(sessiondelegate.session)
        }
    }

    private fun translate(session: GeckoSession) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.translate)
        val fromSelect = Spinner(requireContext())
        val toSelect = Spinner(requireContext())
        // Set spinners with data
        TranslationsController.RuntimeTranslation.listSupportedLanguages()
            .then<Any> { supportedLanguages: TranslationSupport? ->
                // Just a check if sorting is working on the Language object by reversing, Languages
                // should generally come from the API in the display order.
                if (supportedLanguages?.fromLanguages == null || supportedLanguages.toLanguages == null) {
                    return@then null
                }
                supportedLanguages.fromLanguages?.reverse()
                val fromData =
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        supportedLanguages.fromLanguages!!
                    )
                fromSelect.adapter = fromData
                // Set detected language
                val index = fromData.getPosition(
                    TranslationsController.Language(sessiondelegate.mDetectedLanguage ?: "en", null)
                )
                fromSelect.setSelection(index)
                val toData =
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        supportedLanguages.toLanguages!!
                    )
                toSelect.adapter = toData
                // Set preferred language
                TranslationsController.RuntimeTranslation.preferredLanguages()
                    .then<Any> { preferredList: MutableList<String>? ->
                        Log.d(
                            "test",
                            "Preferred Translation Languages: $preferredList"
                        )
                        if (preferredList == null) {
                            return@then null
                        }
                        // Reorder dropdown listing based on preferences
                        for (i in preferredList.indices.reversed()) {
                            val langIndex = toData.getPosition(
                                TranslationsController.Language(preferredList.get(i)!!, null)
                            )
                            if (langIndex >= 0) {
                                val displayLanguage =
                                    toData.getItem(langIndex)
                                toData.remove(displayLanguage)
                                toData.insert(displayLanguage, 0)
                            }
                            if (i == 0) {
                                toSelect.setSelection(0)
                            }
                        }
                        null
                    }
                null
            }
        builder.setView(
            translateLayout(
                fromSelect,
                R.string.translate_language_from_hint,
                toSelect,
                R.string.translate_language_to_hint,
                -1
            )
        )
        builder.setPositiveButton(
            R.string.translate_action
        ) { dialog, which ->
            val fromLang =
                fromSelect.selectedItem as TranslationsController.Language
            val toLang =
                toSelect.selectedItem as TranslationsController.Language
            session.sessionTranslation!!.translate(fromLang.code, toLang.code, null)
                .exceptionally<Any> {
                    sessiondelegate.mTranslateRestore = false
                    Log.e("test", "translate: " + it.toString(), it)
                    null
                }
            sessiondelegate.mTranslateRestore = true
        }
        builder.setNegativeButton(
            R.string.cancel
        ) { dialog: DialogInterface?, which: Int -> }
        builder.setNeutralButton(
            R.string.translate_manage
        ) { dialog: DialogInterface?, which: Int ->
            translateManage()
        }
        builder.show()
    }

    private fun translateRestore(session: GeckoSession) {
        sessiondelegate.mTranslateRestore = false
        session
            .sessionTranslation?.restoreOriginalPage()?.then<Any> {
                sessiondelegate.mTranslateRestore = false
                null
            }
    }

    private fun translateManage() {
        val languageSelect = Spinner(context)
        val operationSelect = Spinner(context)
        // Should match ModelOperation choices
        val operationChoices: List<String> = ArrayList(
            Arrays.asList(
                *arrayOf(
                    TranslationsController.RuntimeTranslation.DELETE,
                    TranslationsController.RuntimeTranslation.DOWNLOAD
                )
            )
        )
        val operationData = ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_spinner_item, operationChoices
        )
        operationSelect.adapter = operationData

        // Get current model states
        val currentStates = TranslationsController.RuntimeTranslation.listModelDownloadStates()
        currentStates.then(
            object : OnValueListener<List<LanguageModel>, Any> {
                override fun onValue(models: List<LanguageModel>?): GeckoResult<Any>? {
                    if (models == null) return null
                    val languages: MutableList<TranslationsController.Language?> =
                        ArrayList()
                    // Pseudo container of "all" just to simplify spinner for GVE
                    languages.add(TranslationsController.Language("all", "All Models"))
                    for (model in models) {
                        Log.i("test", "Translate Model State: $model")
                        languages.add(model.language)
                    }
                    val languageData =
                        ArrayAdapter<TranslationsController.Language?>(
                            requireContext(), android.R.layout.simple_spinner_item, languages
                        )
                    languageSelect.adapter = languageData
                    return null
                }
            })
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.translate_manage)
        builder.setView(
            translateLayout(
                languageSelect,
                R.string.translate_manage_languages,
                operationSelect,
                R.string.translate_manage_operations,
                R.string.translate_display_hint
            )
        )
        builder.setPositiveButton(
            R.string.translate_manage_action
        ) { dialog, which ->
            val selectedLanguage =
                languageSelect.selectedItem as TranslationsController.Language
            val operation = operationSelect.selectedItem as String
            var operationLevel = TranslationsController.RuntimeTranslation.LANGUAGE
            // Pseudo option for ease of GVE
            if (selectedLanguage.code == "all") {
                operationLevel = TranslationsController.RuntimeTranslation.ALL
            }
            val options = ModelManagementOptions.Builder()
                .languageToManage(selectedLanguage.code)
                .operation(operation)
                .operationLevel(operationLevel)
                .build()

            // Complete Operation
            val requestOperation =
                TranslationsController.RuntimeTranslation.manageLanguageModel(options)
            requestOperation.then<Any> { opt: Void? ->
                // Log Changes
                val reportChanges =
                    TranslationsController.RuntimeTranslation.listModelDownloadStates()
                reportChanges.then<Any> { models ->
                    models?.let {
                        for (model in models) {
                            Log.i("test", "Translate Model State: $model")
                        }
                    }
                    null
                }
                null
            }
        }
        builder.setNegativeButton(
            R.string.cancel
        ) { dialog, which -> }
        builder.show()
    }

    private fun translateLayout(
        spinnerA: Spinner, labelA: Int, spinnerB: Spinner, labelB: Int, labelInfo: Int
    ): RelativeLayout {
        // From fields
        val fromLangLabel = TextView(requireContext())
        fromLangLabel.setText(labelA)
        val from = LinearLayout(requireContext())
        from.id = View.generateViewId()
        from.addView(fromLangLabel)
        from.addView(spinnerA)
        val fromParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        fromParams.marginStart = 30

        // To fields
        val toLangLabel = TextView(context)
        toLangLabel.setText(labelB)
        val to = LinearLayout(context)
        to.id = View.generateViewId()
        to.addView(toLangLabel)
        to.addView(spinnerB)
        val toParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        toParams.marginStart = 30
        toParams.addRule(RelativeLayout.BELOW, from.id)

        // Layout
        val layout = RelativeLayout(context)
        layout.addView(from, fromParams)
        layout.addView(to, toParams)

        // Hint
        val info = TextView(context)
        if (labelInfo != -1) {
            val infoParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            infoParams.marginStart = 30
            infoParams.addRule(RelativeLayout.BELOW, to.id)
            info.setText(labelInfo)
            layout.addView(info, infoParams)
        }
        return layout
    }
}