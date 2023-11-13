package org.mozilla.xiu.browser.session;

import org.mozilla.geckoview.GeckoSession;

/**
 * 作者：By 15968
 * 日期：On 2023/11/13
 * 时间：At 10:19
 */

public interface SessionProvider {
    GeckoSession current();
}
