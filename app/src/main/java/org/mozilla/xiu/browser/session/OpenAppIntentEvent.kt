package org.mozilla.xiu.browser.session

import android.content.Intent
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult

/**
 * 作者：By 15968
 * 日期：On 2023/11/18
 * 时间：At 11:29
 */
data class OpenAppIntentEvent(
    var intent: Intent,
    var result: GeckoResult<AllowOrDeny>
)
