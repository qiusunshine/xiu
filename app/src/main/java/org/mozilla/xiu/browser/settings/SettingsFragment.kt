package org.mozilla.xiu.browser.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.utils.PreferenceMgr
import org.mozilla.xiu.browser.video.event.FloatVideoSwitchEvent

/**
 * 2023.2.11 19:10
 * 正月廿一
 * thallo
 **/
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<Preference>("settingNewVersion")?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.data = Uri.parse(getString(R.string.new_version_url))
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            true
        }
        findPreference<Preference>("settingAbout")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_aboutFragment)
            false
        }
        findPreference<Preference>("searching")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_settingsSearching2)
            false
        }
        findPreference<Preference>("float_video")?.setOnPreferenceClickListener {
            val now = PreferenceMgr.getBoolean(getContext(), "float_xiutan", false)
            val m = if(now) {
                ContextCompat.getString(requireContext(), R.string.float_video_opened)
            } else {
                ContextCompat.getString(requireContext(), R.string.float_video_closed)
            }
            val c = if(now) {
                ContextCompat.getString(requireContext(), R.string.close)
            } else {
                ContextCompat.getString(requireContext(), R.string.open)
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(ContextCompat.getString(requireContext(), R.string.float_video))
                .setMessage(getString(R.string.float_video_message, m))
                .setPositiveButton(c) { d, _ ->
                    EventBus.getDefault().post(FloatVideoSwitchEvent())
                    d.dismiss()
                }.setNegativeButton(getString(R.string.cancel)) { d, _ ->
                    d.dismiss()
                }.show()
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