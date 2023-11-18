package org.mozilla.xiu.browser.webextension;

import androidx.annotation.Nullable;

import org.mozilla.geckoview.WebExtension;

/**
 * 作者：By 15968
 * 日期：On 2023/11/8
 * 时间：At 21:50
 */

public class WebExtensionConnectPortEvent {
    @Nullable
    private WebExtension.Port port;

    public WebExtensionConnectPortEvent(WebExtension.Port port) {
        this.port = port;
    }

    public WebExtension.Port getPort() {
        return port;
    }

    public void setPort(WebExtension.Port port) {
        this.port = port;
    }
}