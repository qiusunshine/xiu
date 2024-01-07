package org.mozilla.xiu.browser.broswer.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.sync.SyncReason
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.base.async
import org.mozilla.xiu.browser.base.runOnUI
import org.mozilla.xiu.browser.componets.BookmarkDialog
import org.mozilla.xiu.browser.database.history.History
import org.mozilla.xiu.browser.database.shortcut.Shortcut
import org.mozilla.xiu.browser.database.shortcut.ShortcutViewModel
import org.mozilla.xiu.browser.databinding.FragmentHistoryBinding
import org.mozilla.xiu.browser.fxa.AccountManagerCollection
import org.mozilla.xiu.browser.fxa.Fxa
import org.mozilla.xiu.browser.session.createSession
import org.mozilla.xiu.browser.utils.ToastMgr

class SyncHistoryFragment : Fragment() {
    lateinit var binding: FragmentHistoryBinding
    lateinit var shortcutViewModel: ShortcutViewModel
    private var histories: List<History?>? = null
    lateinit var historyAdapter: HistoryAdapter
    var i: Int = 0
    private lateinit var accountManager: FxaAccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shortcutViewModel = ViewModelProvider(this)[ShortcutViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHistoryBinding.inflate(LayoutInflater.from(requireContext()))
        lifecycleScope.launch {
            val accountManagerCollection =
                ViewModelProvider(requireActivity())[AccountManagerCollection::class.java]
            accountManagerCollection.data.collect {
                accountManager = it
            }
        }
        historyAdapter = HistoryAdapter()
        binding.historyRecyclerview.adapter = historyAdapter
        binding.historyRecyclerview.layoutManager = LinearLayoutManager(context)
        loadData()
        binding.constraintLayout15.visibility = View.GONE
        binding.constraintLayout116.visibility = View.VISIBLE
        binding.constraintLayout116.setOnClickListener {
            lifecycleScope.launch {
                accountManager.syncNow(SyncReason.User)
                val success = accountManager.authenticatedAccount()
                    ?.deviceConstellation()
                    ?.pollForCommands()
                if (accountManager.authenticatedAccount() == null) {
                    ToastMgr.shortBottomCenter(
                        context,
                        ContextCompat.getString(requireContext(), R.string.login_msg)
                    )
                } else {
                    ToastMgr.shortBottomCenter(
                        context,
                        ContextCompat.getString(
                            requireContext(),
                            if (success == true) R.string.sync_success else R.string.sync_failed
                        )
                    )
                    if (success == true) {
                        loadData()
                    }
                }
            }
        }
        binding.HistoryFragmentSearching?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                var s1 = s.toString().trim()
                ToastMgr.shortBottomCenter(context, "暂不支持搜索")
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.materialButton20.setOnClickListener { showMenu(it) }

        historyAdapter.select = object : HistoryAdapter.Select {
            override fun onSelect(url: String) {
                createSession(url, requireActivity())
            }
        }
        historyAdapter.popupSelect = object : HistoryAdapter.PopupSelect {
            override fun onPopupSelect(bean: History, item: Int) {
                when (item) {
                    HistoryAdapter.DELETE -> {
                        async {
                            Fxa.historyStorage.value.deleteVisitsFor(bean.url)
                        }
                        histories = histories?.toMutableList()?.apply { remove(bean) }
                        historyAdapter.submitList(histories)
                    }

                    HistoryAdapter.ADD_TO_HOMEPAGE -> {
                        shortcutViewModel.insertShortcuts(
                            Shortcut(
                                bean.url,
                                bean.title,
                                System.currentTimeMillis().toInt(),
                                bean.icon
                            )
                        )
                        ToastMgr.shortBottomCenter(context, getString(R.string.excute_success))
                    }

                    HistoryAdapter.ADD_TO_BOOKMARK -> {
                        BookmarkDialog(
                            requireActivity(),
                            bean.title,
                            bean.url,
                            bean.icon,
                            oldBookmark = null
                        ).show()
                    }
                }
            }
        }
        return binding.root
    }

    private fun loadData() {
        async {
            val list = Fxa.historyStorage.value.getVisitsPaginated(0, 1000, listOf())
            val arr = list.map {
                History(it.url, it.title ?: it.url, it.visitTime.toInt(), "")
            }
            runOnUI {
                histories = arr
                historyAdapter.submitList(histories)
            }
        }
    }

    private fun showMenu(v: View) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(R.menu.history_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.history_menu_all_delete -> {
                    async {
                        Fxa.historyStorage.value.deleteEverything()
                    }
                    historyAdapter.submitList(null)
                }
            }
            false
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }
}