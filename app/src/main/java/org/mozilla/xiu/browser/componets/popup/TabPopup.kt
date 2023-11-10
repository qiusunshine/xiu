package org.mozilla.xiu.browser.componets.popup

import android.view.LayoutInflater
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.databinding.PopupTabBinding
import org.mozilla.xiu.browser.session.GeckoViewModel
import org.mozilla.xiu.browser.session.SeRuSettings
import org.mozilla.xiu.browser.tab.DelegateListLiveData
import org.mozilla.xiu.browser.tab.TabListAdapter
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

class TabPopup {
    val context:MainActivity
    private val bottomSheetDialog: BottomSheetDialog
    private val binding:PopupTabBinding
    val  geckoViewModel: GeckoViewModel
    constructor(context: MainActivity) {
        this.context = context
        bottomSheetDialog = BottomSheetDialog(context,R.style.BottomSheetDialog )
        binding = PopupTabBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        geckoViewModel= ViewModelProvider(context).get(GeckoViewModel::class.java)

    }
    fun show(){
        val adapter=TabListAdapter()
        binding.recyclerView.adapter=adapter
        adapter.select=object :TabListAdapter.Select{
            override fun onSelect() {
                bottomSheetDialog.dismiss()
            }

        }

        binding.recyclerView.layoutManager = GridLayoutManager(context, 2);
        DelegateListLiveData.getInstance().observe(context){
            adapter.submitList(it.toList())
        }
        binding.popAddButton.setOnClickListener {
            HomeLivedata.getInstance().Value(true)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }
    fun createSession(uri: String) {
        val session = GeckoSession()
        val sessionSettings = session.settings
        SeRuSettings(sessionSettings, context)

        session.open(GeckoRuntime.getDefault(context) )
        session.loadUri(uri)
        geckoViewModel.changeSearch(session)

    }
}