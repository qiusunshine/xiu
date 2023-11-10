package org.mozilla.xiu.browser.componets.popup

import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import org.mozilla.xiu.browser.*
import org.mozilla.xiu.browser.componets.CollectionAdapter
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.broswer.bookmark.BookmarkFragment
import org.mozilla.xiu.browser.broswer.bookmark.sync.SyncBookmarkFolderFragment
import org.mozilla.xiu.browser.databinding.PopupBookmarkBinding

class BookmarkPopup {
    val context: MainActivity
    lateinit var bottomSheetDialog: BottomSheetDialog
    lateinit var binding: PopupBookmarkBinding

    private val fragments=listOf<Fragment>(BookmarkFragment(), SyncBookmarkFolderFragment())
    constructor(
        context: MainActivity
    ) {
        this.context = context
        bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog )
        binding = PopupBookmarkBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        binding.bookmarkViewPager.adapter= CollectionAdapter(context,fragments)
        TabLayoutMediator(binding.tabLayout,binding.bookmarkViewPager){ tab,position->
            when (position) {
                0 -> tab.setIcon(R.drawable.bookmarks)
                1 -> tab.setIcon(R.drawable.sync_circle)
            }
        }.attach()
        HomeLivedata.getInstance().observe(context){
            if (!it) bottomSheetDialog.dismiss()
        }

    }
    fun show(){
        bottomSheetDialog.show()
    }
}