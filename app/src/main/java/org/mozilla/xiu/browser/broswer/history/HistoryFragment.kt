package org.mozilla.xiu.browser.broswer.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.database.history.History
import org.mozilla.xiu.browser.database.history.HistoryViewModel
import org.mozilla.xiu.browser.database.shortcut.Shortcut
import org.mozilla.xiu.browser.database.shortcut.ShortcutViewModel
import org.mozilla.xiu.browser.databinding.FragmentHistoryBinding
import org.mozilla.xiu.browser.session.createSession

class HistoryFragment : Fragment() {
    lateinit var binding:FragmentHistoryBinding
    lateinit var historyViewModel: HistoryViewModel
    lateinit var shortcutViewModel: ShortcutViewModel
    private var histories: List<History?>? = null
    lateinit var historyAdapter: HistoryAdapter
    var i:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        shortcutViewModel = ViewModelProvider(this)[ShortcutViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=FragmentHistoryBinding.inflate(LayoutInflater.from(requireContext()))
        historyAdapter= HistoryAdapter()
        binding.historyRecyclerview.adapter=historyAdapter
        binding.historyRecyclerview.layoutManager = LinearLayoutManager(context)
        historyViewModel.allHistoriesLive?.observe(viewLifecycleOwner){
            if (i == 0) {
                histories = it
                historyAdapter.submitList(histories)
                i=1
            }


        }



        binding.HistoryFragmentSearching?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                var s1=s.toString().trim()
                historyViewModel.findHistoriesWithMix(s1)?.observe(viewLifecycleOwner){
                        historyAdapter.submitList(it)
                }



            }

            override fun afterTextChanged(s: Editable?) {

            }
        })


        binding.materialButton20.setOnClickListener { showMenu(it) }



        historyAdapter.select= object : HistoryAdapter.Select {
            override fun onSelect(url: String) {
                createSession(url,requireActivity())
            }

        }
        historyAdapter.popupSelect = object : HistoryAdapter.PopupSelect {
            override fun onPopupSelect(bean: History, item: Int) {
                when(item){
                    HistoryAdapter.DELETE ->{
                        historyViewModel.deleteHistories(bean)
                        histories = histories?.toMutableList()?.apply { remove(bean) }
                        historyAdapter.submitList(histories)

                    }
                    HistoryAdapter.ADD_TO_HOMEPAGE ->{
                        shortcutViewModel.insertShortcuts(Shortcut(bean.url,bean.title,System.currentTimeMillis().toInt()))
                    }
                }
            }

        }

        return binding.root
    }

    private fun showMenu(v: View) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(R.menu.history_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when(menuItem.itemId){
                R.id.history_menu_all_delete ->{
                    historyViewModel.deleteAllHistories()
                    historyAdapter.submitList(null)
                }
            }
            false
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }
}