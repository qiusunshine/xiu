package org.mozilla.xiu.browser


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.FloatingWindow
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.sidesheet.SideSheetBehavior
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import org.apache.commons.lang3.StringUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebNotification
import org.mozilla.xiu.browser.base.VarHolder
import org.mozilla.xiu.browser.broswer.dialog.SearchDialog
import org.mozilla.xiu.browser.broswer.home.TipsAdapter
import org.mozilla.xiu.browser.componets.BookmarkDialog
import org.mozilla.xiu.browser.componets.CollectionAdapter
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.componets.popup.BookmarkPopup
import org.mozilla.xiu.browser.componets.popup.HistoryPopup
import org.mozilla.xiu.browser.componets.popup.MenuPopup
import org.mozilla.xiu.browser.componets.popup.TabPopup
import org.mozilla.xiu.browser.database.history.History
import org.mozilla.xiu.browser.database.history.HistoryViewModel
import org.mozilla.xiu.browser.databinding.ActivityMainBinding
import org.mozilla.xiu.browser.databinding.PrivacyAgreementLayoutBinding
import org.mozilla.xiu.browser.download.UrlDetector
import org.mozilla.xiu.browser.session.*
import org.mozilla.xiu.browser.tab.AddTabLiveData
import org.mozilla.xiu.browser.tab.DelegateListLiveData
import org.mozilla.xiu.browser.tab.RemoveTabLiveData
import org.mozilla.xiu.browser.tab.TabListAdapter
import org.mozilla.xiu.browser.utils.FileUtil
import org.mozilla.xiu.browser.utils.PreferenceMgr
import org.mozilla.xiu.browser.utils.SoftKeyBoardListener.OnSoftKeyBoardChangeListener
import org.mozilla.xiu.browser.utils.StatusUtils
import org.mozilla.xiu.browser.utils.StrUtil
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ThreadTool.postDelayed
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.UriUtilsPro
import org.mozilla.xiu.browser.video.FloatVideoController
import org.mozilla.xiu.browser.video.FloatVideoController.WebHolder
import org.mozilla.xiu.browser.video.VideoDetector
import org.mozilla.xiu.browser.video.event.FloatPlayEvent
import org.mozilla.xiu.browser.video.event.FloatVideoSwitchEvent
import org.mozilla.xiu.browser.video.model.DetectedMediaResult
import org.mozilla.xiu.browser.video.model.Media
import org.mozilla.xiu.browser.video.model.MediaType
import org.mozilla.xiu.browser.view.toast.ChefSnackbar
import org.mozilla.xiu.browser.view.toast.make
import org.mozilla.xiu.browser.webextension.BrowseEvent
import org.mozilla.xiu.browser.webextension.DetectorListener
import org.mozilla.xiu.browser.webextension.EvalJSEvent
import org.mozilla.xiu.browser.webextension.InputHeightListenEvent
import org.mozilla.xiu.browser.webextension.InputHeightListenPostEvent
import org.mozilla.xiu.browser.webextension.TabRequest
import org.mozilla.xiu.browser.webextension.TabRequestEvent
import org.mozilla.xiu.browser.webextension.WebExtensionConnectPortEvent
import org.mozilla.xiu.browser.webextension.WebExtensionRuntimeManager
import org.mozilla.xiu.browser.webextension.WebExtensionsAddEvent
import org.mozilla.xiu.browser.webextension.WebExtensionsEnableEvent
import org.mozilla.xiu.browser.webextension.WebextensionSession
import org.mozilla.xiu.browser.webextension.addDelegate
import org.mozilla.xiu.browser.webextension.removeDelegate
import timber.log.Timber
import java.io.File
import java.util.Objects


/**
 * 2023.1.4创建，1.21除夕
 * 2023.2.11 19:10 正月廿一 记录
 * thallo
 **/
