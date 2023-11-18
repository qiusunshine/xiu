package chuangyuan.ycj.videolibrary.listener;

import com.google.android.exoplayer2.upstream.DataSource;

/**
 * The interface Data source listener.
 *
 * @author yangc          date 2017/8/26         E-Mail:yangchaojiang@outlook.com         Deprecated: 数据源工厂接口
 */
public interface DataSourceListener {
    /***
     * 自定义数据源工厂
     * @return DataSource.Factory data source factory
     */
    DataSource.Factory getDataSourceFactory();


}
