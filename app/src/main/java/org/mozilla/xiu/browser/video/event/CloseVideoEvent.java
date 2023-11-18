package org.mozilla.xiu.browser.video.event;

/**
 * 作者：By 15968
 * 日期：On 2021/9/20
 * 时间：At 16:39
 */

public class CloseVideoEvent {
    private boolean checkPip;

    public CloseVideoEvent(boolean checkPip) {
        this.checkPip = checkPip;
    }

    public CloseVideoEvent() {

    }

    public boolean isCheckPip() {
        return checkPip;
    }

    public void setCheckPip(boolean checkPip) {
        this.checkPip = checkPip;
    }
}