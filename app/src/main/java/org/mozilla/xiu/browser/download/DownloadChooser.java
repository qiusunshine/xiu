package org.mozilla.xiu.browser.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;

import org.mozilla.xiu.browser.utils.ClipboardUtil;
import org.mozilla.xiu.browser.utils.ToastMgr;

import java.util.List;
import java.util.Map;

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

    public static boolean startDownloadUseIDM(Context context, String name, String url) {
        try {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//268435456
            localIntent.setType("text/plain");
            localIntent.setAction("android.intent.action.SEND");
            localIntent.putExtra("android.intent.extra.TEXT", url);
            localIntent.putExtra("title", name);
            localIntent.setComponent(new ComponentName("idm.internet.download.manager.plus", "idm.internet.download.manager.Downloader"));
            context.startActivity(localIntent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用IDM+下载器失败！" + e.getMessage());
        }
        return true;
    }

    /**
     * @param context
     * @return null表示已安装，"表示没有推荐下载地址
     */
    public static String checkAndGetIDMUrl(Context context) {
        if (!appInstalledOrNot(context, "idm.internet.download.manager.plus")) {
            return "";
        }
        return null;
    }

    public static String getIDMInstalledPackage(Context context) {
        if (appInstalledOrNot(context, "idm.internet.download.manager.plus")) {
            return "idm.internet.download.manager.plus";
        }
        return null;
    }

    public static boolean startDownloadUseADM(Context context, String name, String url) {
        try {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//268435456
            localIntent.setType("text/plain");
            localIntent.setAction("android.intent.action.SEND");
            localIntent.putExtra("android.intent.extra.TEXT", url);
            localIntent.putExtra("title", name);
            localIntent.putExtra("name", name);
            ClipboardUtil.copyToClipboard(context, name);
            try {
                localIntent.setComponent(new ComponentName("com.dv.adm.pay", "com.dv.adm.pay.AEditor"));
                context.startActivity(localIntent);
            } catch (Exception e) {
                e.printStackTrace();
                localIntent.setComponent(new ComponentName("com.dv.adm", "com.dv.adm.AEditor"));
                context.startActivity(localIntent);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用ADM下载器失败！" + e.getMessage());
        }
        return true;
    }

    public static String getADMInstalledPackage(Context context) {
        if (appInstalledOrNot(context, "com.dv.adm.pay")) {
            return "com.dv.adm.pay";
        }
        if (appInstalledOrNot(context, "com.dv.adm")) {
            return "com.dv.adm";
        }
        return null;
    }

    /**
     * @param context
     * @return null表示已安装，"表示没有推荐下载地址
     */
    public static String checkAndGetADMUrl(Context context) {
        if (!appInstalledOrNot(context, "com.dv.adm.pay") && !appInstalledOrNot(context, "com.dv.adm")) {
            return "";
        }
        return null;
    }

    public static boolean startDownloadUseYoutoo(Context context, String name, String url, @Nullable Map<String, String> headers) {
        try {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//268435456
            localIntent.setType("text/plain");
            localIntent.setAction("android.intent.action.SEND");
            localIntent.putExtra("android.intent.extra.TEXT", url);
            localIntent.putExtra("title", name);
            localIntent.putExtra("url", url);
            localIntent.putExtra("download", true);
            if (headers != null) {
                localIntent.putExtra("headers", JSON.toJSONString(headers));
            }
            localIntent.setComponent(new ComponentName("com.hiker.youtoo", "com.example.hikerview.ui.home.ResolveIntentActivity"));
            context.startActivity(localIntent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用嗅觉浏览器失败！" + e.getMessage());
        }
        return true;
    }

    public static String getYoutooInstalledPackage(Context context) {
        if (appInstalledOrNot(context, "com.hiker.youtoo")) {
            return "com.hiker.youtoo";
        }
        return null;
    }

    /**
     * @param context
     * @return null表示已安装，"表示没有推荐下载地址
     */
    public static String checkAndGetYoutooUrl(Context context) {
        if (!appInstalledOrNot(context, "com.hiker.youtoo")) {
            return "https://haikuo.lanzouj.com/b0ekkjzi";
        }
        return null;
    }

    public static boolean startDownloadUseHiker(Context context, String name, String url, @Nullable Map<String, String> headers) {
        try {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//268435456
            localIntent.setType("text/plain");
            localIntent.setAction("android.intent.action.SEND");
            localIntent.putExtra("android.intent.extra.TEXT", url);
            localIntent.putExtra("title", name);
            localIntent.putExtra("url", url);
            localIntent.putExtra("download", true);
            if (headers != null) {
                localIntent.putExtra("headers", JSON.toJSONString(headers));
            }
            if (appInstalledOrNot(context, "com.example.hikerview.dev")) {
                localIntent.setComponent(new ComponentName("com.example.hikerview.dev", "com.example.hikerview.ui.home.ResolveIntentActivity"));
            } else {
                localIntent.setComponent(new ComponentName("com.example.hikerview", "com.example.hikerview.ui.home.ResolveIntentActivity"));
            }
            context.startActivity(localIntent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "调用海阔视界失败！" + e.getMessage());
        }
        return true;
    }

    /**
     * @param context
     * @return null表示已安装，"表示没有推荐下载地址
     */
    public static String checkAndGetHikerUrl(Context context) {
        if (!appInstalledOrNot(context, "com.example.hikerview")) {
            return "https://haikuo.lanzouj.com/b0ekkjzi";
        }
        return null;
    }

    public static String getHikerInstalledPackage(Context context) {
        if (appInstalledOrNot(context, "com.example.hikerview.dev")) {
            return "com.example.hikerview.dev";
        }
        if (appInstalledOrNot(context, "com.example.hikerview")) {
            return "com.example.hikerview";
        }
        return null;
    }

    public static boolean appInstalledOrNot(Context context, String paramString) {
        try {
            PackageManager packageManager = context.getPackageManager();// 获取packagemanager
            List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);// 获取所有已安装程序的包信息
            if (pinfo != null) {
                for (int i = 0; i < pinfo.size(); i++) {
                    String pn = pinfo.get(i).packageName;
                    if (paramString.equals(pn)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            PackageManager pm = context.getPackageManager();
            try {
                pm.getPackageInfo(paramString, PackageManager.GET_ACTIVITIES);
                return true;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