class MainActivity : AppCompatActivity(), DetectorListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var privacyAgreementLayoutBinding: PrivacyAgreementLayoutBinding
    private var fragments: List<Fragment>? = null
    private lateinit var geckoViewModel: GeckoViewModel
    var sessionDelegates = ArrayList<SessionDelegate>()
    private val adapter = TabListAdapter()
    var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null
    lateinit var sideSheetBehavior: SideSheetBehavior<ConstraintLayout>
    var isHome: Boolean = true
    lateinit var historyViewModel: HistoryViewModel
    var searching = ""
    private var port: WebExtension.Port? = null
    private var floatVideoController: FloatVideoController? = null

    private fun fullScreenCall(fullScreen: Boolean) {
        if (fullScreen) {
            StatusUtils.setStatusBarVisibility(this, false, binding.content.viewPager)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            StatusUtils.setStatusBarVisibility(this, true, binding.content.viewPager)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.setHomeActivity(this)
        fragments =
            listOf(
                HomeFragment(),
                WebFragment({ full -> fullScreenCall(full) }, { url, _ ->
                    floatVideoController?.loadUrl(url)
                }, this)
            )
        binding = ActivityMainBinding.inflate(layoutInflater)
        privacyAgreementLayoutBinding = PrivacyAgreementLayoutBinding.inflate(layoutInflater)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        privacyAgreementLayoutBinding.materialButton14.setOnClickListener {
            setContentView(binding.root)
            prefs.edit().putBoolean("privacy1", true).commit()
        }
        privacyAgreementLayoutBinding.materialButton17.setOnClickListener {
            setContentView(binding.root)
            prefs.edit().putBoolean("privacy1", true).commit()
        }
        if (prefs.getBoolean("privacy1", false))
            setContentView(binding.root)
        else
            setContentView(privacyAgreementLayoutBinding.root)
        privacyAgreementLayoutBinding.webView.loadUrl("file:///android_asset/privacy.txt")

        StatusUtils.init(this, binding.root)
        WebextensionSession(this)

        onBackPressedDispatcher.addCallback(this, onBackPress)
        geckoViewModel = ViewModelProvider(this)[GeckoViewModel::class.java]
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        if (binding.drawer != null) {
            bottomSheetBehavior =
                BottomSheetBehavior.from(binding.drawer as ConstraintLayout)
            bottomSheetBehavior!!.peekHeight = 0
            bottomSheetBehavior!!.isDraggable = false
        }

        adapter.select = object : TabListAdapter.Select {
            override fun onSelect() {}
        }

        binding.SearchText?.imeOptions = EditorInfo.IME_ACTION_SEARCH


        binding.materialButton13?.setOnClickListener {
            var searchDialog = SearchDialog(this)
            searchDialog.setOnDismissListener {
                when (org.mozilla.xiu.browser.broswer.SearchEngine(this)) {
                    getString(org.mozilla.xiu.browser.R.string.baidu) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Baidu))

                    getString(org.mozilla.xiu.browser.R.string.google) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Google))

                    getString(org.mozilla.xiu.browser.R.string.bing) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Bing))

                    getString(org.mozilla.xiu.browser.R.string.sogou) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Sougou))

                    getString(org.mozilla.xiu.browser.R.string.sk360) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.s360))

                    getString(org.mozilla.xiu.browser.R.string.wuzhui) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Wuzhui))

                    getString(org.mozilla.xiu.browser.R.string.yandex) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Yandex))

                    getString(org.mozilla.xiu.browser.R.string.shenma) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Shenma))
                }
                if (prefs.getBoolean("switch_diy", false))
                    binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.diySearching))
            }
            searchDialog.show()

        }
        binding.SearchText?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                binding.bottomMotionLayout?.transitionToEnd()
                binding.constraintLayout10?.visibility = View.VISIBLE
                if(binding.SearchText?.text?.toString() == "about:blank") {
                    binding.SearchText?.setText("")
                }
                when (org.mozilla.xiu.browser.broswer.SearchEngine(this)) {
                    getString(org.mozilla.xiu.browser.R.string.baidu) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Baidu))

                    getString(org.mozilla.xiu.browser.R.string.google) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Google))

                    getString(org.mozilla.xiu.browser.R.string.bing) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Bing))

                    getString(org.mozilla.xiu.browser.R.string.sogou) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Sougou))

                    getString(org.mozilla.xiu.browser.R.string.sk360) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.s360))

                    getString(org.mozilla.xiu.browser.R.string.wuzhui) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Wuzhui))

                    getString(org.mozilla.xiu.browser.R.string.yandex) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Yandex))

                    getString(org.mozilla.xiu.browser.R.string.shenma) -> binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.Shenma))
                }
                if (prefs.getBoolean("switch_diy", false))
                    binding.materialButton13?.text =
                        getString(R.string.EngineTips, getString(R.string.diySearching))
            } else {
                binding.bottomMotionLayout?.transitionToStart()
                binding.constraintLayout10?.visibility = View.GONE
                if (!isHome)
                    binding.SearchText?.setText(binding.user?.u)
            }
        }
        binding.materialButtonClear?.setOnClickListener { binding.SearchText?.setText("") }
        var tipsAdapter = TipsAdapter()
        binding.recyclerView4?.adapter = tipsAdapter
        binding.recyclerView4?.layoutManager = LinearLayoutManager(this)
        tipsAdapter.select = object : TipsAdapter.Select {
            override fun onSelect(url: String) {
                searching(url)
                val imm: InputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                // 隐藏软键盘
                imm.hideSoftInputFromWindow(
                    window.decorView.windowToken,
                    0
                )

            }

        }
        val tipsObserver = { it: List<History?>? ->
            tipsAdapter.submitList(it)
        }
        val tipsLiveDataHolder = VarHolder<LiveData<List<History?>?>?>()
        binding.SearchText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val s1 = s.toString().trim()
                if (s1 != "") {
                    if (tipsLiveDataHolder.data != null) {
                        //让其释放资源
                        tipsLiveDataHolder.data?.removeObserver(tipsObserver)
                    }
                    tipsLiveDataHolder.data = historyViewModel.findHistoriesWithMix(s1)
                    tipsLiveDataHolder.data?.observe(this@MainActivity, tipsObserver)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.SearchText?.setOnKeyListener(View.OnKeyListener { _, i, keyEvent ->
            if (KeyEvent.KEYCODE_ENTER == i && keyEvent.action == KeyEvent.ACTION_DOWN) {
                var value = binding.SearchText!!.text.toString()
                searching(value)
                searching = value
            }

            false
        })
        binding.materialButtonEdit?.setOnClickListener { binding.SearchText?.setText(searching) }
        binding.urlText?.setOnKeyListener(View.OnKeyListener { _, i, keyEvent ->
            if (KeyEvent.KEYCODE_ENTER == i && keyEvent.action == KeyEvent.ACTION_DOWN) {
                var value = binding.urlText!!.text.toString()
                searching(value)

            }
            false
        })
        binding.recyclerView2?.adapter = adapter
        binding.recyclerView2?.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.content.viewPager.adapter = CollectionAdapter(this, fragments!!)
        binding.content.viewPager.isUserInputEnabled = false
        binding.materialButtonMenu?.setOnClickListener { MenuPopup(this).show() }
        binding.materialButtonHome?.setOnClickListener { HomeLivedata.getInstance().Value(true) }
        binding.materialButtonTab?.setOnClickListener { TabPopup(this).show() }
        binding.materialButtonTab?.setOnLongClickListener {
            showTabLongClickMenu(it)
            true
        }
        binding.addButton?.setOnClickListener { HomeLivedata.getInstance().Value(true) }
        binding.popupCloseButton?.setOnClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        if (binding.content.viewPager.currentItem == 1) {

            binding.reloadButtonL?.isClickable = false
            binding.forwardButtonL?.isClickable = false
            binding.backButtonL?.isClickable = false
        } else {
            binding.backButtonL?.setOnClickListener {
                if (binding.user?.canBack == true)
                    binding.user?.session?.goBack()
                else RemoveTabLiveData.getInstance().Value(binding.user)
            }
            binding.forwardButtonL?.setOnClickListener {
                if (binding.user?.canForward == true)
                    binding.user?.session?.goForward()
            }
            binding.reloadButtonL?.setOnClickListener {
                binding.user?.session?.reload()
            }
        }
        binding.menuButton?.setOnClickListener {
            openMenu()
        }
        binding.addonsButton?.setOnClickListener {
            if (!isHome)
                BookmarkDialog(this, binding.user!!.mTitle, binding.user!!.u).show()
        }
        binding.content.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val c = ContextCompat.getColor(getContext(), R.color.surface)
                if (position == 0) {
                    binding.SearchText?.setText("")
                    StatusUtils.setStatusBarColor(
                        this@MainActivity,
                        c,
                        binding.root
                    )
                } else {
                    StatusUtils.setStatusBarColor(
                        this@MainActivity,
                        binding.user?.statusBarColor ?: c,
                        binding.root
                    )
                }
            }
        })


        DelegateLivedata.getInstance().observe(this) {
            binding.user = it
        }
        DelegateListLiveData.getInstance().observe(this) {
            binding.SizeText?.setText(it.size.toString())
            adapter.submitList(it.toList())
            sessionDelegates = it
            if (it.isEmpty()) {
                onProgressUpdate(ProgressEvent(100))
                WebExtensionRuntimeManager.clearSessions(this@MainActivity)
            }
        }
        HomeLivedata.getInstance().observe(this) {
            isHome = it
            if (it) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                val newTabUrl = WebExtensionRuntimeManager.findHomePageUrl()
                if (newTabUrl.isNullOrEmpty()) {
                    binding.content.viewPager.currentItem = 0
                    binding.urlText?.setText("")
                } else {
                    val list = DelegateListLiveData.getInstance().value ?: emptyList()
                    for (sessionDelegate in list) {
                        if (sessionDelegate.u == newTabUrl) {
                            if (DelegateLivedata.getInstance().value != sessionDelegate) {
                                DelegateLivedata.getInstance().Value(sessionDelegate)
                            }
                            return@observe
                        }
                    }
                    createSession(newTabUrl, this@MainActivity)
                }
            } else {
                binding.content.viewPager.currentItem = 1
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        AddTabLiveData.getInstance().observe(this) {
            binding.recyclerView2?.smoothScrollToPosition(it)
        }


//        if (getSizeName(this) == "large")
//            FullScreen(this)

        val uri: Uri? = intent?.data
        if (uri != null) {
            createSession(uri.toString(), this)
        }
        //初始化一下
        WebExtensionRuntimeManager
        ThreadTool.async {
            val fileDirPath: String =
                UriUtilsPro.getRootDir(getContext()) + File.separator + "_cache"
            if (File(fileDirPath).exists()) {
                FileUtil.deleteDirs(fileDirPath)
            }
        }
        initFloatVideo()
    }

    private fun showTabLongClickMenu(v: View?) {
        if (v == null) {
            return
        }
        if (DelegateListLiveData.getInstance().value.isNullOrEmpty()) {
            return
        }
        val popup = PopupMenu(getContext(), v)
        popup.menuInflater.inflate(R.menu.tab_btn_long_click_menu, popup.menu)
        if (HomeLivedata.getInstance().value == true) {
            popup.menu.removeItem(R.id.tab_open_one)
            popup.menu.removeItem(R.id.tab_remove)
            popup.menu.removeItem(R.id.tab_remove_other)
        }
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.tab_open_one -> {
                    HomeLivedata.getInstance().Value(true)
                }

                R.id.tab_remove -> {
                    DelegateLivedata.getInstance().value?.let { sessionDelegate ->
                        RemoveTabLiveData.getInstance().Value(sessionDelegate)
                    }
                }

                R.id.tab_remove_other -> {
                    DelegateLivedata.getInstance().value?.let { sessionDelegate ->
                        val webFragment = getWebFragment()
                        webFragment?.removeSessionDelegates(sessionDelegate)
                        ToastMgr.shortBottomCenter(getContext(), getString(R.string.excute_success))
                    }
                }

                R.id.tab_remove_all -> {
                    val webFragment = getWebFragment()
                    webFragment?.removeSessionDelegates(null)
                    ToastMgr.shortBottomCenter(getContext(), getString(R.string.excute_success))
                }
            }
            true
        }
        popup.show()
    }


    override fun onFindVideo(session: SessionDelegate, request: TabRequest) {
        Timber.d("onFindVideo: %s", request.url)
        if (DelegateLivedata.getInstance().value == session && floatVideoController != null && !UrlDetector.isMusic(
                request.url
            )
        ) {
            //videoEvent.getMediaResult().setClicked(true);
            val uri: String = session.u
            if (WebExtensionRuntimeManager.isFloatVideoBlockDom(uri)) {
                return
            }
            floatVideoController!!.show(
                request.url,
                uri,
                session.mTitle,
                request.requestHeaderMap,
                true
            )
        }
    }

    @Subscribe
    fun onFloatPlay(event: FloatPlayEvent) {
        val request = event.request
        if (floatVideoController != null) {
            val session = DelegateLivedata.getInstance().value
            val uri: String = request.documentUrl ?: session?.u ?: request.url!!
            floatVideoController!!.show(
                request.url,
                uri,
                session?.mTitle ?: request.url,
                request.requestHeaderMap,
                true
            )
        } else {
            val context = getContext()
            val c = ContextCompat.getString(context, R.string.open)
            val m = ContextCompat.getString(context, R.string.float_video_closed)
            MaterialAlertDialogBuilder(context)
                .setTitle(ContextCompat.getString(context, R.string.float_video))
                .setMessage(getString(R.string.float_video_message, m))
                .setPositiveButton(c) { d, _ ->
                    PreferenceMgr.put(getContext(), "float_xiutan", true)
                    if (floatVideoController == null) {
                        initFloatVideo()
                    }
                    ToastMgr.shortBottomCenter(getContext(), "已开启悬浮嗅探")
                    onFloatPlay(event)
                    d.dismiss()
                }.setNegativeButton(getString(R.string.cancel)) { d, _ ->
                    d.dismiss()
                }.show()
        }
    }

    @Subscribe
    fun changeFloatVideoSwitch(event: FloatVideoSwitchEvent) {
        changeFloatVideoSwitch()
    }

    fun changeFloatVideoSwitch() {
        val now = PreferenceMgr.getBoolean(getContext(), "float_xiutan", false)
        PreferenceMgr.put(getContext(), "float_xiutan", !now)
        if (now) {
            //需要关闭
            if (floatVideoController != null) {
                floatVideoController?.destroy()
                floatVideoController = null
            }
            ToastMgr.shortBottomCenter(getContext(), "已关闭悬浮嗅探")
        } else {
            //需要开启
            if (floatVideoController == null) {
                initFloatVideo()
            }
            ToastMgr.shortBottomCenter(getContext(), "已开启悬浮嗅探")
        }
    }

    private fun initFloatVideo() {
        if (!PreferenceMgr.getBoolean(getContext(), "float_xiutan", false)) {
            return
        }
        if (binding.containerView == null) {
            return
        }
        floatVideoController = FloatVideoController(this,
            binding.containerView!!,
            { pause: Boolean, force: Boolean? ->
                if (pause) {
                    val delegate = DelegateLivedata.getInstance().value
                    if (delegate?.activeMediaSession != null) {
                        delegate?.activeMediaSession?.pause()
                    }
                }
                0
            },
            object : VideoDetector {
                override fun putIntoXiuTanLiked(context: Context, dom: String, url: String) {}
                override fun getDetectedMediaResults(
                    webUrl: String,
                    mt: MediaType
                ): List<DetectedMediaResult> {
                    if (mt != MediaType.VIDEO) {
                        return emptyList()
                    }
                    val fragment = getWebFragment()
                    fragment?.let { webFragment ->
                        if (StringUtils.equals(
                                webUrl,
                                webFragment.sessiondelegate.u
                            ) || StringUtil.isEmpty(webUrl) || "null" == webUrl
                        ) {
                            return webFragment.sessiondelegate.requests.filter { it.type == TabRequest.VIDEO }
                                .map {
                                    DetectedMediaResult(
                                        it.url
                                    ).apply {
                                        mediaType = Media(MediaType.VIDEO)
                                        headers = it.requestHeaderMap
                                    }
                                }
                        }
                    }
                    for (sessionDelegate in sessionDelegates) {
                        if (StringUtils.equals(webUrl, sessionDelegate.u)) {
                            return sessionDelegate.requests.filter { it.type == TabRequest.VIDEO }
                                .map {
                                    DetectedMediaResult(
                                        it.url
                                    ).apply {
                                        mediaType = Media(MediaType.VIDEO)
                                        headers = it.requestHeaderMap
                                    }
                                }
                        }
                    }
                    return emptyList()
                }
            },
            object : WebHolder {
                override fun getRequestMap(): MutableMap<String, MutableMap<String, String?>?> {
                    return hashMapOf()
                }
            })
    }

    @Subscribe
    fun showOpenAppIntent(event: OpenAppIntentEvent) {
        val holder = VarHolder(false)
        make(
            getWebFragment()?.getCoordinatorLayout() ?: binding.root,
            getString(R.string.intent_message),
            Snackbar.LENGTH_LONG
        )
            .setAction(getString(R.string.open)) { v -> holder.data = true }
            .setCancelButton(getString(R.string.refuse)) { view ->
                holder.data = false
            }.addCallback(object : BaseTransientBottomBar.BaseCallback<ChefSnackbar?>() {
                override fun onDismissed(
                    transientBottomBar: ChefSnackbar?,
                    e: Int
                ) {

                    if (holder.data) {
                        event.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(event.intent)
                        event.result.complete(AllowOrDeny.ALLOW)
                    } else {
                        event.result.complete(AllowOrDeny.DENY)
                    }
                    super.onDismissed(transientBottomBar, e)
                }
            }).show()
    }

    override fun onPause() {
        super.onPause()
        floatVideoController?.onPause()
        DelegateLivedata.getInstance().value?.pause()
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Timber.d("onRestoreInstanceState")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        if (SeRuSettings.mKillProcessOnDestroy) {
            postDelayed(500) { Process.killProcess(Process.myPid()) }
        }
        App.setHomeActivity(null)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun addWebExtension(event: WebExtensionsAddEvent) {
        Log.d("open", "open addWebExtension: ")
        WebextensionSession(this).install("file://${event.path}")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updateWebExtension(event: WebExtensionsEnableEvent) {
        if (event.isEnabled) {
            event.webExtension.addDelegate(this)
        } else {
            event.webExtension.removeDelegate()
        }
    }

    @Subscribe
    fun updateWebExtensionConnectPort(event: WebExtensionConnectPortEvent) {
        port = event.port
    }

    fun evaluateJavaScript(code: String) {
        val jsonObject = JSONObject()
        jsonObject.put("type", "evaluateJavaScript")
        jsonObject.put("code", code)
        port?.postMessage(jsonObject)
    }

    @Subscribe
    fun evalJS(event: EvalJSEvent) {
        evaluateJavaScript(event.code)
    }

    fun showEruda() {
        val jsonObject = JSONObject()
        jsonObject.put("type", "eruda")
        port?.postMessage(jsonObject)
    }

    @Subscribe
    fun onRequestEvent(event: TabRequestEvent) {
        val fragment = getWebFragment()
        val request = TabRequest(event.json)
        //Log.d("test", "onMessage: " + request.url + ", documentUrl: " + request.documentUrl)
        fragment?.let { webFragment ->
            if (StringUtils.equals(
                    request.documentUrl,
                    webFragment.sessiondelegate.u
                ) || StringUtil.isEmpty(request.documentUrl) || "null" == request.documentUrl
            ) {
                webFragment.sessiondelegate.onRequest(request)
                return
            }
        }
        for (sessionDelegate in sessionDelegates) {
            if (StringUtils.equals(request.documentUrl, sessionDelegate.u)) {
                sessionDelegate.onRequest(request)
                return
            }
        }
    }

    private var inputHeightListener: InputHeightListenPostEvent? = null

    @Subscribe
    fun inputHeightListenPost(event: InputHeightListenPostEvent) {
        if (DelegateLivedata.getInstance().value != null) {
            val delegate = DelegateLivedata.getInstance().value!!
            if (delegate.u.startsWith("moz-extension") && (
                        delegate.mTitle.contains("标签页") ||
                                delegate.mTitle.contains("起始页") ||
                                delegate.mTitle.endsWith("Tab")
                        )
            ) {
                event.isUp.data = true
                event.countDownLatch.countDown()
                return
            }
        }
        inputHeightListener = event
        val jsonObject = JSONObject()
        jsonObject.put("type", "inputHeight")
        port?.postMessage(jsonObject)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onInputHeightCallback(event: InputHeightListenEvent) {
        try {
            Log.d("test", "onInputHeightCallback: " + event.isUp)
            inputHeightListener?.isUp?.data = event.isUp
            inputHeightListener?.countDownLatch?.countDown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun browse(event: BrowseEvent) {
        searching(event.url)
    }

    @Subscribe
    fun onProgressUpdate(event: ProgressEvent) {
        ThreadTool.runOnUI {
            binding.progress?.setWebProgress(event.progress)
        }
    }

    @Subscribe
    fun showSearch(event: ShowSearchEvent) {
        binding.SearchText?.requestFocus()
        try {
            (Objects.requireNonNull(getSystemService(INPUT_METHOD_SERVICE)) as InputMethodManager)
                .showSoftInput(
                    binding.SearchText!!,
                    InputMethodManager.SHOW_FORCED
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getContext(): Context {
        return this
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {


        return super.onCreateView(name, context, attrs)
    }

    private fun getWebFragment(): WebFragment? {
        return if (fragments.isNullOrEmpty() || fragments!!.size < 2) null else fragments!![1] as WebFragment
    }

    private val onBackPress = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                return
            }
            if (binding.content.viewPager.currentItem == 1 && getWebFragment()?.isErrorShown() == true && binding.user?.session != null) {
                binding.user?.session?.reload()
                return
            }
            if (floatVideoController != null && floatVideoController!!.onBackPressed()) {
                return
            }
            if (binding.content.viewPager.currentItem == 0 && sessionDelegates.isNotEmpty()) {
                HomeLivedata.getInstance().Value(false)
            } else if (binding.user?.isFull == true) {
                binding.user?.session?.exitFullScreen()
            } else if (binding.user?.canBack == true) {
                binding.user?.session?.goBack()
            } else {
                if (sessionDelegates.indexOf(binding.user) != -1)
                    RemoveTabLiveData.getInstance().Value(binding.user)
                else {
                    finish()
                }
            }
        }
    }

    fun showPage(url: String) {
        when (url) {
            "hiker://bookmark" -> {
                if (binding.drawer != null) {
                    openMenu("bookmark")
                } else {
                    BookmarkPopup(this).show()
                }
            }

            "hiker://download" -> {
                if (binding.drawer != null) {
                    openMenu("download")
                } else {
                    val intent = Intent(getContext(), HolderActivity::class.java)
                    intent.putExtra("Page", "DOWNLOAD")
                    intent.putExtra("downloaded", true)
                    startActivity(intent)
                }
            }

            "hiker://history" -> {
                if (binding.drawer != null) {
                    openMenu("history")
                } else {
                    HistoryPopup(this).show()
                }
            }
        }
    }

    private fun getDestId(item: MenuItem): Int {
        return when (item.title) {
            getString(R.string.download) -> R.id.download
            getString(R.string.settings) -> R.id.settings
            getString(R.string.bookmark) -> R.id.bookmark
            getString(R.string.history) -> R.id.history
            getString(R.string.addons) -> R.id.addons
            else -> R.id.settings
        }
    }

    private fun openMenu(dest: String? = null) {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        val navController = findNavController(R.id.fragmentContainerView3)
        //不用NavigationUI是因为有时候有问题
        binding.navigationrail?.let { navigationBarView ->
            navigationBarView.headerView?.findViewById<FloatingActionButton>(R.id.floatingActionButton)?.visibility =
                if (isHome) View.GONE else View.VISIBLE
            binding.navigationrail?.headerView?.findViewById<FloatingActionButton>(R.id.floatingActionButton)
                ?.setOnClickListener {
                    navController.navigate(R.id.addonsPopupFragment2)
                }
            if (navigationBarView.tag != null) {
                return@let
            }
            navigationBarView.tag = true
            navigationBarView.setOnItemSelectedListener { item ->
                val id = getDestId(item)
                if (navController.currentDestination?.id != id) {
                    navController.navigate(id)
                }
                true
            }
            navController.addOnDestinationChangedListener(
                object : NavController.OnDestinationChangedListener {
                    override fun onDestinationChanged(
                        controller: NavController,
                        destination: NavDestination,
                        arguments: Bundle?
                    ) {
                        val view = binding.navigationrail
                        if (view == null) {
                            navController.removeOnDestinationChangedListener(this)
                            return
                        }
                        if (destination is FloatingWindow) {
                            return
                        }
                        view.menu.forEach { item ->
                            if (destination.hierarchy.any { it.id == getDestId(item) }) {
                                item.isChecked = true
                            }
                        }
                    }
                })
        }
        binding.navigationrail?.post {
            when (dest) {
                "download" -> {
                    navController.navigate(R.id.download)
                }

                "bookmark" -> {
                    navController.navigate(R.id.bookmark)
                }

                "history" -> {
                    navController.navigate(R.id.history)
                }
            }
        }
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.hasExtra("onClick") == true) {
            val notification = intent.extras!!.getParcelable<WebNotification>("onClick")
            if (notification != null) {
                intent.removeExtra("onClick")
                notification.click()
            }
        }
        val uri: Uri? = intent?.data
        if (uri != null) {
            createSession(uri.toString(), this)
        }
        GeckoRuntime.getDefault(this).activityDelegate = GeckoRuntime.ActivityDelegate {
            Log.d("test", uri.toString())
            GeckoResult.fromValue(Intent())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (SeRuSettings.mPendingActivityResult.containsKey(requestCode)) {
            val result: GeckoResult<Intent>? =
                SeRuSettings.mPendingActivityResult.remove(requestCode)
            if (resultCode == RESULT_OK) {
                result?.complete(data)
            } else {
                result?.completeExceptionally(RuntimeException("Unknown error"))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        //binding.user?.resume()
        floatVideoController?.onResume()
        org.mozilla.xiu.browser.utils.SoftKeyBoardListener.setListener(
            this,
            object : OnSoftKeyBoardChangeListener {
                override fun keyBoardShow(height: Int) {
                }

                override fun keyBoardHide(height: Int) {
                    binding.constraintLayout10?.visibility = View.GONE

                    binding.bottomMotionLayout?.transitionToStart()
                    binding.SearchText?.clearFocus()
                }
            })
        DelegateLivedata.getInstance().value?.resume()
    }

    fun searching(value: String) {
        if (StrUtil.isGeckoUrl(value)) {
            if (binding.content.viewPager.currentItem == 1)
                binding.user?.session?.loadUri(value)
            else
                createSession(value, this)
        } else {
            if (binding.content.viewPager.currentItem == 1)
                binding.user?.session?.loadUri("${org.mozilla.xiu.browser.broswer.SearchEngine(this)}$value")
            else
                createSession("${org.mozilla.xiu.browser.broswer.SearchEngine(this)}$value", this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ExamplePermissionDelegate.REQUEST_PERMISSIONS) {
            val permission = binding.user?.session?.permissionDelegate as ExamplePermissionDelegate?
            permission?.onRequestPermissionsResult(permissions, grantResults)
        }
    }
}