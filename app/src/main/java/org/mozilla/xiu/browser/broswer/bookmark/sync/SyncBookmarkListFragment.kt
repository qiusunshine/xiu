package org.mozilla.xiu.browser.broswer.bookmark.sync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.BookmarkNode
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.databinding.FragmentSyncBookmarkListBinding
import org.mozilla.xiu.browser.fxa.Fxa
import org.mozilla.xiu.browser.session.createSession
import java.util.Stack


class SyncBookmarkListFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var guid: String? = null
    private var bookmarksRoot: BookmarkNode? = null
    private var stack: Stack<BookmarkNode> = Stack()
    private lateinit var bookmarkNodes: ArrayList<BookmarkNode>
    lateinit var binding: FragmentSyncBookmarkListBinding
    private lateinit var syncBookmarkItemAdapter: SyncBookmarkItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        guid = arguments?.getString("guid")
        bookmarkNodes = ArrayList()
        binding = FragmentSyncBookmarkListBinding.inflate(LayoutInflater.from(requireContext()))
        //guid?.let { Log.d("arguments?.getString", it) }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        syncBookmarkItemAdapter = SyncBookmarkItemAdapter()
        binding.constraintLayout16.setOnClickListener {
            if (stack.isEmpty()) {
                findNavController().navigate(R.id.action_syncBookmarkListFragment_to_syncBookmarkFragment)
            } else {
                stack.pop()
                if (stack.isEmpty()) {
                    findNavController().navigate(R.id.action_syncBookmarkListFragment_to_syncBookmarkFragment)
                } else {
                    loadData(stack.lastElement().guid)
                }
            }
        }
        lifecycleScope.launch {
            binding.recyclerView5.adapter = syncBookmarkItemAdapter
            binding.recyclerView5.layoutManager = LinearLayoutManager(context)
            syncBookmarkItemAdapter.select = object : SyncBookmarkItemAdapter.Select {
                override fun onSelect(bean: BookmarkNode) {
                    if (bean.type.name == "FOLDER") {
                        loadData(bean.guid)
                        stack.push(bean)
                    } else if (!bean.url.isNullOrEmpty()) {
                        createSession(bean.url!!, requireActivity())
                    }
                }
            }
            withContext(Dispatchers.IO) {
                guid?.let { loadData(it, true) }
            }
        }
        return binding.root
    }

    @Subscribe
    fun onSyncRefresh(event: SyncRefreshEvent) {
        lifecycleScope.launch(Dispatchers.IO) {
            guid?.let { loadData(it, true) }
        }
    }

    private fun loadData(uuid: String, push: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bookmarks = Fxa.bookmarksStorage.value.getTree(
                uuid,
                recursive = false
            )
            withContext(Dispatchers.Main) {
                if (bookmarks == null) {
                    syncBookmarkItemAdapter.submitList(null)
                } else {
                    val list = mutableListOf<BookmarkNode>()
                    //将文件夹放前面
                    for (child in bookmarks.children!!) {
                        if (child.type.name == "FOLDER") {
                            list.add(child)
                        }
                    }
                    for (child in bookmarks.children!!) {
                        if (child.type.name != "FOLDER") {
                            list.add(child)
                        }
                    }
                    syncBookmarkItemAdapter.submitList(list)
                    binding.recyclerView5.postDelayed({
                        binding.recyclerView5.scrollToPosition(0)
                    }, 50)
                    if (push) {
                        stack.push(bookmarks)
                    }
                }
            }
        }
    }
}