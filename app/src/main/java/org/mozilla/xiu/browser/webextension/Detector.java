package org.mozilla.xiu.browser.webextension;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.xiu.browser.download.UrlDetector;
import org.mozilla.xiu.browser.session.SessionDelegate;
import org.mozilla.xiu.browser.utils.CollectionUtil;
import org.mozilla.xiu.browser.utils.FileUtil;
import org.mozilla.xiu.browser.utils.HttpParser;
import org.mozilla.xiu.browser.utils.StringUtil;

import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2023/10/17
 * 时间：At 14:03
 */

public class Detector {
    private static List<String> videos = CollectionUtil.asList(".mp4", ".MP4", ".m3u8", ".flv", ".avi", ".3gp", "mpeg", ".wmv", ".mov", ".MOV", "rmvb", ".dat", ".mkv", "qqBFdownload", "mime=video%2F", "=video_mp4");

    public static boolean hasVideoTag(String url) {
        if (url == null || url.isEmpty()) return false;
        if (!url.contains("#ignoreVideo=true#")) {
            for (String music : videos) {
                if (url.contains(music)) {
                    return true;
                }
            }
        }
        return false;
    }

    private DetectorListener detectorListener;

    public void generateTypeAndSize(SessionDelegate sessionDelegate, TabRequest request) {
        try {
            //Log.d("test", "generateTypeAndSize: " + request.getUrl() + ", method: " + request.getMethod());
            if ("get".equalsIgnoreCase(request.getMethod())) {
                boolean hasM3u8 = request.getUrl() != null && request.getUrl().contains(".m3u8");
                String type = hasM3u8 ? TabRequest.VIDEO : TabRequest.OTHER;
                String size = null;
                JSONArray responseHeaders = request.getResponseHeaders();
                if (responseHeaders != null) {
                    for (int i = 0; i < responseHeaders.length(); i++) {
                        JSONObject header = responseHeaders.optJSONObject(i);
                        if (header == null) {
                            continue;
                        }
                        if ("content-type".equalsIgnoreCase(header.optString("name"))) {
                            String contentType = header.optString("value");
                            if (StringUtil.isNotEmpty(contentType)) {
                                if (contentType.startsWith("video/")) {
                                    if ((request.getUrl() != null && request.getUrl().contains(".ts")) || "video/mp2t".equals(contentType)) {
                                        type = TabRequest.OTHER;
                                    } else {
                                        type = TabRequest.VIDEO;
                                    }
                                } else if (contentType.startsWith("audio/")) {
                                    type = TabRequest.AUDIO;
                                } else if (contentType.startsWith("image/")) {
                                    if (hasM3u8) {
                                        //fuckImage
                                        type = TabRequest.VIDEO;
                                    } else {
                                        type = TabRequest.IMAGE;
                                    }
                                } else if (contentType.startsWith("text/")) {
                                    //                            if (hasM3u8 && !request.getUrl().contains("url=http")) {
                                    //                                type = TabRequest.VIDEO;
                                    //                            }
                                    if (hasM3u8) {
                                        type = TabRequest.VIDEO;
                                    } else {
                                        type = TabRequest.OTHER;
                                    }
                                } else {
                                    if ("application/vnd.apple.mpegurl".equals(contentType) || hasM3u8) {
                                        type = TabRequest.VIDEO;
                                    } else if (isStream(contentType) && hasVideoTag(request.getUrl())) {
                                        type = TabRequest.VIDEO;
                                    } else {
                                        type = TabRequest.OTHER;
                                    }
                                }
                            }
                        } else if ("content-length".equalsIgnoreCase(header.optString("name"))) {
                            size = header.optString("value");
                        }
                    }
                }
                request.setType(type);
                if (StringUtil.isNotEmpty(size)) {
                    long s = Long.parseLong(size);
                    request.setSize(FileUtil.getFormatedFileSize(s));
                }
                if (TabRequest.VIDEO.equals(type)) {
                    String[] urls = request.getUrl().split("\\?url=http");
                    if (urls.length > 1) {
                        TabRequest videoRequest = request.clone();
                        videoRequest.setUrl("http" + HttpParser.decodeUrl(urls[1].split("&")[0], "UTF-8"));
                        if (detectorListener != null) {
                            detectorListener.onFindVideo(sessionDelegate, videoRequest);
                        }
                    }
                    if (detectorListener != null) {
                        detectorListener.onFindVideo(sessionDelegate, request);
                    }
                } else if (TabRequest.OTHER.equals(type)) {
                    if (UrlDetector.isMusic(request.getUrl())) {
                        request.setType(TabRequest.AUDIO);
                    } else if (UrlDetector.isImage(request.getUrl())) {
                        request.setType(TabRequest.IMAGE);
                    }
                }
            } else {
                request.setType(TabRequest.OTHER);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setType(TabRequest.OTHER);
        }
        //Log.d("Detector", "generateTypeAndSize: " + request.getUrl() + ", type=" + request.getType() + ", size=" + request.getSize());
    }

    public DetectorListener getDetectorListener() {
        return detectorListener;
    }

    public void setDetectorListener(DetectorListener detectorListener) {
        this.detectorListener = detectorListener;
    }

    public static boolean isStream(String mime) {
        return "application/octet-stream".equals(mime) || "application/oct-stream".equals(mime) || "multipart/form-data".equals(mime);
    }
}