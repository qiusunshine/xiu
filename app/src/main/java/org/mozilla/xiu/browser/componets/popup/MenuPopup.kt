package org.mozilla.xiu.browser.componets.popup

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.StorageController.ClearFlags.ALL
import org.mozilla.xiu.browser.HolderActivity
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.base.addOnBackPressed
import org.mozilla.xiu.browser.componets.BookmarkDialog
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.componets.MenuAddonsAdapater
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.database.bookmark.BookmarkViewModel
import org.mozilla.xiu.browser.databinding.PopupMenuBinding
import org.mozilla.xiu.browser.session.DelegateLivedata
import org.mozilla.xiu.browser.session.PrivacyModeLivedata
import org.mozilla.xiu.browser.session.SeRuSettings
import org.mozilla.xiu.browser.session.SessionDelegate
import org.mozilla.xiu.browser.utils.PreferenceMgr
import org.mozilla.xiu.browser.utils.ShareUtil
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.webextension.WebExtensionRuntimeManager


class MenuPopup {
    val context: MainActivity
    private var bottomSheetDialog: BottomSheetDialog
    var binding: PopupMenuBinding
    private var bookmarkViewModel: BookmarkViewModel
    private var sessionDelegate: SessionDelegate? = null
    var isHome: Boolean = true
    private var homeObserver: Observer<Boolean>
    private var delegateLiveObserver: Observer<SessionDelegate>
    private var bookmarkObserver: Observer<List<Bookmark?>?>
    private var bookmarkValueLiveData: LiveData<List<Bookmark?>?>? = null
    private var privacyObserver: Observer<Boolean>

    constructor(
        context: MainActivity,
    ) {
        this.context = context
        bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
        val onBackPressedCallback = bottomSheetDialog.addOnBackPressed {
            false
        }
        bookmarkViewModel =
            ViewModelProvider(context).get(BookmarkViewModel::class.java)
        binding = PopupMenuBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        homeObserver = Observer {
            isHome = it
            val newTabUrl = WebExtensionRuntimeManager.findHomePageUrl()
            if (!newTabUrl.isNullOrEmpty()) {
                //用了主页标签扩展程序，始终显示扩展程序和标题
                isHome = false
            }
            if (isHome) {
                binding.constraintLayout5.visibility = View.GONE
            } else {
                binding.constraintLayout5.visibility = View.VISIBLE
            }
        }
        binding.starButton.setOnClickListener {
            if (sessionDelegate != null) {
                BookmarkDialog(context, sessionDelegate!!.mTitle, sessionDelegate!!.u, sessionDelegate!!.icon).show()
            }
        }
        bookmarkObserver = Observer {
            if (!it.isNullOrEmpty()) {
                val bookmark = it[0]!!
                binding.starButton.icon = ContextCompat.getDrawable(context, R.drawable.star_fill)
                binding.starButton.setOnClickListener {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(context.getString(R.string.remove_bookmark_confirm))
                        .setNegativeButton(context.getString(R.string.cancel)) { _, _ -> }
                        .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                            bookmarkViewModel.deleteBookmarks(bookmark)
                            binding.starButton.icon =
                                ContextCompat.getDrawable(context, R.drawable.star2)
                        }
                        .show()
                }
            } else {
                binding.starButton.icon = ContextCompat.getDrawable(context, R.drawable.star2)
                binding.starButton.setOnClickListener {
                    if (sessionDelegate != null) {
                        BookmarkDialog(
                            context,
                            sessionDelegate!!.mTitle,
                            sessionDelegate!!.u,
                            sessionDelegate!!.icon,
                        ).show()
                    }
                }
            }
        }
        delegateLiveObserver = Observer {
            sessionDelegate = it
            binding.user = sessionDelegate
            if (it.u.isNotEmpty()) {
                bookmarkValueLiveData = bookmarkViewModel.findBookmarksWithUrl(sessionDelegate!!.u)
                bookmarkValueLiveData?.observe(context, bookmarkObserver)
            }
        }
        privacyObserver = Observer {
            if (it) {
                binding.privacyButton.icon = context.getDrawable(R.drawable.icon_privacy_fill)
            } else {
                binding.privacyButton.icon = context.getDrawable(R.drawable.icon_privacy)
            }
        }
        HomeLivedata.getInstance().observe(context, homeObserver)
        DelegateLivedata.getInstance().observe(context, delegateLiveObserver)
        PrivacyModeLivedata.getInstance().observe(context, privacyObserver)
        bottomSheetDialog.setOnDismissListener {
            onBackPressedCallback.remove()
            HomeLivedata.getInstance().removeObserver(homeObserver)
            DelegateLivedata.getInstance().removeObserver(delegateLiveObserver)
            bookmarkValueLiveData?.removeObserver(bookmarkObserver)
            PrivacyModeLivedata.getInstance().removeObserver(privacyObserver)
        }
        binding.materialButton16.setOnClickListener {
            binding.constraintLayout13.setTransition(R.id.top_start, R.id.top_end)
            if (binding.constraintLayout13.targetPosition == 1f) {
                val j = binding.constraintLayout13::class.java
                val f = j.getDeclaredField("mTransitionLastPosition")
                f.isAccessible = true
                f.set(binding.constraintLayout13, 1f)
                binding.constraintLayout13.transitionToStart()
            } else {
                binding.constraintLayout13.transitionToEnd()
            }
        }
        binding.toolsButton.setOnClickListener {
            binding.constraintLayout13.setTransition(R.id.tools_start, R.id.tools_end)
            if (binding.constraintLayout13.targetPosition == 1f) {
                val j = binding.constraintLayout13::class.java
                val f = j.getDeclaredField("mTransitionLastPosition")
                f.isAccessible = true
                f.set(binding.constraintLayout13, 1f)
                binding.constraintLayout13.transitionToStart()
            } else {
                binding.constraintLayout13.transitionToEnd()
            }
        }
        binding.downloadButton.setOnClickListener {
            val intent = Intent(context, HolderActivity::class.java)
            intent.putExtra("Page", "DOWNLOAD")
            context.startActivity(intent)
            bottomSheetDialog.dismiss()
        }
        binding.settingButton.setOnClickListener {
            val intent = Intent(context, HolderActivity::class.java)
            intent.putExtra("Page", "SETTINGS")
            context.startActivity(intent)
            bottomSheetDialog.dismiss()
        }
        binding.privacyButton.setOnClickListener {
            val isPrivacy = PrivacyModeLivedata.getInstance().value ?: false
            if (isPrivacy) {
                PreferenceMgr.remove(context, "privacyMode")
                PrivacyModeLivedata.getInstance().Value(false)
                ToastMgr.shortBottomCenter(context, context.getString(R.string.private_mode_closed))
            } else {
                PreferenceMgr.put(context, "privacyMode", true)
                PrivacyModeLivedata.getInstance().Value(true)
                ToastMgr.shortBottomCenter(context, context.getString(R.string.private_mode_opened))
            }
            bottomSheetDialog.dismiss()
        }
        binding.bookmarkButton.setOnClickListener {
            BookmarkPopup(context).show()
            bottomSheetDialog.dismiss()

        }
        binding.historyButton.setOnClickListener {
            HistoryPopup(context).show()
            bottomSheetDialog.dismiss()
        }

