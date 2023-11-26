package org.mozilla.xiu.browser.componets.popup

import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.broswer.bookmark.BookmarkFragment
import org.mozilla.xiu.browser.broswer.bookmark.sync.SyncBookmarkFolderFragment
import org.mozilla.xiu.browser.componets.CollectionAdapter
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.databinding.PopupBookmarkBinding

class BookmarkPopup {
    val context: MainActivity
    lateinit var bottomSheetDialog: BottomSheetDialog
    lateinit var binding: PopupBookmarkBinding
    private var observer: Observer<Boolean>

    private var fragments: List<Fragment>

    constructor(
        context: MainActivity
    ) {
        this.context = context
        val bookmarkFragment = BookmarkFragment(context)
        fragments = listOf(bookmarkFragment, SyncBookmarkFolderFragment())
        bottomSheetDialog = object : BottomSheetDialog(context, R.style.BottomSheetDialog) {
            override fun onBackPressed() {
                if(bookmarkFragment.onBackPressed()) {
                    return
                }
                super.onBackPressed()
            }
        }
        binding = PopupBookmarkBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        binding.bookmarkViewPager.adapter = CollectionAdapter(context, fragments)
        TabLayoutMediator(binding.tabLayout, binding.bookmarkViewPager) { tab, position ->
            when (position) {
                0 -> tab.setIcon(R.drawable.bookmarks)
                1 -> tab.setIcon(R.drawable.sync_circle)
            }
        }.attach()
        observer = Observer {
            if (!it) bottomSheetDialog.dismiss()
        }
        HomeLivedata.getInstance().observe(context, observer)
        bottomSheetDialog.setOnDismissListener {
            HomeLivedata.getInstance().removeObserver(observer)
        }
    }

    fun show() {
        bottomSheetDialog.show()
    }
}