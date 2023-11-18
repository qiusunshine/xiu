package org.mozilla.xiu.browser.download

/**
 * 作者：By 15968
 * 日期：On 2023/11/15
 * 时间：At 17:01
 */
data class DownloadFileWithPermissionEvent(
    var task: () -> Unit
)
