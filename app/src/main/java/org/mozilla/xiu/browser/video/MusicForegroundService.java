package org.mozilla.xiu.browser.video;

/**
 * 作者：By 15968
 * 日期：On 2019/12/4
 * 时间：At 23:01
 */
public class MusicForegroundService {
    private static final int ONGOING_NOTIFICATION_ID = 3;
    /**
     * 歌曲播放
     */
    public static final String PLAY = "play";
    /**
     * 歌曲暂停
     */
    public static final String PAUSE = "pause";
    /**
     * 歌曲暂停
     */
    public static final String PAUSE_NOW = "pause_now";
    /**
     * 歌曲暂停
     */
    public static final String PAUSE_NOW_DISCONNECTED = "pause_now_when_disconnected";
    public static final String PLAY_NOW_CONNECTED = "play_now_when_connected";
    /**
     * 上一曲
     */
    public static final String PREV = "prev";
    /**
     * 下一曲
     */
    public static final String NEXT = "next";
    /**
     * 关闭通知栏
     */
    public static final String CLOSE = "close";

    public MusicForegroundService() {
    }
}