        binding.shareButton.setOnClickListener {
            if (sessionDelegate != null) {
                ShareUtil.shareText(context, sessionDelegate!!.mTitle + "\n" + sessionDelegate!!.u)
            }
            bottomSheetDialog.dismiss()
        }

        binding.reloadBotton.setOnClickListener {
            if (!isHome)
                sessionDelegate?.session?.reload()
            bottomSheetDialog.dismiss()

        }
        binding.forwardButton.setOnClickListener {
            if (!isHome)
                sessionDelegate?.session?.goForward()
            bottomSheetDialog.dismiss()
        }
        binding.dataClearingButton.setOnClickListener {
            sessionDelegate?.let { it1 ->
                GeckoRuntime.getDefault(context).storageController.clearDataFromHost(
                    it1.secureHost, ALL
                )
                it1.session.reload()

            }
            bottomSheetDialog.dismiss()
        }
        binding.modeBotton.setOnClickListener {
            if (!isHome) {
                if (SeRuSettings.desktopMode) {
                    SeRuSettings.desktopMode = false
                    //sessionDelegate!!.session.settings.userAgentOverride = null
                    sessionDelegate!!.session.settings.userAgentMode =
                        GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                    sessionDelegate!!.session.settings.viewportMode =
                        GeckoSessionSettings.VIEWPORT_MODE_MOBILE
                    sessionDelegate!!.session.reload()
                    ToastMgr.shortBottomCenter(context, context.getString(R.string.desktop_closed))
                } else {
                    SeRuSettings.desktopMode = true
                    //sessionDelegate!!.session.settings.userAgentOverride = DESKTOP_UA
                    sessionDelegate!!.session.settings.userAgentMode =
                        GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                    sessionDelegate!!.session.settings.viewportMode =
                        GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                    sessionDelegate!!.session.reload()
                    ToastMgr.shortBottomCenter(context, context.getString(R.string.desktop_opened))
                }
            }
            bottomSheetDialog.dismiss()
        }

        val adapter = MenuAddonsAdapater {
            bottomSheetDialog.dismiss()
        }
        binding.menuAddonsRecyclerView.layoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false);
        binding.menuAddonsRecyclerView.adapter = adapter
        GeckoRuntime.getDefault(context).webExtensionController.list().accept {
            if (it != null) {
                if (it.size != 0 && !isHome)
                    binding.addonContainer.visibility = View.VISIBLE
                else
                    binding.addonContainer.visibility = View.GONE
                var webExtensions = it
                for (e in it) {
                    if (!e.metaData.enabled)
                        if (webExtensions != null) {
                            webExtensions = webExtensions.toMutableList().apply { remove(e) }
                        }
                }
                adapter.submitList(webExtensions)
            }
        }

        binding.addonIcon.setOnClickListener {
            var intent = Intent(context, HolderActivity::class.java)
            intent.putExtra("Page", "ADDONS")
            context.startActivity(intent)
            bottomSheetDialog.dismiss()
        }
        binding.devButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            context.showEruda()
        }
        binding.floatButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            context.changeFloatVideoSwitch()
        }
        binding.domainButton.setOnClickListener {
            //ToastMgr.shortBottomCenter(context, "开发中，敬请期待")
            bottomSheetDialog.dismiss()
            DelegateLivedata.getInstance().value?.let {
                NetworkPopup(context, it).show()
            }
        }
    }

    fun show() {
        bottomSheetDialog.show()
    }
}