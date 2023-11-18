package org.mozilla.xiu.browser.video

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import chuangyuan.ycj.videolibrary.listener.VideoInfoListener
import chuangyuan.ycj.videolibrary.video.ExoUserPlayer
import chuangyuan.ycj.videolibrary.video.GestureVideoPlayer.DoubleTapArea
import chuangyuan.ycj.videolibrary.video.ManualPlayer
import chuangyuan.ycj.videolibrary.video.VideoPlayerManager
import chuangyuan.ycj.videolibrary.widget.VideoPlayerView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener.EventTime
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.BaseTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.util.MimeTypes
import com.lxj.xpopup.XPopup
import com.qingfeng.clinglibrary.service.manager.ClingManager
import org.apache.commons.lang3.StringUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.dlan.DLandataInter
import org.mozilla.xiu.browser.dlan.DlanListPop
import org.mozilla.xiu.browser.dlan.DlanListPopUtil
import org.mozilla.xiu.browser.dlan.MediaPlayActivity
import org.mozilla.xiu.browser.download.UrlDetector
import org.mozilla.xiu.browser.utils.ClipboardUtil
import org.mozilla.xiu.browser.utils.CollectionUtil
import org.mozilla.xiu.browser.utils.DisplayUtil
import org.mozilla.xiu.browser.utils.HeavyTaskUtil
import org.mozilla.xiu.browser.utils.HttpParser
import org.mozilla.xiu.browser.utils.PreferenceMgr
import org.mozilla.xiu.browser.utils.ScreenUtil
import org.mozilla.xiu.browser.utils.StringUtil
import org.mozilla.xiu.browser.utils.TimeUtil
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.video.event.CloseVideoEvent
import org.mozilla.xiu.browser.video.event.MusicAction
import org.mozilla.xiu.browser.video.event.OnDeviceUpdateEvent
import org.mozilla.xiu.browser.video.model.DetectedMediaResult
import org.mozilla.xiu.browser.video.model.MediaType
import org.mozilla.xiu.browser.video.model.SimpleAnalyticsListener
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max

/**
 * 作者：By 15968
 * 日期：On 2022/5/22
 * 时间：At 10:54
 */
