package org.mozilla.xiu.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
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
    lateinit var session: GeckoSession
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
                                if (sessionDelegate.privacy)
                                    sessionDelegate.bitmap =
                                        requireActivity().getDrawable(R.drawable.close_outline)
                                            ?.toBitmap()!!
                                else
                                    sessionDelegate.bitmap = it
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
}