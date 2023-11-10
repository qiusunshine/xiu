package org.mozilla.xiu.browser.componets.popup

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReceivedTabPopupObervers :ViewModel(){
    private val _state = MutableStateFlow<Boolean>(false)
    val state = _state.asStateFlow()
    fun changeState(boolean: Boolean){
        _state.value = boolean
    }
}