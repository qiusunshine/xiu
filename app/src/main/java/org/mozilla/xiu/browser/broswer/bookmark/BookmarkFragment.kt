package org.mozilla.xiu.browser.broswer.bookmark

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.broswer.bookmark.data.ChromeParser
import org.mozilla.xiu.browser.broswer.history.HistoryAdapter
import org.mozilla.xiu.browser.componets.BookmarkDialog
import org.mozilla.xiu.browser.componets.BookmarkDirDialog
import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.database.bookmark.BookmarkViewModel
import org.mozilla.xiu.browser.database.shortcut.Shortcut
import org.mozilla.xiu.browser.database.shortcut.ShortcutViewModel
import org.mozilla.xiu.browser.databinding.FragmentBookmarkBinding
import org.mozilla.xiu.browser.session.createSession
import org.mozilla.xiu.browser.utils.CollectionUtil
import org.mozilla.xiu.browser.utils.HeavyTaskUtil
import org.mozilla.xiu.browser.utils.ShareUtil
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.UriUtilsPro
import org.mozilla.xiu.browser.utils.addByList
import org.mozilla.xiu.browser.utils.copyToDownloadDir
import org.mozilla.xiu.browser.utils.deleteByGroupNode
import org.mozilla.xiu.browser.utils.filterList
import org.mozilla.xiu.browser.utils.getGroupPath
import org.mozilla.xiu.browser.utils.getNewFilePath
import org.mozilla.xiu.browser.utils.initBookmarkParent
import java.io.File
import java.io.IOException

