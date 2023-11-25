package org.mozilla.xiu.browser.video.event;

import org.mozilla.xiu.browser.webextension.TabRequest;

/**
 * 作者：By 15968
 * 日期：On 2023/11/18
 * 时间：At 11:00
 */

public class FloatPlayEvent {
    private TabRequest request;

    public FloatPlayEvent(TabRequest request) {
        this.request = request;
    }

    public TabRequest getRequest() {
        return request;
    }

    public void setRequest(TabRequest request) {
        this.request = request;
    }
}