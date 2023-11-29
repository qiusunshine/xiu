package org.mozilla.xiu.browser.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.mozilla.xiu.browser.databinding.FragmentPrivacyAndServiceBinding

class UpdateRecordsFragment : Fragment() {
    lateinit var fragmentPrivacyAndServiceBinding: FragmentPrivacyAndServiceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentPrivacyAndServiceBinding = FragmentPrivacyAndServiceBinding.inflate(LayoutInflater.from(context))
        fragmentPrivacyAndServiceBinding.webView2.loadUrl("file:///android_asset/update_records.txt")
        // Inflate the layout for this fragment
        return fragmentPrivacyAndServiceBinding.root
    }
}