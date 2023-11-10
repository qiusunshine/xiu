package org.mozilla.xiu.browser.componets.popup

import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.componets.CollectionAdapter
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.broswer.history.HistoryFragment
import org.mozilla.xiu.browser.databinding.PopupHistoryBinding

class HistoryPopup {
    val context: MainActivity
    lateinit var bottomSheetDialog: BottomSheetDialog
    lateinit var binding: PopupHistoryBinding

    private val fragments=listOf<Fragment>(HistoryFragment())
    constructor(
        context: MainActivity,
    ) {
        this.context = context
        bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog )
        binding = PopupHistoryBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        binding.historyViewPager.adapter= CollectionAdapter(context,fragments)
        TabLayoutMediator(binding.historytabLayout,binding.historyViewPager){ tab,position->
            when (position) {
                0 -> tab.setIcon(R.drawable.hourglass_split)
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