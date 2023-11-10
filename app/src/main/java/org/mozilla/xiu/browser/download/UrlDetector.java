package org.mozilla.xiu.browser.download;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.xiu.browser.utils.CollectionUtil;
import org.mozilla.xiu.browser.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 作者：By 15968
 * 日期：On 2023/10/19
 * 时间：At 11:52
 */

public class UrlDetector {

    private static List<String> apps = CollectionUtil.asList(".css", ".html", ".js", ".apk", ".apks", ".apk.1", ".exe", ".zip", ".rar", ".7z", ".hap", ".mtz");
    private static List<String> htmls = CollectionUtil.asList(".css", ".html", ".js", ".apk", ".exe");
    public static List<String> videos = CollectionUtil.asList(".mp4", ".MP4", ".m3u8", ".flv", ".avi", ".3gp", "mpeg", ".wmv", ".mov", ".MOV", "rmvb", ".dat", ".mkv", "qqBFdownload", "mime=video%2F", "=video_mp4");
    private static List<String> musics = CollectionUtil.asList(".mp3", ".wav", ".ogg", ".flac", ".m4a");
    private static List<String> images = CollectionUtil.asList(".ico", ".png", ".PNG", ".jpg", ".JPG", ".jpeg", ".JPEG", ".gif", ".GIF", ".webp", ".svg");
    private static List<String> blockUrls = CollectionUtil.asList(".php?url=http", "/?url=http");
    private static List<String> makeSureNotVideoRules = CollectionUtil.asList(".mp4.jp", ".mp4.png");

    public static List<String> getVideoRules() {
        return videoRules;
    }

    private static List<String> videoRules = Collections.synchronizedList(new ArrayList<>());

    public static String getNeedCheckUrl(String url) {
        url = url.replace("http://", "").replace("https://", "");
        if (!url.contains("/")) {
            return url;
        }
        String[] urls = url.split("/");
        if (urls.length > 1) {
            //去掉域名
            return StringUtil.listToString(Arrays.asList(urls), 1, "/");
        } else if (urls.length < 1) {
            return url;
        } else if ((urls[0] + "/").equals(url)) {
            return "";
        }
        return url;
    }

    public static boolean isVideoOrMusic(String url) {
        if (StringUtil.isEmpty(url)) {
            return false;
        }
        if (url.contains("isVideo=true") || url.contains("isMusic=true")) {
            return true;
        }
        if (url.contains("ignoreVideo=true") || url.contains("#ignoreMusic=true#")) {
            return false;
        }
        if (url.startsWith("x5Play://")) {
            return true;
        }
        if (url.contains("@rule=") || url.contains("@lazyRule=")) {
            return false;
        }
        if (url.startsWith("rtmp://")) {
            return true;
        } else if (url.startsWith("rtsp://") || url.contains("video://")) {
            return true;
        }
        url = getNeedCheckUrl(url);
        for (String html : makeSureNotVideoRules) {
            //拦截掉
            if (url.contains(html)) {
                return false;
            }
        }
        for (String rule : videoRules) {
            try {
                if (Pattern.matches(rule.split("@domain=")[0], url)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (String html : blockUrls) {
            //拦截掉
            if (url.contains(html)) {
                return false;
            }
        }
        for (String app : apps) {
            if (url.endsWith(app)) {
                return false;
            }
        }
        for (String music : musics) {
            if (url.contains(music)) {
                return true;
            }
        }
        for (String music : videos) {
            if (url.contains(music)) {
                return true;
            }
        }
        return false;
    }


    public static boolean isImage(String url) {
        if (StringUtil.isEmpty(url)) {
            return false;
        }
        if (url.startsWith("x5Play://")) {
            return false;
        }
        if (url.contains("@rule=") || url.contains("@lazyRule=") || url.contains("isVideo=true")) {
            return false;
        }
//    if (ImageUrlMapEnum.getIdByUrl(url) > 0) {
//      return true;
//    }
        for (String app : apps) {
            if (url.endsWith(app)) {
                return false;
            }
        }
        url = StringUtil.removeDom(url);
        if (url.contains("ignoreImg=true")) {
            return false;
        }
        for (String image : images) {
            if (url.contains(image)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMusic(String url) {
        if (StringUtil.isEmpty(url)) {
            return false;
        }
        if (url.contains("@rule=") || url.contains("@lazyRule=") || url.contains("#ignoreMusic=true#")) {
            return false;
        }
        if (url.contains("isMusic=true")) {
            return true;
        }
        for (String app : apps) {
            if (url.endsWith(app)) {
                return false;
            }
        }
        url = StringUtil.removeDom(url);
        for (String image : musics) {
            if (url.contains(image)) {
                return true;
            }
        }
        return false;
    }

  public static String clearTag(String url) {
    if (StringUtil.isEmpty(url)) {
      return url;
    }
    if (url.startsWith("x5Play://")) {
      url = StringUtils.replaceOnce(url, "x5Play://", "");
    }
    String[] tagList = new String[]{
            "#ignoreVideo=true#",
            "#isVideo=true#",
            "#ignoreImg=true#",
            "#immersiveTheme#",
            "#noRecordHistory#",
            "#noHistory#",
            "#noLoading#",
            "#isMusic=true#",
            "#ignoreMusic=true#",
            "#autoPage#",
            "#pre#",
            "#noPre#",
            "#fullTheme#",
            "#readTheme#",
            "#gameTheme#",
            "#noRefresh#",
            "#background#",
            "#autoCache#",
            "#cacheOnly#",
            "#originalSize#",
            "#memoryPage#"
    };
    for (String tag : tagList) {
      url = StringUtils.replaceOnce(url, tag, "");
    }
    return url;
  }
} 