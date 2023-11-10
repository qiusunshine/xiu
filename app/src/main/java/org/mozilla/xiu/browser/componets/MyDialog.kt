package org.mozilla.xiu.browser.componets

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.mozilla.xiu.browser.R


open class MyDialog(context: Context) : AlertDialog(context) {
    init {
        window!!.setBackgroundDrawable(context.getDrawable(R.drawable.bg_dialog))
    }
}