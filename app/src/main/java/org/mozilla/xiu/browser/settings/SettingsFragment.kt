package org.mozilla.xiu.browser.settings

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.session.createSession

/**
 * 2023.2.11 19:10
 * 正月廿一
 * thallo
 **/
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<Preference>("settingAbout")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_aboutFragment)
            false
        }
        findPreference<Preference>("searching")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_settingsSearching2)
            false
        }
        findPreference<Preference>("addons")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_addonsManagerFragment)
            false
        }
        findPreference<Preference>("privacyAndService")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_privacyAndServiceFragment)
            false
        }
        preferenceScreen.onPreferenceClickListener = Preference.OnPreferenceClickListener { true }
    }
}