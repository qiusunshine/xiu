package org.mozilla.xiu.browser.componets.popup

import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.base.addOnBackPressed
import org.mozilla.xiu.browser.broswer.bookmark.BookmarkFragment
import org.mozilla.xiu.browser.broswer.history.HistoryFragment
import org.mozilla.xiu.browser.componets.CollectionAdapter
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.databinding.PopupHistoryBinding

class HistoryPopup {
    val context: MainActivity
    lateinit var bottomSheetDialog: BottomSheetDialog
    lateinit var binding: PopupHistoryBinding
    private var observer: Observer<Boolean>

    private var fragments: List<Fragment>

    constructor(
        context: MainActivity,
    ) {
        this.context = context
        val bookmarkFragment = BookmarkFragment(context)
        val historyFragment = HistoryFragment()
        fragments = listOf(bookmarkFragment, historyFragment)
        bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
        val onBackPressedCallback = bottomSheetDialog.addOnBackPressed {
            binding.historyViewPager.currentItem == 0 && bookmarkFragment.onBackPressed()
        }
        binding = PopupHistoryBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        binding.historyViewPager.adapter = CollectionAdapter(context, fragments)
        TabLayoutMediator(binding.historytabLayout, binding.historyViewPager) { tab, position ->
            when (position) {
                0 -> tab.setIcon(R.drawable.bookmarks)
                1 -> tab.setIcon(R.drawable.hourglass_split)
            }
        }.attach()
        observer = Observer {
            if (!it) bottomSheetDialog.dismiss()
        }
        HomeLivedata.getInstance().observe(context, observer)
        bottomSheetDialog.setOnDismissListener {
            onBackPressedCallback.remove()
            HomeLivedata.getInstance().removeObserver(observer)
        }
        binding.historyViewPager.setCurrentItem(1, false)
    }

    fun show() {
        bottomSheetDialog.show()
    }
}