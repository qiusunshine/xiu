package org.mozilla.xiu.browser.componets.popup

import android.content.Context
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.base.addOnBackPressed
import org.mozilla.xiu.browser.databinding.PopupNetworkBinding
import org.mozilla.xiu.browser.session.SessionDelegate
import org.mozilla.xiu.browser.webextension.TabRequest

class NetworkPopup {
    val context: Context
    private var bottomSheetDialog: BottomSheetDialog
    private var binding: PopupNetworkBinding
    private var adapter: NetworkAdapter

    constructor(
        context: Context,
        sessionDelegate: SessionDelegate
    ) {
        this.context = context
        bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
        val onBackPressedCallback = bottomSheetDialog.addOnBackPressed {
            false
        }
        bottomSheetDialog.setOnDismissListener {
            onBackPressedCallback.remove()
        }
        binding = PopupNetworkBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        adapter = NetworkAdapter()
        adapter.select = object : NetworkAdapter.Select {
            override fun onSelect(tabRequest: TabRequest) {
                bottomSheetDialog.dismiss()
            }
        }
        binding.historyRecyclerview.adapter = adapter
        binding.historyRecyclerview.layoutManager = LinearLayoutManager(context)
        updateData(sessionDelegate.requests, TabRequest.ALL)
        binding.typeAll.setOnClickListener {
            updateType(TabRequest.ALL)
            updateData(sessionDelegate.requests, TabRequest.ALL)
        }
        binding.typeVideo.setOnClickListener {
            updateType(TabRequest.VIDEO)
            updateData(sessionDelegate.requests, TabRequest.VIDEO)
        }
        binding.typeAudio.setOnClickListener {
            updateType(TabRequest.AUDIO)
            updateData(sessionDelegate.requests, TabRequest.AUDIO)
        }
        binding.typeImage.setOnClickListener {
            updateType(TabRequest.IMAGE)
            updateData(sessionDelegate.requests, TabRequest.IMAGE)
        }
        binding.typeOther.setOnClickListener {
            updateType(TabRequest.OTHER)
            updateData(sessionDelegate.requests, TabRequest.OTHER)
        }
    }

    private fun updateType(type: String) {
        val ok = ContextCompat.getDrawable(context, R.drawable.icon_ok)
        binding.typeAll.icon = if (type == TabRequest.ALL) ok else null
        binding.typeVideo.icon = if (type == TabRequest.VIDEO) ok else null
        binding.typeAudio.icon = if (type == TabRequest.AUDIO) ok else null
        binding.typeImage.icon = if (type == TabRequest.IMAGE) ok else null
        binding.typeOther.icon = if (type == TabRequest.OTHER) ok else null
    }

    private fun updateData(requests: List<TabRequest>, type: String) {
        if (type == TabRequest.ALL) {
            adapter.submitList(ArrayList(requests))
        } else {
            adapter.submitList(requests.filter { it.type == type })
        }
    }

    fun show() {
        bottomSheetDialog.show()
    }
}