/**
 * A simple [Fragment] subclass.
 * Use the [BookmarkFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BookmarkFragment(
    val activity: MainActivity
) : Fragment() {
    lateinit var binding: FragmentBookmarkBinding
    private lateinit var bookmarkViewModel: BookmarkViewModel
    private lateinit var shortcutViewModel: ShortcutViewModel
    private var bookmarks: MutableList<Bookmark?> = ArrayList()
    private lateinit var bookmarkAdapter: BookmarkAdapter
    private var groupSelected: String = ""
    private var searchKey: String = ""

    fun onBackPressed(): Boolean {
        if (StringUtil.isNotEmpty(groupSelected)) {
            val groups = groupSelected.split("/").dropLastWhile { it.isEmpty() }
                .toTypedArray()
            groupSelected = if (groups.size > 1) {
                StringUtil.arrayToString(groups, 0, groups.size - 1, "/")
            } else {
                ""
            }
            val list = filterList(bookmarks, groupSelected, searchKey)
            bookmarkAdapter.submitList(list)
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarkViewModel =
            ViewModelProvider(requireActivity()).get(BookmarkViewModel::class.java)
        shortcutViewModel = ViewModelProvider(this)[ShortcutViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBookmarkBinding.inflate(LayoutInflater.from(requireContext()))
        bookmarkAdapter = BookmarkAdapter()
        binding.bookmarkRecyclerview.adapter = bookmarkAdapter
        binding.bookmarkRecyclerview.layoutManager = LinearLayoutManager(context)

        binding.BookmarkFragmentEdittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //var s1=s.toString().trim()
                searchKey = s?.toString() ?: ""
                val list = filterList(bookmarks, groupSelected, searchKey)
                bookmarkAdapter.submitList(list)
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        bookmarkViewModel.allBookmarksLive?.observe(viewLifecycleOwner) {
            bookmarks = ArrayList(it ?: arrayListOf())
            initBookmarkParent(bookmarks)
            val list = filterList(bookmarks, groupSelected, searchKey)
            bookmarkAdapter.submitList(list)
        }

        bookmarkAdapter.select = object : BookmarkAdapter.Select {
            override fun onSelect(bean: Bookmark) {
                if (!bean.isDir()) {
                    createSession(bean.url, requireActivity())
                } else {
                    groupSelected = getGroupPath(bean)
                    val list = filterList(bookmarks, groupSelected, searchKey)
                    bookmarkAdapter.submitList(list)
                }
            }
        }
        bookmarkAdapter.popupSelect = object : BookmarkAdapter.PopupSelect {
            override fun onPopupSelect(bean: Bookmark, item: Int) {
                when (item) {
                    HistoryAdapter.UPDATE -> {
                        if (bean.isDir()) {
                            BookmarkDirDialog(
                                requireActivity(),
                                bookmarks,
                                groupSelected = groupSelected,
                                oldBookmark = bean
                            ).show()
                        } else {
                            BookmarkDialog(requireActivity(), bean.title, bean.url, bean).open()
                        }
                    }

                    HistoryAdapter.DELETE -> {
                        if (bean.isDir()) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(requireContext().getString(R.string.notify))
                                .setMessage(
                                    requireContext().getString(R.string.delete_bookmark_dir_message)
                                )
                                .setPositiveButton(requireContext().getString(R.string.confirm)) { d, _ ->
                                    d.dismiss()
                                    val groupNode: ChromeParser.BookmarkGroupNode =
                                        ChromeParser.toGroupNode(bookmarks, bean)
                                    deleteByGroupNode(bookmarkViewModel, groupNode)
                                }.setNegativeButton(requireContext().getString(R.string.cancel)) { d, _ ->
                                    d.dismiss()
                                }.show()
                        } else {
                            bookmarkViewModel.deleteBookmarks(bean)
                            bookmarks = bookmarks.toMutableList().apply { remove(bean) }
                            bookmarkAdapter.submitList(bookmarks)
                        }
                    }

                    HistoryAdapter.ADD_TO_HOMEPAGE -> {
                        if (bean.isDir()) {
                            ToastMgr.shortBottomCenter(context, "开发中")
                        } else {
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
        }
        binding.materialButton19.setOnClickListener {
            showMenu(it, bookmarks, groupSelected, activity)
        }
        return binding.root
    }

    private val launcher: ActivityResultLauncher<String> =
        registerForActivityResult(
            object :
                ActivityResultContract<String, Intent?>() {
                override fun createIntent(context: Context, input: String): Intent {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = input
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    return intent
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
                    return intent
                }
            },
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
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val bookmarks = ChromeParser.parse(s)
                                    File(s).delete()
                                    if (CollectionUtil.isEmpty(bookmarks)) {
                                        ToastMgr.shortBottomCenter(
                                            context,
                                            requireContext().getString(R.string.import_error)
                                        )
                                    } else {
                                        val count: Int = addByList(
                                            requireContext(),
                                            bookmarkViewModel,
                                            bookmarks
                                        )
                                        ToastMgr.shortBottomCenter(
                                            context,
                                            requireContext().getString(
                                                R.string.import_success,
                                                count.toString()
                                            )
                                        )
                                    }
                                }
                            }

                            override fun failed(msg: String) {
                                ToastMgr.shortBottomCenter(
                                    context,
                                    "出错：$msg"
                                )
                            }
                        })
                }
            })

    private fun showMenu(
        v: View,
        bookmarks: MutableList<Bookmark?>,
        groupSelected: String,
        context: Activity
    ) {
        val popup = PopupMenu(context, v)
        popup.menuInflater.inflate(R.menu.bookmark_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.menu_bookmark_create_dir -> {
                    BookmarkDirDialog(context, bookmarks, groupSelected = groupSelected).show()
                }

                R.id.menu_bookmark_import -> {
                    launcher.launch("text/html")
                }

                R.id.menu_bookmark_export -> {
                    exportRuleToHtml()
                }

                R.id.menu_bookmark_clear -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(requireContext().getString(R.string.notify))
                        .setMessage(
                            requireContext().getString(R.string.clear_bookmark_message)
                        )
                        .setPositiveButton(requireContext().getString(R.string.confirm)) { d, _ ->
                            d.dismiss()
                            bookmarkViewModel.deleteAllBookmarks()
                        }.setNegativeButton(requireContext().getString(R.string.cancel)) { d, _ ->
                            d.dismiss()
                        }.show()
                }
            }
            true
        }
        popup.show()
    }

    private fun exportRuleToHtml() {
        val list = bookmarks
        if (CollectionUtil.isEmpty(list)) {
            return
        }
        val path: String = getShareFilePath("share-bookmarks.html") ?: return
        if (StringUtil.isNotEmpty(path)) {
            HeavyTaskUtil.executeNewTask {
                try {
                    ChromeParser.exportToFile(list, path)
                    lifecycleScope.launch(Dispatchers.Main) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(requireContext().getString(R.string.notify))
                            .setMessage(
                                requireContext().getString(R.string.export_success)
                            )
                            .setPositiveButton(requireContext().getString(R.string.share)) { d, _ ->
                                d.dismiss()
                                ShareUtil.findChooserToSend(
                                    context,
                                    "file://$path"
                                )
                            }.setNegativeButton(requireContext().getString(R.string.move)) { d, _ ->
                                d.dismiss()
                                copyToDownloadDir(requireContext(), path)
                                val np: String? = getNewFilePath(requireContext(), path, null)
                                ToastMgr.shortBottomCenter(context, "已转存到$np")
                            }.show()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    ToastMgr.shortBottomCenter(context, "出错：" + e.message)
                }
            }
        }
    }

    private fun getShareFilePath(fileName: String): String? {
        val fileDirPath: String = UriUtilsPro.getRootDir(context) + File.separator + "share"
        val dir = File(fileDirPath)
        if (!dir.exists()) {
            val ok = dir.mkdir()
            if (!ok) {
                ToastMgr.shortBottomCenter(
                    context,
                    requireContext().getString(R.string.create_dir_failed)
                )
                return null
            }
        }
        val ruleFile = File(fileDirPath + File.separator + fileName)
        if (ruleFile.exists()) {
            val ok = ruleFile.delete()
            if (!ok) {
                ToastMgr.shortBottomCenter(
                    context,
                    requireContext().getString(R.string.delete_file_failed)
                )
                return null
            }
        }
        return ruleFile.absolutePath
    }
}