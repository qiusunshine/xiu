package org.mozilla.xiu.browser

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import org.mozilla.xiu.browser.broswer.bookmark.shortcut.ShortcutAdapter
import org.mozilla.xiu.browser.componets.StageFxAEntryPoint
import org.mozilla.xiu.browser.componets.TabBottomSheetDialog.Companion.TAG
import org.mozilla.xiu.browser.componets.popup.AccountPopup
import org.mozilla.xiu.browser.database.shortcut.Shortcut
import org.mozilla.xiu.browser.database.shortcut.ShortcutViewModel
import org.mozilla.xiu.browser.databinding.FragmentFirstBinding
import org.mozilla.xiu.browser.fxa.AccountManagerCollection
import org.mozilla.xiu.browser.fxa.Fxa
import org.mozilla.xiu.browser.fxa.AccountProfileViewModel
import org.mozilla.xiu.browser.session.*
import kotlinx.coroutines.launch
import mozilla.components.service.fxa.FxaAuthData
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.toAuthType
import mozilla.components.support.rustlog.RustLog
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    lateinit var geckoViewModel: GeckoViewModel
    private lateinit var fxaViewModel :AccountProfileViewModel
    private lateinit var accountManagerCollection :AccountManagerCollection
    private var fxaAccountManager: FxaAccountManager? = null
    private  lateinit var fxa: Fxa
    lateinit var shortcutViewModel: ShortcutViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        geckoViewModel = ViewModelProvider(requireActivity())[GeckoViewModel::class.java]
        shortcutViewModel=ViewModelProvider(requireActivity())[ShortcutViewModel::class.java]
        fxaViewModel = ViewModelProvider(requireActivity())[AccountProfileViewModel::class.java]
        accountManagerCollection = ViewModelProvider(requireActivity())[AccountManagerCollection::class.java]
        try {
            fxa = Fxa()
            try {
                RustLog.disable()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            fxaAccountManager = fxa.init(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }


        lifecycleScope.launch {

            fxaViewModel.data.collect(){
                binding.signinButton?.let { it1 ->
                    Glide.with(requireContext()).load(it.avatar).circleCrop().into(
                        it1
                    )
                }
            }
        }

        lifecycleScope.launch {
            accountManagerCollection.data.collect(){
                fxaAccountManager = it
                Log.d("fxaAccountManager",""+it)

            }
        }







        binding.signinButton?.setOnClickListener {
            lifecycleScope.launch {
                if (!fxa.isLogin){
                    fxaAccountManager?.beginAuthentication(entrypoint =StageFxAEntryPoint.DeepLink)?.let {
                    createSession(it,requireActivity())
                    }
                }
                else{
                    //fxaAccountManager.syncNow(SyncReason.User)
                    //fxaAccountManager.authenticatedAccount()?.deviceConstellation()?.pollForCommands()
                    AccountPopup().show(parentFragmentManager,TAG)

                }
            }
        }
        binding.qrButton?.setOnClickListener {
            if (requireActivity().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                var intent= Intent(requireContext(), HolderActivity::class.java)
                intent.putExtra("Page","QRSCANNING")
                requireContext().startActivity(intent)
            }
            else requireActivity().requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)

        }


        binding.HomeSearchText?.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if (KeyEvent.KEYCODE_ENTER == i && keyEvent.action == KeyEvent.ACTION_DOWN) {
                var value= binding.HomeSearchText!!.text.toString()
                if (Patterns.WEB_URL.matcher(value).matches() || URLUtil.isValidUrl(value)) {
                        createSession(value, requireActivity())

                } else {
                    createSession("https://cn.bing.com/search?q=$value",requireActivity())
                }

            }
            false
        })

        DelegateLivedata.getInstance().observe(viewLifecycleOwner){
            it.login=object : SessionDelegate.Login{
                override fun onLogin(code: String, state: String, action: String) {
                    lifecycleScope.launch {
                        fxaAccountManager?.finishAuthentication(
                            FxaAuthData(action.toAuthType(), code = code, state = state),
                        )

                    }

                }
            }
        }

        val calendar = Calendar.getInstance()

        if (calendar[Calendar.HOUR_OF_DAY] in 6..11) {
            binding.tips?.text = "Good\nMorning"
        }
        if (calendar[Calendar.HOUR_OF_DAY] in 12..13) {
            binding.tips?.text = "Good\n" + "Noon"
        }
        if (calendar[Calendar.HOUR_OF_DAY] in 14..19) {
            binding.tips?.text = "Good\n" + "Afternoon"
        }
        if (calendar[Calendar.HOUR_OF_DAY] in 20..22) {
            binding.tips?.text = "Good\n" + "Night"
        }
        if (22 < calendar[Calendar.HOUR_OF_DAY]) {
            binding.tips?.text = "Good\nDream"
        }
        if (calendar[Calendar.HOUR_OF_DAY] in 0..4) {
            binding.tips?.text = "Good\nDream"
        }


        var shortcutAdapter= ShortcutAdapter()
        binding.shortcutsRecyclerView?.adapter =shortcutAdapter
        binding.shortcutsRecyclerView?.layoutManager = GridLayoutManager(context, 4)
        shortcutViewModel.allShortcutsLive?.observe(requireActivity()){
            shortcutAdapter.submitList(it)
        }

        shortcutAdapter.select= object : ShortcutAdapter.Select {
            override fun onSelect(url: String) {
                createSession(url,requireActivity())
            }

        }
        shortcutAdapter.longClick= object : ShortcutAdapter.LongClick {
            override fun onLongClick(bean: Shortcut) {
                shortcutViewModel.deleteShortcuts(bean)
            }


        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 1
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            fxaAccountManager?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun showDialog() {
        val fragmentManager = parentFragmentManager
        val newFragment = AccountPopup()
        if (false) {
            // The device is using a large layout, so show the fragment as a dialog
            newFragment.show(fragmentManager, "dialog")
        } else {
            // The device is smaller, so show the fragment fullscreen
            val transaction = fragmentManager.beginTransaction()
            // For a little polish, specify a transition animation
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            // To make it fullscreen, use the 'content' root view as the container
            // for the fragment, which is always the root view for the activity
            transaction
                .add(android.R.id.content, newFragment)
                .addToBackStack(null)
                .commit()
        }
    }

}