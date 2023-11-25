package org.mozilla.xiu.browser.broswer.bookmark

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

import org.mozilla.xiu.browser.broswer.history.HistoryAdapter
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.database.bookmark.BookmarkViewModel
import org.mozilla.xiu.browser.database.shortcut.Shortcut
import org.mozilla.xiu.browser.database.shortcut.ShortcutViewModel
import org.mozilla.xiu.browser.databinding.FragmentBookmarkBinding
import org.mozilla.xiu.browser.session.createSession
import org.mozilla.xiu.browser.utils.GroupUtils

/**
 * A simple [Fragment] subclass.
 * Use the [BookmarkFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BookmarkFragment : Fragment() {
    lateinit var binding: FragmentBookmarkBinding
    private lateinit var bookmarkViewModel: BookmarkViewModel
    private lateinit var shortcutViewModel: ShortcutViewModel
    private var bookmarks: List<Bookmark?>? = null
    var i: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarkViewModel =
            ViewModelProvider(requireActivity()).get<BookmarkViewModel>(BookmarkViewModel::class.java)
        shortcutViewModel = ViewModelProvider(this)[ShortcutViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBookmarkBinding.inflate(LayoutInflater.from(requireContext()))

        var bookmarkAdapter = BookmarkAdapter()
        binding.bookmarkRecyclerview.adapter = bookmarkAdapter
        binding.bookmarkRecyclerview.layoutManager = LinearLayoutManager(context)

        binding.BookmarkFragmentEdittext?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //var s1=s.toString().trim()
                bookmarkViewModel.findBookmarksWithPattern(s.toString())
                    ?.observe(viewLifecycleOwner) {
                        bookmarkAdapter.submitList(it)
                    }


            }

            override fun afterTextChanged(s: Editable?) {

            }
        })


        bookmarkViewModel.allBookmarksLive?.observe(viewLifecycleOwner) {
            if (i == 0) {
                val groupUtils = it?.let { it1 -> GroupUtils(it1) }
                if (groupUtils != null) {
                    bookmarks = groupUtils.groupBookmark()
                    bookmarkAdapter.submitList(groupUtils.groupBookmark())
                }
                i = 1
            }

        }

        bookmarkAdapter.select = object : BookmarkAdapter.Select {
            override fun onSelect(url: String) {
                createSession(url, requireActivity())
            }

        }
        bookmarkAdapter.popupSelect = object : BookmarkAdapter.PopupSelect {
            override fun onPopupSelect(bean: Bookmark, item: Int) {
                when (item) {
                    HistoryAdapter.DELETE -> {
                        bookmarkViewModel.deleteBookmarks(bean)
                        bookmarks = bookmarks?.toMutableList()?.apply { remove(bean) }
                        bookmarkAdapter.submitList(bookmarks)

                    }

                    HistoryAdapter.ADD_TO_HOMEPAGE -> {
                        shortcutViewModel.insertShortcuts(
                            Shortcut(
                                bean.url,
                                bean.title,
                                System.currentTimeMillis().toInt()
                            )
                        )
                    }
                }
            }
        }
        return binding.root
    }
}