package org.mozilla.xiu.browser.session

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SessionViewModel() : ViewModel() {
    val currentSession: MutableLiveData<SessionDelegate> by lazy {
        MutableLiveData<SessionDelegate>()
    }
}