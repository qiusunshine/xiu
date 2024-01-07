package org.mozilla.xiu.browser.broswer.bookmark.sync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.sync.SyncReason
import org.greenrobot.eventbus.EventBus
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.databinding.FragmentSyncBookmarkBinding
import org.mozilla.xiu.browser.fxa.AccountManagerCollection
import org.mozilla.xiu.browser.fxa.Fxa
import org.mozilla.xiu.browser.session.GeckoViewModel
import org.mozilla.xiu.browser.session.SeRuSettings
import org.mozilla.xiu.browser.utils.ToastMgr

/**
 * A simple [Fragment] subclass.
 * Use the [SyncBookmarkFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SyncBookmarkFragment : Fragment() {

    lateinit var binding: FragmentSyncBookmarkBinding
    lateinit var bookmarkNodes: ArrayList<BookmarkNode>
    lateinit var geckoViewModel: GeckoViewModel
    private var accountManager: FxaAccountManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // GlobalSyncableStoreProvider.configureStore(SyncEngine.Bookmarks to bookmarksStorage)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bookmarkNodes = ArrayList()
        binding = FragmentSyncBookmarkBinding.inflate(LayoutInflater.from(context))
        var bookmarkAdapter = SyncBookmarkFolderAdapter()
        geckoViewModel = activity?.let { ViewModelProvider(it)[GeckoViewModel::class.java] }!!

        lifecycleScope.launch {
            val accountManagerCollection =
                ViewModelProvider(requireActivity())[AccountManagerCollection::class.java]
            accountManagerCollection.data.collect {
                accountManager = it
            }
        }
        lifecycleScope.launch {
            binding.syncBookmarkRecyclerView.adapter = bookmarkAdapter
            binding.syncBookmarkRecyclerView.layoutManager = LinearLayoutManager(context)
            bookmarkAdapter.select = object : SyncBookmarkFolderAdapter.Select {
                override fun onSelect(bean: BookmarkNode) {
                    //createSession(url)
                    if (bean.type.name == "FOLDER") {
                        val bundle = Bundle()
                        bundle.putString("guid", bean.guid)
                        findNavController().navigate(
                            R.id.action_syncBookmarkFragment_to_syncBookmarkListFragment,
                            bundle
                        )
                    } else if (!bean.url.isNullOrEmpty()) {
                        createSession(bean.url!!)
                    }
                }
            }
            bookmarkAdapter.submitList(withContext(Dispatchers.IO) {
                val bookmarksRoot =
                    Fxa.bookmarksStorage.value.getTree("root________", recursive = true)
                if (bookmarksRoot == null) {
                    bookmarkNodes
                } else {
                    bookmarksRoot.children?.forEach {
                        bookmarkNodes.add(it)
                    }
                    bookmarkNodes
                }
            }.toList())

            binding.constraintLayout116.setOnClickListener {
                lifecycleScope.launch {
                    accountManager?.syncNow(SyncReason.User)
                    val success = accountManager?.authenticatedAccount()
                        ?.deviceConstellation()
                        ?.pollForCommands()
                    if (accountManager?.authenticatedAccount() == null) {
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
                            EventBus.getDefault().post(SyncRefreshEvent())
                        }
                    }
                }
            }
        }

        return binding.root
    }

    fun createSession(uri: String) {
        val session = GeckoSession()
        val sessionSettings = session.settings
        SeRuSettings(sessionSettings, requireActivity())
        context?.let { GeckoRuntime.getDefault(it) }?.let { session.open(it) }
        session.loadUri(uri)
        geckoViewModel.changeSearch(session)
        HomeLivedata.getInstance().Value(false)
    }
}