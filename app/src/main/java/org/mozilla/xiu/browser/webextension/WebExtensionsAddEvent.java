package org.mozilla.xiu.browser.webextension;

/**
 * 作者：By 15968
 * 日期：On 2023/11/8
 * 时间：At 21:50
 */

public class WebExtensionsAddEvent {
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    private String path;

    public WebExtensionsAddEvent(String path) {
        this.path = path;
    }
} 