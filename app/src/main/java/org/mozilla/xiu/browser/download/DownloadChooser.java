package org.mozilla.xiu.browser.download;

/**
 * 作者：By hdy
 * 日期：On 2019/1/28
 * 时间：At 14:53
 */
public class DownloadChooser {

    public static String smartFilm(String fileName) {
        return smartFilm(fileName, false);
    }

    public static String smartFilm(String fileName, boolean includeVideo) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        if (UrlDetector.isVideoOrMusic(fileName)) {
            if (!UrlDetector.isMusic(fileName)) {
                return includeVideo ? "视频" : "";
            } else {
                return "音乐/音频";
            }
        } else if (UrlDetector.isImage(fileName)) {
            return "图片";
        } else if (fileName.contains(".zip") || fileName.contains(".rar") || fileName.contains(".7z") || fileName.contains(".tar") || fileName.contains(".gz")) {
            return "压缩包";
        } else if (fileName.contains(".txt") || fileName.contains(".epub") || fileName.contains(".azw3") || fileName.contains(".mobi") || fileName.contains(".pdf")
                || fileName.contains(".doc") || fileName.contains(".xls") || fileName.contains(".json")) {
            return "文档/电子书";
        } else if (fileName.contains(".apk") || fileName.contains(".exe") || fileName.contains(".hap") || fileName.contains(".msi")
                || fileName.contains(".dmg") || fileName.contains(".xpi") || fileName.contains(".crx")) {
            return "安装包";
        }
        return "其它格式";
    }
}
