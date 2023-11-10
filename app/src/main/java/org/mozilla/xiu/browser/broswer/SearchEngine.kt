package org.mozilla.xiu.browser.broswer

import android.app.Activity
import androidx.preference.PreferenceManager
import org.mozilla.xiu.browser.R

fun SearchEngine(activity: Activity):String {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity /* Activity context */)
    var engine:String
    engine = if (sharedPreferences.getBoolean("switch_diy",false))
        sharedPreferences.getString("edit_diy","").toString()
    else
        sharedPreferences.getString("searchEngine",activity.getString(R.string.bing)).toString()
    sharedPreferences.registerOnSharedPreferenceChangeListener{ sharedPreferences, _ ->
        engine = if (sharedPreferences.getBoolean("switch_diy",false))
            sharedPreferences.getString("edit_diy","").toString()
        else
            sharedPreferences.getString("searchEngine",activity.getString(R.string.bing)).toString()
    }
    return engine
}