class FloatVideoController(
    var context: Activity,
    var container: ViewGroup,
    var pauseWebView: (pause: Boolean, force: Boolean) -> Int,
    var videoDetector: VideoDetector,
    var webHolder: WebHolder
) : View.OnClickListener {

    private var url: String = ""
    private var webUrl: String = ""
    private var title: String = ""
    private var position: Long = 0
    private var initPlayPos: Long = 0
    private var showing = false
    private var holderView: View? = null

    private var exo_bg_video_top: View? = null
    private var custom_lock_screen_bg: android.view.View? = null
    private var custom_control_bottom: android.view.View? = null
    private var exo_controller_bottom: android.view.View? = null
    private var exo_play_pause2: ImageView? = null
    private var exo_pip: View? = null
    private var layoutNow = VideoPlayerView.Layout.VERTICAL
    private var player: ManualPlayer? = null
    private var playerView: VideoPlayerView? = null
    private var listCard: View? = null
    private val timeView: TextView? = null
    private var descView: android.widget.TextView? = null
    private var listScrollView: ScrollView? = null
    private var video_address_view: android.widget.TextView? = null
    private var dlanListPop: DlanListPop? = null
    private var webDlanPlaying: Boolean = false
    private var analyticsListener: SimpleAnalyticsListener? = null
    private var vaildTicket: Long = 0
    private var trackListener: Player.Listener? = null
    private var autoSelectAudioTextFinished = false
    private var audio_tracks: LinearLayout? = null
    private var subtitle_tracks: LinearLayout? = null
    private var video_tracks: LinearLayout? = null
    private var jumpStartView: TextView? = null
    private var jumpStartDuration = 0
    private var bluetoothBroadcastReceiver: BroadcastReceiver? = null
    private var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var lastPlaying = false

    /**
     * 初始化View
     */
    @SuppressLint("UseCompatLoadingForDrawables", "MissingInflatedId")
    private fun initView() {
        if (playerView != null) {
            return
        }
        val view = LayoutInflater.from(context).inflate(R.layout.view_float_video, null)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        container.addView(view, params)
        holderView = view
        audio_tracks = view.findViewById(R.id.audio_tracks)
        subtitle_tracks = view.findViewById(R.id.subtitle_tracks)
        video_tracks = view.findViewById(R.id.video_tracks)
        val pv = view.findViewById(R.id.float_video_player) as VideoPlayerView
        val layoutParams = pv.layoutParams as FrameLayout.LayoutParams
        layoutParams.height = ScreenUtil.getScreenMin(context) * 9 / 16
        pv.layoutParams = layoutParams
        exo_bg_video_top = view.findViewById(R.id.exo_controller_top)
        custom_lock_screen_bg = view.findViewById(R.id.custom_lock_screen_bg)
        custom_control_bottom = view.findViewById(R.id.custom_control_bottom)
        exo_controller_bottom =
            view.findViewById(com.google.android.exoplayer2.ui.R.id.exo_controller_bottom)
        exo_play_pause2 = view.findViewById(R.id.exo_play_pause2)
        exo_pip = view.findViewById(R.id.exo_pip)
        exo_pip?.setOnClickListener(this)
        descView = view.findViewById(R.id.custom_toolbar_desc)
        pv.bottomAnimateViews = listOf(
            custom_lock_screen_bg,
            custom_control_bottom,
            view.findViewById(com.google.android.exoplayer2.ui.R.id.exo_video_fullscreen),
            view.findViewById(com.google.android.exoplayer2.ui.R.id.exo_video_switch),
            exo_play_pause2
        )
        exo_play_pause2?.setOnClickListener { v: View? ->
            player?.let {
                it.setStartOrPause(
                    !it.isPlaying
                )
            }
        }
        pv.exoFullscreen.setOnClickListener { v: View? ->
            enterFullScreen(v)
        }
        //片头片尾
        jumpStartView = pv.findViewById(R.id.jump_start)
        pv.rightAnimateView = pv.findViewById(R.id.exo_right_group)
        pv.exoControlsBack.setOnClickListener {
            if (layoutNow != VideoPlayerView.Layout.VERTICAL) {
                onBackPressed()
            } else {
                destroy()
            }
        }
        pv.setOnClickListener {

        }
        playerView = pv
        pv.playerView.setShowControllerIndefinitely(true)
        pv.setOnLayoutChangeListener {
            layoutNow = it
            if (it == VideoPlayerView.Layout.VERTICAL) {
                //非全屏
                if (listCard!!.visibility == View.VISIBLE) {
                    listCard!!.visibility = View.INVISIBLE
                }
            }
            if (it == VideoPlayerView.Layout.VERTICAL) {
                playerView?.playbackControlView?.postDelayed({
                    playerView?.playerView?.isShowControllerIndefinitely = true
                    playerView?.showControllerForce()
                    playerView?.playbackControlView?.postDelayed({
                        playerView?.showControllerForce()
                    }, 500)
                }, 300)
                pauseWebView(false, true)
                val lpa: WindowManager.LayoutParams = context.window.attributes
                lpa.screenBrightness = BRIGHTNESS_OVERRIDE_NONE
                context.window.attributes = lpa
            } else {
                pauseWebView(true, true)
                playerView!!.playerView.isShowControllerIndefinitely = false
            }
            //记忆进度
            player?.let { p ->
                if (p.currentPosition > 0) {
                    addPlayerPosition(context, getMemoryId(), p.currentPosition)
                }
            }
        }
        pv.playbackControlView
            .setPlayPauseListener { isPlaying: Boolean ->
                exo_play_pause2?.setImageDrawable(
                    context.resources
                        .getDrawable(if (isPlaying) R.drawable.ic_pause_ else R.drawable.ic_play_)
                )
            }
        pv.findViewById<View>(R.id.custom_download).setOnClickListener {
            var t: String = title.replace(" ", "")
            if (t.length > 85) {
                t = t.substring(0, 85)
            }
            ToastMgr.shortCenter(context, "暂不支持此功能")
            //todo DownloadDialogUtil.showEditDialog(context, t, url)
        }
        val networkNotify =
            PreferenceMgr.getBoolean(
                context,
                "networkNotify",
                true
            )
        pv.isNetworkNotify = networkNotify
        initActionAdapter()
        ScreenUtil.setDisplayInNotch(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            exo_pip?.visibility = View.VISIBLE
        }
        try {
            bluetoothBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action
                    if (action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) {
                        val state = intent?.getIntExtra(
                            BluetoothAdapter.EXTRA_CONNECTION_STATE,
                            BluetoothAdapter.STATE_DISCONNECTED
                        )
                        if (state == BluetoothAdapter.STATE_DISCONNECTED) {
                            // 处理蓝牙断开的逻辑
                            onMusicAction(MusicAction(MusicForegroundService.PAUSE_NOW_DISCONNECTED))
                        } else if (state == BluetoothAdapter.STATE_CONNECTED) {
                            // 处理蓝牙连上的逻辑
                            onMusicAction(MusicAction(MusicForegroundService.PLAY_NOW_CONNECTED))
                        }
                    } else if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                        // 处理蓝牙设备断开连接事件
                        onMusicAction(MusicAction(MusicForegroundService.PAUSE_NOW_DISCONNECTED))
                    } else if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                        // 处理蓝牙设备重新连接事件
                        onMusicAction(MusicAction(MusicForegroundService.PLAY_NOW_CONNECTED))
                    }
                }
            }
            val filter = IntentFilter()
            filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            context.registerReceiver(bluetoothBroadcastReceiver, filter)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        try {
            audioFocusChangeListener = object : AudioManager.OnAudioFocusChangeListener {
                private var lastPlaying = false
                override fun onAudioFocusChange(focusChange: Int) {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        // 处理丢失焦点事件，暂停音频播放
                        lastPlaying = player != null && player!!.isPlaying
                        onMusicAction(MusicAction(MusicForegroundService.PAUSE_NOW))
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // 处理获得焦点事件，恢复音频播放
                        if (lastPlaying) {
                            player!!.setStartOrPause(true)
                            //消费一次，重置
                            lastPlaying = false
                        }
                    }
                }
            }
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // 请求音频焦点
            val result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // 获得了音频焦点，可以开始播放视频
                //playVideo();
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            phoneStateListener = object : PhoneStateListener() {
                private var lastPlaying = false
                override fun onCallStateChanged(state: Int, phoneNumber: String) {
                    super.onCallStateChanged(state, phoneNumber)
                    when (state) {
                        TelephonyManager.CALL_STATE_IDLE ->                         //System.out.println("挂断");
                            if (lastPlaying) {
                                player!!.setStartOrPause(true)
                                //消费一次，重置
                                lastPlaying = false
                            }

                        TelephonyManager.CALL_STATE_OFFHOOK -> {}
                        TelephonyManager.CALL_STATE_RINGING -> {
                            //响铃
                            lastPlaying = player != null && player!!.isPlaying
                            onMusicAction(MusicAction(MusicForegroundService.PAUSE_NOW))
                        }
                    }
                }
            }
            (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).listen(
                phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun enterFullScreen(anchor: View?) {
        if (anchor == null) {
            enterFullScreen0(false)
        } else {
            val format = player?.player?.videoFormat
            if (playerView?.isNowVerticalFullScreen == true && format != null && format.width > format.height) {
                //正在竖屏全屏，并且本身是横向视频
                XPopup.Builder(getContext())
                    .atView(anchor)
                    .asAttachList(
                        arrayOf("退出全屏", "横向全屏"), null
                    ) { p: Int, _: String? ->
                        enterFullScreen0(p == 1)
                    }
                    .show()
                playerView?.exoFullscreen?.isChecked = true
            } else {
                enterFullScreen0(false)
            }
        }
    }

    private fun enterFullScreen0(verticalFullClickJustLandFull: Boolean) {
        val format = player?.player?.videoFormat
        playerView?.setVerticalFullClickJustLandFull(verticalFullClickJustLandFull)
        if (format != null) {
            if (format.width < format.height) {
                playerView?.verticalFullScreen()
            } else {
                playerView?.enterFullScreen()
            }
        } else {
            playerView?.enterFullScreen()
        }
    }

    /**
     * 加载新的网站就销毁
     */
    fun loadUrl(url: String) {
        val dom = StringUtil.getDom(webUrl)
        val domNow = StringUtil.getDom(url)
        if (!StringUtils.equals(dom, domNow) && holderView != null && showing) {
            destroy()
        }
    }

    /**
     * 嗅探到视频就显示
     */
    fun show(
        videoUrl: String,
        webUrl: String,
        title: String,
        headers: Map<String, String>?,
        checkDuplicate: Boolean = true
    ) {
        if (checkDuplicate && StringUtils.equals(webUrl, this.webUrl)) {
            return
        }
        videoDetector.putIntoXiuTanLiked(
            getContext(),
            StringUtil.getDom(webUrl),
            StringUtil.getDom(videoUrl)
        )
        val u = PlayerChooser.decorateHeader(
            headers,
            videoUrl
        )
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        vaildTicket = System.currentTimeMillis()
        this.title = title
        this.webUrl = webUrl
        initView()
        if (player != null && url.isNotEmpty()) {
            addPlayerPosition(context, getMemoryId(), player!!.currentPosition)
        }
        updateUrl(u)
        showing = true
        if (player != null) {
            reStartPlayer(true)
            return
        }
        val realUrl = HttpParser.getRealUrlFilterHeaders(url)
        //恢复播放进度
        initPlayPos = getPlayPos(context, getMemoryId())
        val extraData = HeavyTaskUtil.findJumpPos(context, null, null)
        jumpStartDuration = extraData.jumpStartDuration
        if (jumpStartDuration > 0 && initPlayPos < jumpStartDuration * 1000) {
            initPlayPos = (jumpStartDuration * 1000).toLong()
        }
        position = initPlayPos
        player = VideoPlayerManager.Builder(VideoPlayerManager.TYPE_PLAY_MANUAL, playerView!!)
            .setTitle("")
            .setPosition(position)
            .setPlayUri(realUrl, HttpParser.getHeaders(url))
            .addVideoInfoListener(object : VideoInfoListener {
                override fun onPlayReady(currPosition: Long) {
                    pauseWebDelay()
                    player?.let {
                        vaildTicket = System.currentTimeMillis()
                        if (it.duration > 0 && it.duration <= 20000 && it.player?.isCurrentWindowLive != true) {
                            //小于等于20秒的广告
                            val count = max(5, (it.duration / 2000).toInt() + 1)
                            autoChangeXiuTanVideo(count, vaildTicket)
                        }
                    }
                }

                override fun onPlayStart(currPosition: Long) {
                    player?.let {
                        if (it.duration > 200000) {
                            if (initPlayPos > 0 && it.duration - initPlayPos < 10000) {
                                //跳过片头后视频长度不足5分钟，或者上次播放位置已经到最后5分钟
                                initPlayPos = 0
                                it.seekTo(0)
                                ToastMgr.shortBottomCenter(
                                    getContext(),
                                    "上次播放剩余时长不足10秒，已重新播放"
                                );
                            }
                        }
                    }
                    //关闭之前的小窗窗口
                    if (EventBus.getDefault().hasSubscriberForEvent(CloseVideoEvent::class.java)) {
                        EventBus.getDefault().post(CloseVideoEvent(true))
                    }
                }

                override fun onLoadingChanged() {}
                override fun onPlayerError(e: ExoPlaybackException?) {
                    vaildTicket = System.currentTimeMillis()
                    autoChangeXiuTanVideo(5, vaildTicket)
                }

                override fun onPlayEnd() {

                }

                override fun isPlaying(playWhenReady: Boolean) {}
            }).create()
        player?.setPlaybackParameters(VideoPlayerManager.PLAY_SPEED, 1f)
        player?.setPlayerGestureOnTouch(true)
        addFormatListener()
        initTrackListener()
        player?.setVerticalMoveGestureListener { dx, dy ->
            if (abs(dy) > abs(dx) && player != null) {
                //竖向滑动
                holderView?.let {
                    var y = it.y + dy
                    if (y < 0) {
                        y = 0F
                    }
                    if (y + it.measuredHeight > container.measuredHeight) {
                        y = container.measuredHeight.toFloat() - it.measuredHeight
                    }
                    it.translationY = y
                }
            }
        }
        player?.setOnDoubleTapListener { e: MotionEvent?, tapArea: DoubleTapArea? ->
            if (player == null) {
                return@setOnDoubleTapListener
            }
            if (tapArea == DoubleTapArea.LEFT) {
                fastPositionJump(-10L)
                showFastJumpNotice(-10)
            } else if (tapArea == DoubleTapArea.RIGHT) {
                fastPositionJump(10L)
                showFastJumpNotice(10)
            } else {
                player?.setStartOrPause(!player!!.isPlaying)
            }
        }
        player?.startPlayer<ExoUserPlayer>()
        //停止网页里面的播放
        playerView?.postDelayed({
            if (playerView != null && player != null && url.isNotEmpty()) {
                pauseWebDelay()
            }
        }, 1000)
        if (jumpStartDuration > 0) {
            jumpStartView!!.text = TimeUtil.secToTime(jumpStartDuration)
        }
        jumpStartView!!.setOnClickListener { v: View? ->
            if (player == null || player!!.duration < 1) {
                return@setOnClickListener
            }
            val now = player!!.currentPosition
            if (now > 1000 * 60 * 10) {
                ToastMgr.shortBottomCenter(
                    getContext(),
                    "点击我会将当前进度设置为片头，当前进度超过10分钟，不能设为片头"
                )
                return@setOnClickListener
            }
            jumpStartDuration = (now / 1000).toInt()
            val str =
                TimeUtil.secToTime(jumpStartDuration)
            jumpStartView!!.text = str
            HeavyTaskUtil.updateJumpPos(null, null, jumpStartDuration, 0)
            ToastMgr.shortBottomCenter(getContext(), "全局片头已设置为$str")
        }
        jumpStartView!!.setOnLongClickListener { v: View? ->
            jumpStartDuration = 0
            jumpStartView!!.text = "片头"
            HeavyTaskUtil.updateJumpPos(null, null, jumpStartDuration, 0)
            ToastMgr.shortBottomCenter(getContext(), "全局片头已清除")
            true
        }
        //上次拉伸模式
        val resizeMode = PreferenceMgr.get(
            getContext(),
            "ijkplayer",
            "resizeMode",
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        ) as Int
        when (resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> descView!!.text =
                "速度×" + VideoPlayerManager.PLAY_SPEED + "/自适应"

            AspectRatioFrameLayout.RESIZE_MODE_FILL -> descView!!.text =
                "速度×" + VideoPlayerManager.PLAY_SPEED + "/充满"

            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> descView!!.text =
                "速度×" + VideoPlayerManager.PLAY_SPEED + "/裁剪"

            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> descView!!.text =
                "速度×" + VideoPlayerManager.PLAY_SPEED + "/宽度"

            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> descView!!.text =
                "速度×" + VideoPlayerManager.PLAY_SPEED + "/高度"
        }
        playerView?.playerView?.resizeMode = resizeMode
    }

    /**
     * activity onPause
     */
    fun onPause() {
        player?.onPause()
        player?.let {
            if (it.currentPosition > 0) {
                addPlayerPosition(context, getMemoryId(), it.currentPosition)
            }
        }
    }

    /**
     * activity onResume
     */
    fun onResume() {
        player?.onResume()
        initTrackListener()
    }

    /**
     * activity onBackPressed
     */
    fun onBackPressed(): Boolean {
        if (playerView != null && playerView!!.exoFullscreen.isChecked) {
            if (listCard?.visibility == View.VISIBLE) {
                reverseListCardVisibility()
                return true
            }
            playerView?.exitFullView()
            return true
        } else if (playerView != null) {
            if (playerView?.isNowVerticalFullScreen == true) {
                playerView?.verticalFullScreen()
                if (listCard!!.visibility == View.VISIBLE) {
                    reverseListCardVisibility()
                }
                return true
            }
            player?.onBackPressed()
        }
        return false
    }

    fun exitFullScreen(): Boolean {
        if (playerView != null && playerView!!.exoFullscreen.isChecked) {
            if (listCard?.visibility == View.VISIBLE) {
                reverseListCardVisibility()
            }
            playerView?.exitFullView()
            return true
        } else if (playerView != null) {
            if (playerView?.isNowVerticalFullScreen == true) {
                playerView?.verticalFullScreen()
                if (listCard!!.visibility == View.VISIBLE) {
                    reverseListCardVisibility()
                }
                return true
            }
            player?.onBackPressed()
        }
        return false
    }

    fun isFullScreen(): Boolean {
        return layoutNow != VideoPlayerView.Layout.VERTICAL
    }

    /**
     * 销毁、隐藏
     */
    fun destroy() {
        //必须先退出全屏，否则无法隐藏播放器界面
        val mid = getMemoryId()
        exitFullScreen()
        showing = false
        webUrl = ""
        url = ""
        position = 0
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        try {
            if (bluetoothBroadcastReceiver != null) {
                context.unregisterReceiver(bluetoothBroadcastReceiver)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        try {
            if (audioFocusChangeListener != null) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            if (phoneStateListener != null) {
                (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).listen(
                    phoneStateListener,
                    PhoneStateListener.LISTEN_NONE
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        if (holderView != null) {
            container.removeView(holderView)
            holderView = null
        }
        player?.let {
            try {
                val p = it.currentPosition
                player?.onDestroy()
                player = null
                if (p > 0) {
                    addPlayerPosition(context, mid, p)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            playerView?.onDestroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        playerView = null
        pauseWebView(false, true)
    }


    private fun addFormatListener() {
        if (analyticsListener == null) {
            analyticsListener = object : SimpleAnalyticsListener() {
                override fun onDecoderInputFormatChanged(
                    eventTime: EventTime,
                    trackType: Int,
                    format: Format
                ) {
                    playerView?.let {
                        if (trackType == C.TRACK_TYPE_VIDEO) {
                            updateVideoFormatView(format)
                        } else if (trackType == C.TRACK_TYPE_AUDIO) {
//                            audio_str_view!!.text = "音频格式：" + getAudioString(format)
                        }
//                        if (format.width > format.height) {
//                            val layoutParams = it.layoutParams as FrameLayout.LayoutParams
//                            layoutParams.height =
//                                ScreenUtil.getScreenMin(context) * format.height / format.width
//                            it.layoutParams = layoutParams
//                        }
                    }
                }
            }
        }
        player!!.player.removeAnalyticsListener(analyticsListener!!)
        player!!.player.addAnalyticsListener(analyticsListener!!)
    }

    private fun fastPositionJump(forward: Long) {
        if (player == null) {
            return
        }
        val newPos = player!!.currentPosition + forward * 1000
        position = if (player!!.duration < newPos) {
            player!!.duration - 1000
        } else if (newPos < 0) {
            if (forward > 0) {
                forward * 1000
            } else {
                0
            }
        } else {
            newPos
        }
        player!!.seekTo(position)
        try {
            addPlayerPosition(context, getMemoryId(), position)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun showFastJumpNotice(gap: Int) {
        val notice: String = playerView!!.notice
        var finalJump = gap
        if (StringUtil.isNotEmpty(notice)) {
            if (notice.contains("已快进") && gap < 0) {
                Timber.d("之前快进，现在快退, gap=%s", gap)
            } else if (notice.contains("已快退") && gap > 0) {
                Timber.d("之前快退，现在快进, gap=%s", gap)
            } else {
                val nowJump = notice.replace("已快退", "").replace("秒", "").replace("已快进", "")
                if (StringUtil.isNotEmpty(nowJump)) {
                    try {
                        var jump = nowJump.toInt()
                        if (jump != 0) {
                            if (notice.contains("已快退")) {
                                jump = -jump
                            }
                            finalJump = jump + gap
                        }
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        if (finalJump != 0) {
            playerView!!.showNotice((if (finalJump > 0) "已快进" else "已快退") + Math.abs(finalJump) + "秒")
        }
    }

    private fun playNow() {
        if (StringUtil.isEmpty(url) || player == null) {
            return
        }
        player!!.reset()
        try {
            player!!.setPlayUri(HttpParser.getRealUrlFilterHeaders(url), HttpParser.getHeaders(url))
            player!!.setPosition(position)
            player!!.startPlayer<ExoUserPlayer>()
            addFormatListener()
            initTrackListener()
        } catch (e: Exception) {
        }
    }

    private fun reStartPlayer(reGetPos: Boolean) {
        if (StringUtil.isEmpty(url)) {
            return
        }
        if (reGetPos) {
            initPlayPos = getPlayPos(context, getMemoryId())
            val extraData = HeavyTaskUtil.findJumpPos(context, null, null)
            jumpStartDuration = extraData.jumpStartDuration
            if (jumpStartDuration > 0 && initPlayPos < jumpStartDuration * 1000) {
                initPlayPos = (jumpStartDuration * 1000).toLong()
            }
            position = initPlayPos
        }
        playNow()
    }

    private fun getMemoryId(): String {
        return url.split(";")[0]
    }

    /**
     * 启动切换播放地址，超时找不到则销毁
     */
    private fun autoChangeXiuTanVideo(count: Int, ticket: Long) {
        if (vaildTicket != ticket) {
            //ticket已经失效，说明有别的地方发起了新的任务
            return
        }
        if (count <= 0) {
            if (player?.isPlaying == false) {
                destroy()
                ToastMgr.shortBottomCenter(getContext(), "找不到可播放的视频地址")
            }
            return
        }
        val ok = autoChangeXiuTanVideo()
        if (!ok) {
            playerView?.postDelayed({
                autoChangeXiuTanVideo(count - 1, ticket)
            }, 2000)
        }
    }

    /**
     * 切换播放地址
     */
    private fun autoChangeXiuTanVideo(): Boolean {
        if (player == null || playerView == null) {
            return false
        }
        val dom = StringUtil.getDom(this.webUrl) ?: ""
        videoDetector.putIntoXiuTanLiked(context, dom, "www.fy-sys.cn")
        val results = videoDetector.getDetectedMediaResults(webUrl, MediaType.VIDEO)
        if (CollectionUtil.isEmpty(results)) {
            return false
        }
        val nowVideoUrl = url.split(";")[0]
        if (results.size == 1 && StringUtils.equals(results[0]!!.url, nowVideoUrl)) {
            return false
        }
        var idx = -1
        for ((index, mediaResult) in results.withIndex()) {
            if (nowVideoUrl == mediaResult.url) {
                idx = index
                break
            }
        }
        //优先找后面的，像哔哩那种动态hash的，找前面的可能是之前播的地址
        for (i in idx + 1 until results.size) {
            if (playMedia(results[i], nowVideoUrl, dom)) {
                return true
            }
        }

        for (i in 0 until idx) {
            if (playMedia(results[i], nowVideoUrl, dom)) {
                return true
            }
        }
        return false
    }

    private fun playMedia(result: DetectedMediaResult, nowVideoUrl: String, dom: String): Boolean {
        if (!result.isClicked && !StringUtils.equals(result.url, nowVideoUrl)) {
            result.isClicked = true
            val uu = PlayerChooser.decorateHeader(
                result.headers,
                result.url
            )
            updateUrl(uu)
            reStartPlayer(true)
            return true
        }
        return false
    }

    private fun updateUrl(newUrl: String) {
        url = UrlDetector.clearTag(newUrl)
        video_address_view?.text = url.split(";")[0]
    }

    private fun getContext(): Context {
        return context
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.custom_mode, R.id.custom_toolbar_desc -> {
                if (layoutNow != VideoPlayerView.Layout.VERTICAL) {
                    reverseListCardVisibility()
                } else {
                    playerView?.exoFullscreen?.performClick()
                }
            }

            R.id.exo_pip -> {
                ToastMgr.shortCenter(context, "暂不支持此功能")
            }

            R.id.custom_dlan -> {
                startDlan()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDlanDeviceUpdated(event: OnDeviceUpdateEvent?) {
        if (dlanListPop != null) {
            dlanListPop!!.notifyDataChanged()
        }
    }

    /**
     * 传统DLAN投屏
     */
    private fun startDlan() {
        player!!.setStartOrPause(false)
        val playUrl = HttpParser.getThirdDownloadSource(url)
        if (CollectionUtil.isEmpty(DlanListPopUtil.instance().devices)) {
            DlanListPopUtil.instance().reInit()
        } else {
            if (DlanListPopUtil.instance().usedDevice != null &&
                DlanListPopUtil.instance().devices.contains(DlanListPopUtil.instance().usedDevice)
            ) {
                ClingManager.getInstance().selectedDevice = DlanListPopUtil.instance().usedDevice
                val intent1 = Intent(
                    getContext(),
                    MediaPlayActivity::class.java
                )
                intent1.putExtra(DLandataInter.Key.PLAY_TITLE, title)
                intent1.putExtra(DLandataInter.Key.PLAYURL, playUrl)
                intent1.putExtra(
                    DLandataInter.Key.HEADER,
                    DlanListPop.genHeaderString(HttpParser.getHeaders(url))
                )
                context.startActivity(intent1)
                ToastMgr.shortBottomCenter(getContext(), "已使用常用设备投屏，长按投屏按钮切换设备")
                return
            }
        }
        if (dlanListPop == null) {
            dlanListPop = DlanListPop(context, DlanListPopUtil.instance().devices)
        }
        dlanListPop?.updateTitleAndUrl(playUrl, title, HttpParser.getHeaders(url))
        XPopup.Builder(context)
            .asCustom(dlanListPop)
            .show()
    }

    /**
     * 网页投屏
     *
     * @param showToast 复制链接时是否显示toast
     */
    private fun startWebDlan(showToast: Boolean, forRedirect: Boolean) {
        ToastMgr.shortCenter(context, "暂不支持此功能")
    }

    private fun reverseListCardVisibility() {
        if (listCard!!.visibility == View.INVISIBLE || listCard!!.visibility == View.GONE) {
            refreshListScrollView(true, false, listScrollView!!)
            setListCardTextColor()
            listCard!!.visibility = View.VISIBLE
            playerView!!.getPlaybackControlView().setShowTimeoutMs(0)
            listCard!!.animate().alpha(1f).start()
            listCard!!.setOnClickListener { v: View? -> reverseListCardVisibility() }
        } else {
            refreshListScrollView(false, false, listScrollView!!)
            playerView!!.getPlaybackControlView().setShowTimeoutMs(5000)
        }
    }

    private fun refreshListScrollView(open: Boolean, halfWidth: Boolean, listScrollView: View) {
        var start = 0
        var end = 0
        var width = 0
        listCard!!.post {
            if (open) {
                listScrollView.visibility = View.VISIBLE
            }
            val layoutParams1 = listScrollView.layoutParams as RelativeLayout.LayoutParams
            if (playerView!!.isNowVerticalFullScreen) {
                width = playerView!!.measuredWidth
                if (halfWidth) {
                    start = if (open) width else width / 2
                    end = if (open) width / 2 else width
                } else {
                    start = if (open) width else 0
                    end = if (open) 0 else width
                }
            } else {
                width = playerView!!.measuredWidth
                if (halfWidth) {
                    start = if (open) width else width / 4 * 3
                    end = if (open) width / 4 * 3 else width
                } else {
                    start = if (open) width else width / 2
                    end = if (open) width / 2 else width
                }
            }
            val anim = ValueAnimator.ofInt(start, end)
            anim.duration = 300
            val finalEnd = end
            anim.addUpdateListener { animation: ValueAnimator ->
                layoutParams1.leftMargin = (animation.animatedValue as Int)
                listScrollView.layoutParams = layoutParams1
                if (!open && layoutParams1.leftMargin == finalEnd) {
                    playerView!!.getPlaybackControlView().hide()
                    listCard!!.visibility = View.INVISIBLE
                }
            }
            anim.start()
        }
    }

    /**
     * 倍速、显示比例
     */
    private fun setListCardTextColor() {
        val white: Int = context.getResources().getColor(R.color.white)
        val green: Int = context.getResources().getColor(R.color.greenAction)
        val mode_fit = listCard!!.findViewById<TextView>(R.id.mode_fit)
        mode_fit.setTextColor(white)
        val mode_fill = listCard!!.findViewById<TextView>(R.id.mode_fill)
        mode_fill.setTextColor(white)
        val mode_zoom = listCard!!.findViewById<TextView>(R.id.mode_zoom)
        mode_zoom.setTextColor(white)
        val mode_fixed_width = listCard!!.findViewById<TextView>(R.id.mode_fixed_width)
        mode_fixed_width.setTextColor(white)
        val mode_fixed_height = listCard!!.findViewById<TextView>(R.id.mode_fixed_height)
        mode_fixed_height.setTextColor(white)
        val speed_1 = listCard!!.findViewById<TextView>(R.id.speed_1)
        speed_1.setTextColor(white)
        val speed_1_2 = listCard!!.findViewById<TextView>(R.id.speed_1_2)
        speed_1_2.setTextColor(white)
        val speed_1_5 = listCard!!.findViewById<TextView>(R.id.speed_1_5)
        speed_1_5.setTextColor(white)
        val speed_1_75 = listCard!!.findViewById<TextView>(R.id.speed_1_75)
        speed_1_75.setTextColor(white)
        val speed_2 = listCard!!.findViewById<TextView>(R.id.speed_2)
        speed_2.setTextColor(white)
        val speed_25 = listCard!!.findViewById<TextView>(R.id.speed_25)
        speed_25.setTextColor(white)
        val speed_p8 = listCard!!.findViewById<TextView>(R.id.speed_p8)
        speed_p8.setTextColor(white)
        val speed_p5 = listCard!!.findViewById<TextView>(R.id.speed_p5)
        speed_p5.setTextColor(white)
        val speed_3 = listCard!!.findViewById<TextView>(R.id.speed_3)
        speed_3.setTextColor(white)
        val speed_4 = listCard!!.findViewById<TextView>(R.id.speed_4)
        speed_4.setTextColor(white)
        val speed_5 = listCard!!.findViewById<TextView>(R.id.speed_5)
        speed_5.setTextColor(white)
        val speed_6 = listCard!!.findViewById<TextView>(R.id.speed_6)
        speed_6.setTextColor(white)
        when (playerView?.playerView?.resizeMode ?: AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> mode_fit.setTextColor(green)
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> mode_fill.setTextColor(green)
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> mode_zoom.setTextColor(green)
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> mode_fixed_width.setTextColor(green)
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> mode_fixed_height.setTextColor(green)
        }
        when (VideoPlayerManager.PLAY_SPEED * 10) {
            10F -> speed_1.setTextColor(green)
            12F -> speed_1_2.setTextColor(green)
            15F -> speed_1_5.setTextColor(green)
            17.5F -> speed_1_75.setTextColor(green)
            20F -> speed_2.setTextColor(green)
            25F -> speed_25.setTextColor(green)
            8F -> speed_p8.setTextColor(green)
            5F -> speed_p5.setTextColor(green)
            30F -> speed_3.setTextColor(green)
            40F -> speed_4.setTextColor(green)
            50F -> speed_5.setTextColor(green)
            60F -> speed_6.setTextColor(green)
        }
    }

    /**
     * 倍速、显示比例
     */
    private fun initActionAdapter() {
        playerView?.findViewById<View>(R.id.custom_dlan)?.setOnClickListener(this)
        descView?.setOnClickListener(this)
        playerView?.findViewById<View>(R.id.custom_mode)?.setOnClickListener(this)
        listCard = playerView!!.findViewById<View>(R.id.custom_list_bg)
        val listener = View.OnClickListener { v: View? ->
            dealActionViewClick(v!!)
            reverseListCardVisibility()
        }
        listCard?.let {
            it.setOnClickListener(listener)
            it.findViewById<View>(R.id.mode_fit).setOnClickListener(listener)
            it.findViewById<View>(R.id.mode_fill).setOnClickListener(listener)
            it.findViewById<View>(R.id.mode_zoom).setOnClickListener(listener)
            it.findViewById<View>(R.id.mode_fixed_width).setOnClickListener(listener)
            it.findViewById<View>(R.id.mode_fixed_height).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_10).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_30).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_60).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_120).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_10_l).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_30_l).setOnClickListener(listener)
            it.findViewById<View>(R.id.jump_60_l).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_1).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_1_2).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_1_5).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_1_75).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_2).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_25).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_p8).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_p5).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_3).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_4).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_5).setOnClickListener(listener)
            it.findViewById<View>(R.id.speed_6).setOnClickListener(listener)
            video_address_view = it.findViewById(R.id.video_address_view)
            video_address_view?.setOnClickListener {
                ClipboardUtil.copyToClipboardForce(getContext(), url.split(";")[0])
            }
            listScrollView = holderView!!.findViewById(R.id.custom_list_scroll_view)
            val dp44 = DisplayUtil.dpToPx(getContext(), 44)
            val dp10 = DisplayUtil.dpToPx(getContext(), 10)
            listScrollView!!.setPadding(dp10, dp44, dp10, dp44)
        }
    }


    /**
     * 倍速、显示比例
     */
    private fun dealActionViewClick(v: View) {
        when (v.id) {
            R.id.mode_fit -> {
                playerView!!.getPlayerView()
                    .setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
                descView!!.text = "速度×" + VideoPlayerManager.PLAY_SPEED + "/自适应"
                PreferenceMgr.put(getContext(), "ijkplayer", "resizeMode", 0)
            }

            R.id.mode_fill -> {
                playerView!!.getPlayerView()
                    .setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL)
                descView!!.text = "速度×" + VideoPlayerManager.PLAY_SPEED + "/充满"
                PreferenceMgr.put(getContext(), "ijkplayer", "resizeMode", 3)
            }

            R.id.mode_zoom -> {
                playerView!!.getPlayerView()
                    .setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
                descView!!.text = "速度×" + VideoPlayerManager.PLAY_SPEED + "/裁剪"
                PreferenceMgr.put(getContext(), "ijkplayer", "resizeMode", 4)
            }

            R.id.mode_fixed_width -> {
                playerView!!.getPlayerView()
                    .setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)
                descView!!.text = "速度×" + VideoPlayerManager.PLAY_SPEED + "/宽度"
                PreferenceMgr.put(getContext(), "ijkplayer", "resizeMode", 1)
            }

            R.id.mode_fixed_height -> {
                playerView!!.getPlayerView()
                    .setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT)
                descView!!.text = "速度×" + VideoPlayerManager.PLAY_SPEED + "/高度"
                PreferenceMgr.put(getContext(), "ijkplayer", "resizeMode", 2)
            }

            R.id.jump_10, R.id.jump_30, R.id.jump_60, R.id.jump_120, R.id.jump_10_l, R.id.jump_30_l, R.id.jump_60_l -> {
                val forward: Long = (v.tag as String).toLong()
                fastPositionJump(forward)
            }

            R.id.speed_1, R.id.speed_1_2, R.id.speed_1_5, R.id.speed_1_75, R.id.speed_2, R.id.speed_25, R.id.speed_p8, R.id.speed_p5, R.id.speed_3, R.id.speed_4, R.id.speed_5, R.id.speed_6 -> {
                val speed: Float = (v.tag as String).toFloat()
                playFromSpeed(speed)
            }
        }
    }

    /**
     * 倍速，全局保存，重启软件失效
     */
    private fun playFromSpeed(speed: Float) {
        VideoPlayerManager.PLAY_SPEED = speed
        player!!.setPlaybackParameters(speed, 1f)
        descView!!.text =
            ("速度×" + VideoPlayerManager.PLAY_SPEED + "/" + descView!!.text.toString().split("/")
                .toTypedArray()[1])
        val memoryPlaySpeed = PreferenceMgr.getBoolean(
            getContext(),
            PreferenceMgr.SETTING_CONFIG,
            "memoryPlaySpeed",
            true
        )
        if (memoryPlaySpeed) {
            PreferenceMgr.put(getContext(), "ijkplayer", "playSpeed", VideoPlayerManager.PLAY_SPEED)
        }
    }

    private fun getHeaders(
        map: MutableMap<String, MutableMap<String, String?>?>?,
        videoUrl: String
    ): MutableMap<String, String?>? {
        if (map == null) {
            return null
        }
        return map[videoUrl]
    }

    private fun pauseWebDelay(count: Int = 5) {
        if (count < 0) {
            return
        }
        if (playerView != null && player != null && url.isNotEmpty()) {
            pauseWebView(true, false)
            playerView?.postDelayed({
                pauseWebDelay(count - 1)
            }, 1000)
        }
    }

    interface WebHolder {
        fun getRequestMap(): MutableMap<String, MutableMap<String, String?>?>?
    }


    private fun initTrackListener() {
        if (player == null || player?.player == null) {
            return
        }
        autoSelectAudioTextFinished = false
        showAudioSubtitleTracks(ArrayList(), ArrayList(), ArrayList(), null)
        if (trackListener == null) {
            trackListener = object : Player.Listener {
                override fun onTracksChanged(
                    trackGroups: TrackGroupArray,
                    trackSelections: TrackSelectionArray
                ) {
                    val trackSelector = player!!.player.trackSelector
                    if (trackSelector is MappingTrackSelector) {
                        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
                        if (mappedTrackInfo != null) {
                            val audioFormats: MutableList<Format?> = ArrayList()
                            val subtitleFormats: MutableList<Format?> = ArrayList()
                            val videoFormats: MutableList<Format?> = ArrayList()
                            for (i in 0 until mappedTrackInfo.rendererCount) {
                                val rendererTrackGroups = mappedTrackInfo.getTrackGroups(i)
                                if (C.TRACK_TYPE_AUDIO == mappedTrackInfo.getRendererType(i)) { //判断是否是音轨
                                    for (groupIndex in 0 until rendererTrackGroups.length) {
                                        val trackGroup = rendererTrackGroups[groupIndex]
                                        for (j in 0 until trackGroup.length) {
                                            audioFormats.add(trackGroup.getFormat(j))
                                        }
                                    }
                                } else if (C.TRACK_TYPE_TEXT == mappedTrackInfo.getRendererType(i)) { //判断是否是字幕
                                    for (groupIndex in 0 until rendererTrackGroups.length) {
                                        val trackGroup = rendererTrackGroups[groupIndex]
                                        for (j in 0 until trackGroup.length) {
                                            subtitleFormats.add(trackGroup.getFormat(j))
                                        }
                                    }
                                } else if (C.TRACK_TYPE_VIDEO == mappedTrackInfo.getRendererType(i)) { //判断是否是视频
                                    for (groupIndex in 0 until rendererTrackGroups.length) {
                                        val trackGroup = rendererTrackGroups[groupIndex]
                                        for (j in 0 until trackGroup.length) {
                                            videoFormats.add(trackGroup.getFormat(j))
                                        }
                                    }
                                }
                            }
                            if (!autoSelectAudioTextFinished) {
                                autoSelectAudioTextFinished = true
                                autoSelectAudioSubtitle(
                                    audioFormats,
                                    subtitleFormats,
                                    trackSelections
                                )
                            }
                            showAudioSubtitleTracks(
                                videoFormats,
                                audioFormats,
                                subtitleFormats,
                                trackSelections
                            )
                        }
                    }
                }
            }
        }
        player?.player?.removeListener(trackListener!!)
        player?.player?.addListener(trackListener!!)
    }

    /**
     * 显示音频和字幕轨道
     *
     * @param audioFormats
     * @param subtitleFormats
     */
    private fun showAudioSubtitleTracks(
        videoFormats: List<Format?>,
        audioFormats: List<Format?>,
        subtitleFormats: List<Format?>,
        trackSelections: TrackSelectionArray?
    ) {
        var audio: String? = null
        var text: String? = null
        var video: String? = null
        if (trackSelections != null) {
            for (i in 0 until trackSelections.length) {
                if (trackSelections[i] != null) {
                    if (trackSelections[i] is BaseTrackSelection) {
                        val format = (trackSelections[i] as BaseTrackSelection).selectedFormat
                        if (MimeTypes.isText(
                                format.sampleMimeType
                            )
                        ) {
                            text = format.id
                        } else if (MimeTypes.isAudio(
                                format.sampleMimeType
                            )
                            || format.channelCount != Format.NO_VALUE
                        ) {
                            audio = format.id
                        } else if (MimeTypes.isVideo(
                                format.sampleMimeType
                            )
                            || format.channelCount != Format.NO_VALUE
                        ) {
                            video = format.id
                        }
                        continue
                    }
                    for (j in 0 until trackSelections[i]!!.length()) {
                        if (MimeTypes.isText(
                                trackSelections[i]!!.getFormat(j).sampleMimeType
                            )
                        ) {
                            text = trackSelections[i]!!.getFormat(j).id
                        } else if (MimeTypes.isAudio(
                                trackSelections[i]!!.getFormat(j).sampleMimeType
                            )
                            || trackSelections[i]!!.getFormat(j).channelCount != Format.NO_VALUE
                        ) {
                            audio = trackSelections[i]!!.getFormat(j).id
                        } else if (MimeTypes.isVideo(
                                trackSelections[i]!!.getFormat(j).sampleMimeType
                            )
                            || trackSelections[i]!!.getFormat(j).channelCount != Format.NO_VALUE
                        ) {
                            video = trackSelections[i]!!.getFormat(j).id
                        }
                    }
                }
            }
        }
        if (video_tracks!!.childCount != 1) {
            video_tracks!!.removeViews(1, video_tracks!!.childCount - 1)
        }
        if (CollectionUtil.isNotEmpty(videoFormats)) {
            for (videoFormat in videoFormats) {
                val view = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_track_view, video_tracks, false) as TextView
                val label = buildVideoString(videoFormat!!)
                view.text = label
                if (video != null && video == videoFormat.id) {
                    view.setTextColor(context.resources.getColor(R.color.greenAction))
                }
                view.tag = videoFormat
                view.setOnClickListener { v: View ->
                    changeTrack(
                        (v.tag as Format)
                    )
                }
                video_tracks!!.addView(view)
            }
        }
        if (audio_tracks!!.childCount != 1) {
            audio_tracks!!.removeViews(1, audio_tracks!!.childCount - 1)
        }
        if (CollectionUtil.isNotEmpty(audioFormats)) {
            for (audioFormat in audioFormats) {
                val view = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_track_view, audio_tracks, false) as TextView
                var label = if (StringUtil.isNotEmpty(
                        audioFormat!!.label
                    )
                ) audioFormat.label else audioFormat.language
                if ("zh" == label || "Chinese" == label) {
                    label = "国语"
                } else if ("Cantonese" == label) {
                    label = "粤语"
                } else if ("en" == label) {
                    label = "英语"
                }
                val channel = buildAudioChannelString(audioFormat)
                if (StringUtil.isNotEmpty(channel)) {
                    label = "$label, $channel";
                }
                view.text = label
                if (audio != null && audio == audioFormat.id) {
                    view.setTextColor(context.resources.getColor(R.color.greenAction))
                }
                view.tag = audioFormat
                view.setOnClickListener { v: View ->
                    changeTrack(
                        (v.tag as Format)
                    )
                }
                audio_tracks!!.addView(view)
            }
        }
        if (subtitle_tracks!!.childCount != 1) {
            subtitle_tracks!!.removeViews(1, subtitle_tracks!!.childCount - 1)
        }
        if (CollectionUtil.isNotEmpty(subtitleFormats)) {
            for (subtitleFormat in subtitleFormats) {
                val view = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_track_view, subtitle_tracks, false) as TextView
                var label = if (StringUtil.isNotEmpty(
                        subtitleFormat!!.label
                    )
                ) subtitleFormat.label else subtitleFormat.language
                if ("Chinese Simplified" == label) {
                    label = "中文(简体)"
                } else if ("Chinese Traditional" == label) {
                    label = "中文(繁体)"
                } else if ("en" == label) {
                    label = "英语"
                } else if ("Chinese" == label) {
                    label = "中文"
                }
                view.text = label
                if (text != null && text == subtitleFormat.id) {
                    view.setTextColor(context.resources.getColor(R.color.greenAction))
                }
                view.tag = subtitleFormat
                view.setOnClickListener { v: View ->
                    changeTrack(
                        (v.tag as Format)
                    )
                }
                subtitle_tracks!!.addView(view)
            }
        }
    }

    /**
     * 更新字体颜色，因为AdaptiveTrackSelection可能会根据网络随时变化
     *
     * @param format
     */
    private fun updateVideoFormatView(format: Format) {
        if (video_tracks!!.childCount != 1) {
            for (i in 1 until video_tracks!!.childCount) {
                if (video_tracks!!.getChildAt(i).tag != null && video_tracks!!.getChildAt(i)
                        .tag is Format
                ) {
                    val tag = video_tracks!!.getChildAt(i).getTag() as Format
                    val view = video_tracks!!.getChildAt(i) as TextView
                    if (equals(format, tag)) {
                        view.text = buildVideoString(format)
                        view.setTextColor(video_tracks!!.resources.getColor(R.color.greenAction))
                    } else {
                        view.setTextColor(video_tracks!!.resources.getColor(R.color.white))
                    }
                }
            }
        }
    }

    private fun equals(format1: Format, format2: Format): Boolean {
        return if (StringUtil.isNotEmpty(format1.id) && StringUtil.isNotEmpty(format2.id)) {
            format1.id == format2.id
        } else format1.toString() == format2.toString()
    }

    private fun buildAudioChannelString(format: Format): String? {
        val channelCount = format.channelCount
        return if (channelCount < 1) {
            null
        } else when (channelCount) {
            1 -> "单声道"
            2 -> "立体声"
            6, 7 -> "5.1 环绕声"
            8 -> "7.1 环绕声"
            else -> "环绕声"
        }
    }

    private fun buildVideoString(format: Format): String? {
        return (format.sampleMimeType
                + (", " + format.width + " x " + format.height)
                + (if (StringUtil.isNotEmpty(format.codecs)) ", " + format.codecs else "")
                + if (format.frameRate > 0) ", " + format.frameRate + "fps" else "")
    }

    private fun changeTrack(format: Format) {
        val trackSelector = player!!.player.trackSelector
        if (trackSelector is DefaultTrackSelector) {
            val mappedTrackInfo = trackSelector.currentMappedTrackInfo
            if (mappedTrackInfo != null) {
                for (index in 0 until mappedTrackInfo.rendererCount) {
                    val trackGroupArray = mappedTrackInfo.getTrackGroups(index)
                    for (i in 0 until trackGroupArray.length) {
                        val trackGroup = trackGroupArray[i]
                        for (j in 0 until trackGroup.length) {
                            if (StringUtils.equals(trackGroup.getFormat(j).id, format.id)) {
                                trackSelector.setParameters(
                                    trackSelector.parameters.buildUpon()
                                        .setSelectionOverride(
                                            index, trackGroupArray,
                                            SelectionOverride(i, j)
                                        )
                                )
                                return
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 自动选择字幕和音频轨道
     *
     * @param audioFormats
     * @param subtitleFormats
     * @param trackSelections
     */
    private fun autoSelectAudioSubtitle(
        audioFormats: List<Format?>, subtitleFormats: List<Format?>,
        trackSelections: TrackSelectionArray
    ) {
        if (audioFormats.size < 2 && subtitleFormats.size < 2) {
            return
        }
        var audio: String? = null
        var text: String? = null
        for (i in 0 until trackSelections.length) {
            if (trackSelections[i] != null) {
                for (j in 0 until trackSelections[i]!!.length()) {
                    if (MimeTypes.isText(trackSelections[i]!!.getFormat(j).sampleMimeType)) {
                        text = trackSelections[i]!!.getFormat(j).language
                    } else if (MimeTypes.isAudio(
                            trackSelections[i]!!.getFormat(j).sampleMimeType
                        )
                        || trackSelections[i]!!.getFormat(j).channelCount != Format.NO_VALUE
                    ) {
                        audio = trackSelections[i]!!.getFormat(j).language
                    }
                }
            }
        }
        if (audio != null && text != null) {
            return
        }
        if (audio == null) {
            for (i in audioFormats.indices) {
                if (i == 0) {
                    audio = audioFormats[i]!!.language
                }
                if ("zh" == audioFormats[i]!!.language) {
                    audio = "zh"
                    break
                }
            }
        }
        if (text == null) {
            for (i in subtitleFormats.indices) {
                if (i == 0) {
                    text = subtitleFormats[i]!!.language
                }
                if ("zh" == subtitleFormats[i]!!.language) {
                    text = "zh"
                    break
                }
            }
        }
        val trackSelector = player!!.player.trackSelector
        if (trackSelector is DefaultTrackSelector) {
            val defaultTrackSelector = trackSelector
            defaultTrackSelector.setParameters(
                defaultTrackSelector.parameters.buildUpon()
                    .setPreferredTextLanguage(text)
                    .setPreferredAudioLanguage(audio)
            )
        }
    }

    fun onMusicAction(action: MusicAction) {
        if (player == null) {
            return
        }
        if (MusicForegroundService.PAUSE == action.getCode()) {

        } else if (MusicForegroundService.NEXT == action.getCode()) {

        } else if (MusicForegroundService.PAUSE_NOW == action.getCode() || MusicForegroundService.PAUSE_NOW_DISCONNECTED == action.getCode()) {
            if (bluetoothBroadcastReceiver != null && MusicForegroundService.PAUSE_NOW_DISCONNECTED == action.getCode()) {
                //断开连接时记忆当前播放状态，后续重连上好恢复
                lastPlaying = player!!.isPlaying
            }
            if (player!!.isPlaying) {
                player!!.setStartOrPause(false)
            }
        } else if (MusicForegroundService.PLAY_NOW_CONNECTED == action.getCode()) {
            //恢复播放
            if (!player!!.isPlaying && bluetoothBroadcastReceiver != null && lastPlaying) {
                player!!.setStartOrPause(true)
                //消费一次，重置
                lastPlaying = false
            }
        } else if (MusicForegroundService.PREV == action.getCode()) {

        } else if (MusicForegroundService.CLOSE == action.getCode()) {

        }
    }
}