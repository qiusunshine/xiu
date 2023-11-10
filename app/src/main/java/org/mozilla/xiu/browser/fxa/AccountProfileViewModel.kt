package org.mozilla.xiu.browser.fxa

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountProfileViewModel : ViewModel() {
    private var _data = MutableStateFlow(AccountProfile())
    val data = _data.asStateFlow()
    fun changeProfile(profile: AccountProfile) {
        _data.value = profile
    }
}