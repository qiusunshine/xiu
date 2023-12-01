package org.mozilla.xiu.browser.webextension;

import org.mozilla.xiu.browser.base.VarHolder;

import java.util.concurrent.CountDownLatch;

/**
 * 作者：By 15968
 * 日期：On 2023/11/30
 * 时间：At 23:56
 */

public class InputHeightListenPostEvent {
    public CountDownLatch countDownLatch;
    public volatile VarHolder<Boolean> isUp;

    public InputHeightListenPostEvent(CountDownLatch countDownLatch, VarHolder<Boolean> isUp) {
        this.countDownLatch = countDownLatch;
        this.isUp = isUp;
    }
} 