package org.mozilla.xiu.browser


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.sidesheet.SideSheetBehavior
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.xiu.browser.broswer.dialog.SearchDialog
import org.mozilla.xiu.browser.broswer.home.TipsAdapter
import org.mozilla.xiu.browser.componets.BookmarkDialog
import org.mozilla.xiu.browser.componets.CollectionAdapter
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.componets.popup.MenuPopup
import org.mozilla.xiu.browser.componets.popup.TabPopup
import org.mozilla.xiu.browser.database.history.HistoryViewModel
import org.mozilla.xiu.browser.databinding.ActivityMainBinding
import org.mozilla.xiu.browser.databinding.PrivacyAgreementLayoutBinding
import org.mozilla.xiu.browser.session.*
import org.mozilla.xiu.browser.tab.AddTabLiveData
import org.mozilla.xiu.browser.tab.DelegateListLiveData
import org.mozilla.xiu.browser.tab.RemoveTabLiveData
import org.mozilla.xiu.browser.tab.TabListAdapter
import org.mozilla.xiu.browser.utils.FileUtil
import org.mozilla.xiu.browser.utils.SoftKeyBoardListener.OnSoftKeyBoardChangeListener
import org.mozilla.xiu.browser.utils.StatusUtils
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.UriUtilsPro
import org.mozilla.xiu.browser.webextension.BrowseEvent
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
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var privacyAgreementLayoutBinding: PrivacyAgreementLayoutBinding
    var fragments = listOf<Fragment>(HomeFragment(), WebFragment { full -> fullScreenCall(full) })
    private lateinit var geckoViewModel: GeckoViewModel
    var sessionDelegates = ArrayList<SessionDelegate>()
    private val adapter = TabListAdapter()
    var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null
    lateinit var sideSheetBehavior: SideSheetBehavior<ConstraintLayout>
    var isHome: Boolean = true
    lateinit var historyViewModel: HistoryViewModel
    var searching = ""

    private fun fullScreenCall(fullScreen: Boolean) {
        if (fullScreen) {
            StatusUtils.setStatusBarVisibility(this, false, binding.containerView!!)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            StatusUtils.setStatusBarVisibility(this, true, binding.containerView!!)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        if (binding.content.drawer != null) {
            bottomSheetBehavior =
                BottomSheetBehavior.from(binding.content.drawer as ConstraintLayout)
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
                binding.bottomMotionLayout?.transitionToEnd()
                binding.constraintLayout10?.visibility = View.VISIBLE
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
        binding.SearchText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                var s1 = s.toString().trim()
                if (s1 != "") {
                    historyViewModel.findHistoriesWithMix(s1)?.observe(this@MainActivity) {
                        tipsAdapter.submitList(it)
                    }
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
        binding.content.viewPager.adapter = CollectionAdapter(this, fragments)
        binding.content.viewPager.isUserInputEnabled = false
        binding.materialButtonMenu?.setOnClickListener { MenuPopup(this).show() }
        binding.materialButtonHome?.setOnClickListener { HomeLivedata.getInstance().Value(true) }
        binding.materialButtonTab?.setOnClickListener { TabPopup(this).show() }
        binding.addButton?.setOnClickListener { HomeLivedata.getInstance().Value(true) }
        binding.content.popupCloseButton?.setOnClickListener {
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
                else RemoveTabLiveData.getInstance().Value(sessionDelegates.indexOf(binding.user))
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
            }
        }
        HomeLivedata.getInstance().observe(this) {
            isHome = it
            if (it) {
                binding.content.viewPager.currentItem = 0
                binding.urlText?.setText("")
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
        ThreadTool.async {
            val fileDirPath: String =
                UriUtilsPro.getRootDir(getContext()) + File.separator + "_cache"
            if (File(fileDirPath).exists()) {
                FileUtil.deleteDirs(fileDirPath)
            }
        }
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

    private val onBackPress = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.content.viewPager.currentItem == 0 && sessionDelegates.isNotEmpty()) {
                HomeLivedata.getInstance().Value(false)
            } else if (binding.user?.isFull == true) {
                binding.user?.session?.exitFullScreen()
            } else if (binding.user?.canBack == true) {
                binding.user?.session?.goBack()
            } else {
                if (sessionDelegates.indexOf(binding.user) != -1)
                    RemoveTabLiveData.getInstance().Value(sessionDelegates.indexOf(binding.user))
                else {
                    finish()
                }
            }
        }
    }

    private fun openMenu() {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        var navController = findNavController(R.id.fragmentContainerView3)
        binding.content.navigationrail?.setupWithNavController(navController)
        binding.content.appbar?.setupWithNavController(
            navController,
            AppBarConfiguration(navController.graph)
        )
        if (isHome)
            binding.content.navigationrail?.headerView?.findViewById<FloatingActionButton>(R.id.floatingActionButton)?.visibility =
                View.GONE
        else
            binding.content.navigationrail?.headerView?.findViewById<FloatingActionButton>(R.id.floatingActionButton)?.visibility =
                View.VISIBLE

        binding.content.navigationrail?.headerView?.findViewById<FloatingActionButton>(R.id.floatingActionButton)
            ?.setOnClickListener {
                navController.navigate(R.id.addonsPopupFragment2)
            }


    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uri: Uri? = intent?.data
        if (uri != null) {
            createSession(uri.toString(), this)
        }
        GeckoRuntime.getDefault(this).activityDelegate = GeckoRuntime.ActivityDelegate {
            Log.d("test", uri.toString())
            GeckoResult.fromValue(Intent())
        }
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onResume() {
        super.onResume()
        //binding.user?.resume()

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
    }

    fun searching(value: String) {
        if (Patterns.WEB_URL.matcher(value)
                .matches() || URLUtil.isValidUrl(value) || value.startsWith("about:")
        ) {
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
        if (requestCode == ExamplePermissionDelegate.REQUEST_PERMISSIONS) {
            val permission = binding.user?.session?.permissionDelegate as ExamplePermissionDelegate?
            permission?.onRequestPermissionsResult(permissions, grantResults)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}