package org.mozilla.xiu.browser.video;

import androidx.annotation.Nullable;

import org.mozilla.xiu.browser.utils.HttpParser;
import org.mozilla.xiu.browser.utils.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：By 15968
 * 日期：On 2023/10/23
 * 时间：At 11:45
 */

public class PlayerChooser {

    public static String decorateHeader(@Nullable Map<String, String> headers, String videoUrl) {
        return decorateHeader0(headers, null, videoUrl, "");
    }

    public static String decorateHeader(@Nullable Map<String, String> headers, @Nullable String rp, String referer, String videoUrl) {
        return decorateHeader0(headers, null, videoUrl, "");
    }

    public static String decorateHeader0(@Nullable Map<String, String> headers, @Nullable String referer, String videoUrl, String cookie) {
        if (StringUtil.isEmpty(videoUrl)) {
            return videoUrl;
        }
        Map<String, String> hd = getHeaderMap(headers, referer, videoUrl, cookie);
        if (hd.isEmpty()) {
            return videoUrl;
        }
        return videoUrl + ";" + HttpParser.getHeadersStr(hd);
    }

    public static Map<String, String> getHeaderMap(@Nullable Map<String, String> headers, @Nullable String referer, String videoUrl, String cookie) {
        Map<String, String> hd = new HashMap<>();
        if (StringUtil.isEmpty(videoUrl)) {
            return hd;
        }
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if ("Accept".equalsIgnoreCase(entry.getKey())) {
                    continue;
                }
                if ("Range".equalsIgnoreCase(entry.getKey())) {
                    continue;
                }
                if ("Upgrade-Insecure-Requests".equalsIgnoreCase(entry.getKey())) {
                    continue;
                }
                if ("Host".equalsIgnoreCase(entry.getKey())) {
                    continue;
                }
                hd.put(entry.getKey(), entry.getValue());
            }
        }
        if (StringUtil.isNotEmpty(cookie) && !hd.containsKey("Cookie")) {
            hd.put("Cookie", cookie);
        }
        if (StringUtil.isNotEmpty(referer) && !hd.containsKey("Referer")) {
            hd.put("Referer", referer);
        }
        return hd;
    }
} 