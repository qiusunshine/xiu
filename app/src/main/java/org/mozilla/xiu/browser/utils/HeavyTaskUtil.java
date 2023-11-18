package org.mozilla.xiu.browser.utils;

import android.content.Context;

import org.mozilla.xiu.browser.App;
import org.mozilla.xiu.browser.video.model.ViewCollectionExtraData;

/**
 * 作者：By 15968
 * 日期：On 2023/10/14
 * 时间：At 22:57
 */

public class HeavyTaskUtil {

    public static void executeNewTask(Runnable command) {
        ThreadTool.INSTANCE.executeNewTask(command);
    }


    /**
     * 获取片头片尾，优先从收藏、历史中取，其次再从全局取
     *
     * @param context
     * @param MTitle
     * @param CUrl
     * @return
     */
    public static ViewCollectionExtraData findJumpPos(Context context, String MTitle, String CUrl) {
//        ViewCollection viewCollection = HeavyTaskUtil.findCollection(MTitle, CUrl);
//        if (viewCollection != null) {
//            ViewCollectionExtraData extraData = ViewCollectionExtraData.fromJson(viewCollection.getExtraData());
//            if (extraData.getJumpStartDuration() >= 0 || extraData.getJumpEndDuration() > 0) {
//                return extraData;
//            }
//        }
        ViewCollectionExtraData extraData = new ViewCollectionExtraData();
        int jumpStartDuration = PreferenceMgr.getInt(context, "jumpStartDuration", 0);
        int jumpEndDuration = PreferenceMgr.getInt(context, "jumpEndDuration", 0);
        extraData.setJumpStartDuration(jumpStartDuration);
        extraData.setJumpEndDuration(jumpEndDuration);
        return extraData;
    }


    /**
     * 保存片头片尾
     *
     * @param MTitle
     * @param CUrl
     * @return
     */
    public static void updateJumpPos(String MTitle, String CUrl,
                                     int jumpStartDuration, int jumpEndDuration) {
        if (MTitle == null || CUrl == null) {
            PreferenceMgr.put(App.Companion.getContext(), "jumpStartDuration", jumpStartDuration);
            PreferenceMgr.put(App.Companion.getContext(), "jumpEndDuration", jumpEndDuration);
            return;
        }
    }

    public static void clearJumpPos(String MTitle, String CUrl,
                                    int jumpStartDuration, int jumpEndDuration) {
        if (MTitle == null || CUrl == null) {
            PreferenceMgr.put(App.Companion.getContext(), "jumpStartDuration", jumpStartDuration);
            PreferenceMgr.put(App.Companion.getContext(), "jumpEndDuration", jumpEndDuration);
            return;
        }
        boolean cleared = false;
        if (!cleared) {
            //用的是全局的
            PreferenceMgr.put(App.Companion.getContext(), "jumpStartDuration", jumpStartDuration);
            PreferenceMgr.put(App.Companion.getContext(), "jumpEndDuration", jumpEndDuration);
        }
    }
} 