package org.mozilla.xiu.browser.webextension

import org.mozilla.geckoview.WebExtension

/**
 * 作者：By 15968
 * 日期：On 2023/11/14
 * 时间：At 10:08
 */
data class WebExtensionWrapper(
    var name: String?,
    var enabled: Boolean,
    var extension: WebExtension
)
