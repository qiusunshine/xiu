package org.mozilla.xiu.browser.webextension;

import org.mozilla.geckoview.WebExtension;

/**
 * 作者：By 15968
 * 日期：On 2023/11/8
 * 时间：At 21:50
 */

public class WebExtensionsEnableEvent {
    private WebExtension webExtension;

    public WebExtensionsEnableEvent(WebExtension webExtension, boolean enabled) {
        this.webExtension = webExtension;
        this.enabled = enabled;
    }

    private boolean enabled;

    public WebExtension getWebExtension() {
        return webExtension;
    }

    public void setWebExtension(WebExtension webExtension) {
        this.webExtension = webExtension;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}