package chuangyuan.ycj.videolibrary.video;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ClippingMediaSource;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;

import java.util.List;
import java.util.Map;

import chuangyuan.ycj.videolibrary.R;
import chuangyuan.ycj.videolibrary.factory.HttpDefaultDataSourceFactory;
import chuangyuan.ycj.videolibrary.listener.DataSourceListener;
import chuangyuan.ycj.videolibrary.listener.ItemVideo;
import chuangyuan.ycj.videolibrary.utils.VideoPlayUtils;

/**
 * author yangc
 * date 2017/2/28
 * E-Mail:1007181167@qq.com
 * Description：数据源处理类
 */
public class MediaSourceBuilder {
    private static final String TAG = MediaSourceBuilder.class.getName();
    /*** The Context.*/
    protected Context context;
    private MediaSource mediaSource;
    /*** The Listener. */
    protected DataSourceListener listener;
    /**
     * The Source event listener.
     */
    protected MediaSourceEventListener sourceEventListener = null;
    private int indexType = -1;

    protected List<String> videoUri;
    private int loopingCount = 0;

    protected Map<String, String> headers;
    protected String subtitle;

    public List<String> getAudioUrls() {
        return audioUrls;
    }

    public void setAudioUrls(List<String> audioUrls) {
        this.audioUrls = audioUrls;
    }

    protected List<String> audioUrls;

    protected UriProxy uriProxy;

    protected Uri playingUri;

    protected ErrorListener errorListener;

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void setUriProxy(UriProxy uriProxy) {
        this.uriProxy = uriProxy;
    }

    /***
     * 初始化
     *
     * @param context 上下文
     */
    public MediaSourceBuilder(@NonNull Context context) {
        this(context, null);
    }

    /***
     * 初始化
     *
     * @param context 上下文
     * @param listener 自定义数源工厂接口
     */
    public MediaSourceBuilder(@NonNull Context context, @Nullable DataSourceListener listener) {
        this.listener = listener;
        this.context = context.getApplicationContext();
    }

    /****
     * 初始化
     *
     * @param uri 视频的地址
     */
    void setMediaUri(@NonNull Uri uri) {
        mediaSource = initMediaSource(uri);
    }

    /****
     * 初始化
     *
     * @param uri 视频的地址
     * @param startPositionUs startPositionUs  毫秒
     *@param   endPositionUs endPositionUs       毫秒
     */
    public void setClippingMediaUri(@NonNull Uri uri, long startPositionUs, long endPositionUs) {
        MediaSource mediaSources = initMediaSource(uri);
        mediaSource = new ClippingMediaSource(mediaSources, startPositionUs, endPositionUs);
    }

    /****
     * 初始化
     *
     * @param uris 视频的地址列表
     */
    public void setMediaUri(@NonNull Uri... uris) {
        MediaSource[] firstSources = new MediaSource[uris.length];
        int i = 0;
        for (Uri item : uris) {
            firstSources[i] = initMediaSource(item);
            i++;
        }
        mediaSource = new ConcatenatingMediaSource(firstSources);
    }


    /****
     * 初始化多个视频源，无缝衔接
     *
     * @param firstVideoUri 第一个视频， 例如例如广告视频
     * @param secondVideoUri 第二个视频
     */
    public void setMediaUri(@NonNull Uri firstVideoUri, @NonNull Uri secondVideoUri) {
        setMediaUri(0, firstVideoUri, secondVideoUri);
    }

    /****
     * 初始化多个视频源，无缝衔接
     *
     * @param indexType the index type
     * @param switchIndex the switch index
     * @param firstVideoUri 第一个视频， 例如例如广告视频
     * @param secondVideoUri 第二个视频
     */
    public void setMediaUri(@Size(min = 0) int indexType, int switchIndex, @NonNull Uri firstVideoUri, @NonNull List<String> secondVideoUri) {
        this.videoUri = secondVideoUri;
        this.indexType = indexType;
        setMediaUri(indexType, firstVideoUri, Uri.parse(secondVideoUri.get(switchIndex)));
    }

    /****
     * @param indexType 设置当前索引视频屏蔽进度
     * @param firstVideoUri 预览的视频
     * @param secondVideoUri 第二个视频
     */
    public void setMediaUri(@Size(min = 0) int indexType, @NonNull Uri firstVideoUri, @NonNull Uri secondVideoUri) {
        this.indexType = indexType;
        ConcatenatingMediaSource source = new ConcatenatingMediaSource();
        source.addMediaSource(initMediaSource(firstVideoUri));
        source.addMediaSource(initMediaSource(secondVideoUri));
        mediaSource = source;
    }


