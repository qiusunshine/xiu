package org.mozilla.xiu.browser.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.mozilla.xiu.browser.R

class SettingsSearching : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.searching_preferences, rootKey)
    }
}