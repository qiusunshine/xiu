package org.mozilla.xiu.browser.dlan;

/**
 * 作者：By 15968
 * 日期：On 2021/10/28
 * 时间：At 23:54
 */

public class DlanPlayEvent {
    private String title;
    private String url;
    private String headers;

    public DlanPlayEvent() {
    }

    public DlanPlayEvent(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public DlanPlayEvent(String title, String url, String headers) {
        this.title = title;
        this.url = url;
        this.headers = headers;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }
}