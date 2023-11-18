package org.mozilla.xiu.browser.webextension;

import org.mozilla.xiu.browser.session.SessionDelegate;

/**
 * 作者：By 15968
 * 日期：On 2023/10/17
 * 时间：At 14:04
 */

public interface DetectorListener {
    void onFindVideo(SessionDelegate session, TabRequest request);
}
