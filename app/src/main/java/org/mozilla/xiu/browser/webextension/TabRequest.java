package org.mozilla.xiu.browser.webextension;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：By 15968
 * 日期：On 2023/10/17
 * 时间：At 13:19
 */

public class TabRequest {

    public static final String ALL = "全部";
    public static final String OTHER = "其它";
    public static final String VIDEO = "视频";
    public static final String AUDIO = "音频";
    public static final String IMAGE = "图片";


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public JSONArray getRequestHeaders() {
        return requestHeaders;
    }

    public Map<String, String> getRequestHeaderMap() {
        if (requestHeaders == null) {
            return new HashMap<>();
        }
        Map<String, String> map = new HashMap<>();
        try {
            for (int i = 0; i < requestHeaders.length(); i++) {
                JSONObject o = requestHeaders.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                map.put(o.optString("name"), o.optString("value"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public Map<String, String> getResponseHeaderMap() {
        if (responseHeaders == null) {
            return new HashMap<>();
        }
        if (requestHeadersMapInner != null) {
            return requestHeadersMapInner;
        }
        requestHeadersMapInner = new HashMap<>();
        try {
            for (int i = 0; i < responseHeaders.length(); i++) {
                JSONObject o = responseHeaders.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                requestHeadersMapInner.put(o.optString("name"), o.optString("value"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return requestHeadersMapInner;
    }

    public void setRequestHeaders(JSONArray requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public JSONArray getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(JSONArray responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    private String url;
    private String documentUrl;
    private String method;
    private int statusCode;
    private JSONArray requestHeaders;

    private Map<String, String> requestHeadersMapInner;
    private JSONArray responseHeaders;

    private String type;
    private String size;
    private int tabId;

    public TabRequest() {

    }

    public TabRequest clone() {
        TabRequest tabRequest = new TabRequest();
        tabRequest.setUrl(url);
        tabRequest.setDocumentUrl(documentUrl);
        tabRequest.setMethod(method);
        tabRequest.setStatusCode(statusCode);
        tabRequest.setRequestHeaders(requestHeaders);
        tabRequest.setResponseHeaders(responseHeaders);
        tabRequest.setType(type);
        tabRequest.setSize(size);
        tabRequest.setTabId(tabId);
        return tabRequest;
    }

    public TabRequest(JSONObject jsonObject) {
        try {
            setUrl(jsonObject.optString("url"));
            setStatusCode(jsonObject.optInt("statusCode"));
            setMethod(jsonObject.optString("method"));
            String du = jsonObject.optString("documentUrl");
            setDocumentUrl(du);
            setRequestHeaders(jsonObject.optJSONArray("requestHeaders"));
            setResponseHeaders(jsonObject.optJSONArray("responseHeaders"));
            setTabId(jsonObject.optInt("tabId"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TabRequest(String url, String documentUrl, String method, int statusCode, JSONArray requestHeaders, JSONArray responseHeaders) {
        this.url = url;
        this.documentUrl = documentUrl;
        this.method = method;
        this.statusCode = statusCode;
        this.requestHeaders = requestHeaders;
        this.responseHeaders = responseHeaders;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getTabId() {
        return tabId;
    }

    public void setTabId(int tabId) {
        this.tabId = tabId;
    }
}