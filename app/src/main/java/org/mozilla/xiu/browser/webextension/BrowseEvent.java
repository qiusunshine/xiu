package org.mozilla.xiu.browser.webextension;

/**
 * 作者：By 15968
 * 日期：On 2023/11/8
 * 时间：At 21:50
 */

public class BrowseEvent {

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private String url;

    public BrowseEvent(String url) {
        this.url = url;
    }
} 