    /**
     * 设置多线路播放
     *
     * @param videoUri 视频地址
     * @param index    选中播放线路
     */
    public void setMediaSwitchUri(@NonNull List<String> videoUri, int index) {
        this.videoUri = videoUri;
        setMediaUri(Uri.parse(videoUri.get(index) == null ? "hiker://empty" : videoUri.get(index)));
    }

    /****
     * 初始化
     *
     * @param <T>     你的实体类
     * @param uris 视频的地址列表\
     */
    public <T extends ItemVideo> void setMediaUri(@NonNull List<T> uris) {
        MediaSource[] firstSources = new MediaSource[uris.size()];
        int i = 0;
        for (T item : uris) {
            if (item.getVideoUri() != null) {
                firstSources[i] = initMediaSource(Uri.parse(item.getVideoUri()));
            }
            i++;
        }
        mediaSource = new ConcatenatingMediaSource(firstSources);
    }


    /***
     * 设置循环播放视频   Integer.MAX_VALUE 无线循环
     *
     * @param loopingCount 必须大于0
     */
    public void setLooping(@Size(min = 1) int loopingCount) {
        this.loopingCount = loopingCount;
    }

    /***
     * 设置自定义视频数据源
     * @param mediaSource 你的数据源
     */
    public void setMediaSource(MediaSource mediaSource) {
        this.mediaSource = mediaSource;
    }

    /***
     * 获取视频数据源
     * @return the media source
     */
    MediaSource getMediaSource() {
        if (loopingCount > 0) {
            return new LoopingMediaSource(mediaSource, loopingCount);
        }
        return mediaSource;
    }


    /***
     * 初始化数据源工厂
     * @return DataSource.Factory data source
     */
    public DataSource.Factory getDataSource() {
        if (listener != null) {
            return listener.getDataSourceFactory();
        } else {
            return new HttpDefaultDataSourceFactory(context, headers, playingUri);
        }
    }

    /***
     * 移除多媒体
     * @param index 要移除数据
     */
    void removeMediaSource(int index) {
        if (mediaSource instanceof ConcatenatingMediaSource) {
            ConcatenatingMediaSource source = (ConcatenatingMediaSource) mediaSource;
            source.getMediaSource(index).releaseSource(null);
            source.removeMediaSource(index);
        }
    }


    /****
     * 销毁资源
     */
    public void destroy() {
        indexType = -1;
        videoUri = null;
        listener = null;
    }

    /**
     * 获取视频所在索引
     *
     * @return int index type
     */
    public int getIndexType() {
        return indexType;
    }

    /**
     * 设置视频所在索引
     *
     * @param indexType 值
     */
    public void setIndexType(@Size(min = 0) int indexType) {
        this.indexType = indexType;
    }

    /**
     * 获取视频线路地址
     *
     * @return List<String> video uri
     */
    List<String> getVideoUri() {
        return videoUri;
    }

    public void setVideoUri(List<String> videoUri) {
        this.videoUri = videoUri;
    }


    /**
     * 用于通知自适应的回调接口获取视频线路名称
     *
     * @param sourceEventListener 实例
     */
    public void setAdaptiveMediaSourceEventListener(MediaSourceEventListener sourceEventListener) {
        this.sourceEventListener = sourceEventListener;
    }

    /****
     * 初始化视频源，无缝衔接
     *
     * @param uri 视频的地址
     * @return MediaSource media source
     */
    public MediaSource initMediaSource(Uri uri) {
        int streamType = VideoPlayUtils.inferContentType(uri);
        if (uriProxy != null) {
            uri = uriProxy.proxy(uri, streamType);
        }
        playingUri = uri;
        switch (streamType) {
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(getDataSource())
                        .setExtractorsFactory(new DefaultExtractorsFactory())
                        .setCustomCacheKey(uri.toString())
                        .createMediaSource(uri);
            default:
                throw new IllegalStateException(context.getString(com.google.android.exoplayer2.ui.R.string.media_error));
        }
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public interface UriProxy {
        Uri proxy(Uri uri, int streamType);
    }
}
