package org.mozilla.xiu.browser.dlan;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lxj.xpopup.core.CenterPopupView;
import com.qingfeng.clinglibrary.entity.ClingDevice;
import com.qingfeng.clinglibrary.service.manager.ClingManager;

import org.mozilla.xiu.browser.R;
import org.mozilla.xiu.browser.utils.CollectionUtil;
import org.mozilla.xiu.browser.utils.HttpParser;
import org.mozilla.xiu.browser.utils.ToastMgr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author huangyong
 * createTime 2019-10-05
 */
public class DlanListPop extends CenterPopupView {
    private static final String TAG = "DlanListPop";
    private RecyclerView list;
    private List<ClingDevice> devices;
    private String url;
    private String title;
    private Map<String, String> headers;
    private DeviceListAdapter adapter;

    public DlanListPop(@NonNull Activity context, List<ClingDevice> devices) {
        super(context);
        this.devices = devices;
    }

    public void updateTitleAndUrl(String url, String title) {
        this.url = url;
        this.title = title;
    }

    public void updateTitleAndUrl(String url, String title, Map<String, String> headers) {
        this.url = url;
        this.title = title;
        this.headers = headers;
    }

    private String genHeaderString() {
        return genHeaderString(headers);
    }

    public static String genHeaderString(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        } else {
            List<String> hd = new ArrayList<>();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                hd.add(entry.getKey() + "=" + HttpParser.encodeUrl(entry.getValue()));
            }
            return CollectionUtil.listToString(hd, "&");
        }
    }

    public void notifyDataChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        list = findViewById(R.id.device_list);
        View cancle = findViewById(R.id.dlan_to_cancel);
        View help = findViewById(R.id.dlan_to_help);
        help.setOnClickListener(v -> {
            DlanListPopUtil.instance().reInit();
            ToastMgr.shortBottomCenter(getContext(), "已强制刷新，设备列表将稍后自动刷新");
        });
        adapter = new DeviceListAdapter(getContext(), devices, (device, isActived) -> {
            if (device != null && isActived) {
                try {
                    ClingManager.getInstance().setSelectedDevice(device);
                    DlanListPopUtil.instance().setUsedDevice(device);
                    Intent intent = new Intent(getContext(), MediaPlayActivity.class);
                    intent.putExtra(DLandataInter.Key.PLAY_TITLE, title);
                    intent.putExtra(DLandataInter.Key.PLAYURL, url);
                    intent.putExtra(DLandataInter.Key.HEADER, genHeaderString());
                    getContext().startActivity(intent);
                    dismiss();
                } catch (Exception e) {
                    DlanListPopUtil.instance().reInit();
                    list.postDelayed(() -> {
                        try {
                            ClingManager.getInstance().setSelectedDevice(device);
                            DlanListPopUtil.instance().setUsedDevice(device);
                            Intent intent = new Intent(getContext(), MediaPlayActivity.class);
                            intent.putExtra(DLandataInter.Key.PLAY_TITLE, title);
                            intent.putExtra(DLandataInter.Key.PLAYURL, url);
                            intent.putExtra(DLandataInter.Key.HEADER, genHeaderString());
                            getContext().startActivity(intent);
                            dismiss();
                        } catch (Exception e1) {
                            DlanListPopUtil.instance().reInit();
                            list.postDelayed(() -> {
                                try {
                                    ClingManager.getInstance().setSelectedDevice(device);
                                    DlanListPopUtil.instance().setUsedDevice(device);
                                    Intent intent = new Intent(getContext(), MediaPlayActivity.class);
                                    intent.putExtra(DLandataInter.Key.PLAY_TITLE, title);
                                    intent.putExtra(DLandataInter.Key.PLAYURL, url);
                                    intent.putExtra(DLandataInter.Key.HEADER, genHeaderString());
                                    getContext().startActivity(intent);
                                    dismiss();
                                } catch (Exception e2) {
                                    ToastMgr.shortBottomCenter(getContext(), "投屏初始化未完成，请稍候重试");
                                }
                            }, 1500);
                        }
                    }, 500);
                }
            } else {
                Toast.makeText(getContext(), "未连接到设备", Toast.LENGTH_SHORT).show();
            }
        });
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);
        cancle.setOnClickListener(v -> {
            dismiss();
        });
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.item_dlan_ui_device_pop_layout;
    }

}
