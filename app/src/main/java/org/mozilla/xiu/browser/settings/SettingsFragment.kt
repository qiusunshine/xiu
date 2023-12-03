package org.mozilla.xiu.browser.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.download.DownloadChooser
import org.mozilla.xiu.browser.download.DownloadTask
import org.mozilla.xiu.browser.download.DownloadTaskLiveData
import org.mozilla.xiu.browser.utils.PreferenceMgr
import org.mozilla.xiu.browser.video.event.FloatVideoSwitchEvent
import org.mozilla.xiu.browser.webextension.NewTabUrlChangeEvent

/**
 * 2023.2.11 19:10
 * 正月廿一
 * thallo
 **/
class SettingsFragment : PreferenceFragmentCompat() {
    private var listener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "home_page_url" -> {
                    EventBus.getDefault().post(NewTabUrlChangeEvent())
                }

                "customDownloader" -> {
                    val url: String? = when (sharedPreferences.getString(key, getString(R.string.downloader_default))) {
                        getString(R.string.downloader_idm) -> DownloadChooser.checkAndGetIDMUrl(
                            requireContext()
                        )

                        getString(R.string.downloader_adm) -> DownloadChooser.checkAndGetADMUrl(
                            requireContext()
                        )

                        getString(R.string.downloader_youtoo) -> DownloadChooser.checkAndGetYoutooUrl(
                            requireContext()
                        )

                        getString(R.string.downloader_hiker) -> DownloadChooser.checkAndGetHikerUrl(
                            requireContext()
                        )

                        else -> null
                    }
                    if (url != null) {
                        val c =
                            if (url.isEmpty()) getString(R.string.confirm) else getString(R.string.download)
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(ContextCompat.getString(requireContext(), R.string.notify))
                            .setMessage(getString(R.string.downloader_msg))
                            .setPositiveButton(c) { d, _ ->
                                if (url.isNotEmpty()) {
                                    val intent = Intent(requireContext(), MainActivity::class.java)
                                    intent.data = Uri.parse(url)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                }
                                d.dismiss()
                            }.setNegativeButton(getString(R.string.cancel)) { d, _ ->
                                d.dismiss()
                            }.show()
                    }
                }
            }
        }

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
            val m = if (now) {
                ContextCompat.getString(requireContext(), R.string.float_video_opened)
            } else {
                ContextCompat.getString(requireContext(), R.string.float_video_closed)
            }
            val c = if (now) {
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
        findPreference<Preference>("home_page_url")?.summaryProvider =
            SummaryProvider<EditTextPreference> { preference ->
                if (TextUtils.isEmpty(preference.text)) {
                    null
                } else {
                    preference.text
                }
            }
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(listener)
        preferenceScreen.onPreferenceClickListener = Preference.OnPreferenceClickListener { true }
    }

    override fun onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(listener)
        super.onDestroy()
    }
}