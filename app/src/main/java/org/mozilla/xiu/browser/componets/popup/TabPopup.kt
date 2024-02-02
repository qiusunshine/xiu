package org.mozilla.xiu.browser.componets.popup

import android.view.LayoutInflater
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yanzhenjie.recyclerview.touch.OnItemMoveListener
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.base.addOnBackPressed
import org.mozilla.xiu.browser.componets.HomeLivedata
import org.mozilla.xiu.browser.databinding.PopupTabBinding
import org.mozilla.xiu.browser.session.GeckoViewModel
import org.mozilla.xiu.browser.session.SeRuSettings
import org.mozilla.xiu.browser.session.SessionDelegate
import org.mozilla.xiu.browser.tab.DelegateListLiveData
import org.mozilla.xiu.browser.tab.RemoveTabLiveData
import org.mozilla.xiu.browser.tab.TabListAdapter

class TabPopup {
    val context: MainActivity
    private val bottomSheetDialog: BottomSheetDialog
    private val binding: PopupTabBinding
    val geckoViewModel: GeckoViewModel
    private var observer: Observer<ArrayList<SessionDelegate>>? = null

    constructor(context: MainActivity) {
        this.context = context
        bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
        binding = PopupTabBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        geckoViewModel = ViewModelProvider(context).get(GeckoViewModel::class.java)
    }

    fun show() {
        val onBackPressedCallback = bottomSheetDialog.addOnBackPressed {
            false
        }
        val adapter = TabListAdapter()
        binding.recyclerView.adapter = adapter
        adapter.select = object : TabListAdapter.Select {
            override fun onSelect() {
                bottomSheetDialog.dismiss()
            }
        }
        binding.recyclerView.layoutManager = GridLayoutManager(context, 2)
        observer = Observer {
            adapter.submitList(it.toList())
        }
        DelegateListLiveData.getInstance().observe(context, observer!!)
        bottomSheetDialog.setOnDismissListener {
            onBackPressedCallback.remove()
            DelegateListLiveData.getInstance().removeObserver(observer!!)
        }
        binding.popAddButton.setOnClickListener {
            HomeLivedata.getInstance().Value(true)
            bottomSheetDialog.dismiss()
        }
        binding.recyclerView.isItemViewSwipeEnabled = true
        binding.recyclerView.setOnItemMoveListener(object : OnItemMoveListener {
            override fun onItemMove(
                srcHolder: RecyclerView.ViewHolder?,
                targetHolder: RecyclerView.ViewHolder?
            ): Boolean {
                return false
            }

            override fun onItemDismiss(srcHolder: RecyclerView.ViewHolder) {
                try {
                    RemoveTabLiveData.getInstance().Value(adapter.currentList[srcHolder.bindingAdapterPosition])
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        bottomSheetDialog.show()
    }

    fun createSession(uri: String) {
        val session = GeckoSession()
        val sessionSettings = session.settings
        SeRuSettings(sessionSettings, context)
        session.open(GeckoRuntime.getDefault(context))
        session.loadUri(uri)
        geckoViewModel.changeSearch(session)
        if(HomeLivedata.getInstance().value == true) {
            HomeLivedata.getInstance().Value(false)
        }
    }
}