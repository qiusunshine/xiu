package org.mozilla.xiu.browser.video;

import android.content.Context;

import org.mozilla.xiu.browser.video.model.DetectedMediaResult;
import org.mozilla.xiu.browser.video.model.MediaType;

import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2022/5/23
 * 时间：At 11:21
 */

public interface VideoDetector {
    void putIntoXiuTanLiked(Context context, String dom, String url);
    List<DetectedMediaResult> getDetectedMediaResults(String webUrl, MediaType mediaType);
}