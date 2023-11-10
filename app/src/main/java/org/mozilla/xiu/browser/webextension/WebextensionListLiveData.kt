package org.mozilla.xiu.browser.webextension

import androidx.lifecycle.LiveData
import org.mozilla.geckoview.WebExtension

class WebextensionListLiveData: LiveData<ArrayList<WebExtension>>() {
    fun Value(webExtension:ArrayList<WebExtension>){
        postValue(webExtension)
    }
    override fun onActive() {
        super.onActive()
    }

    override fun onInactive() {
        super.onInactive()
    }
    companion object {
        private lateinit var globalData: WebextensionListLiveData
        fun getInstance(): WebextensionListLiveData {
            globalData = if (Companion::globalData.isInitialized) globalData else WebextensionListLiveData()
            return globalData
        }
    }
}