package org.mozilla.xiu.browser.download;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lxj.xpopup.XPopup;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.mozilla.xiu.browser.R;
import org.mozilla.xiu.browser.base.BaseSlideActivity;
import org.mozilla.xiu.browser.utils.ClipboardUtil;
import org.mozilla.xiu.browser.utils.DisplayUtil;
import org.mozilla.xiu.browser.utils.FileUtil;
import org.mozilla.xiu.browser.utils.MyStatusBarUtil;
import org.mozilla.xiu.browser.utils.ShareUtil;
import org.mozilla.xiu.browser.utils.StringUtil;
import org.mozilla.xiu.browser.utils.TimeUtil;
import org.mozilla.xiu.browser.utils.ToastMgr;
import org.mozilla.xiu.browser.utils.UriUtilsPro;
import org.mozilla.xiu.browser.webextension.WebExtensionsAddEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2021/2/8
 * 时间：At 11:44
 */

public class FileBrowserActivity extends BaseSlideActivity {
    private String rootPath;
    private TextView curPathTextView;
    private RecyclerView recyclerView;
    private FileListAdapter adapter;
    private String parentPath;
    private static final String[] txt = new String[]{"json", "txt", "html", "js", "css", "log"};
    private static final String[] video = new String[]{"mp4", "mp3", "ts", "m3u8", "m4a"};

    public FileBrowserActivity() {
    }

    @Override
    protected int initLayout(Bundle savedInstanceState) {
        return R.layout.activity_file_browser;
    }

    @Override
    protected View getBackgroundView() {
        return findView(R.id.ad_list_window);
    }

    @Override
    protected void initView2() {
        int marginTop = MyStatusBarUtil.getStatusBarHeight(getContext()) + DisplayUtil.dpToPx(getContext(), 86);
        View bg = findView(R.id.ad_list_bg);
        findView(R.id.ad_list_window).setOnClickListener(view -> finish());
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) bg.getLayoutParams();
        layoutParams.topMargin = marginTop;
        bg.setLayoutParams(layoutParams);
        curPathTextView = findViewById(R.id.curPath);
        findView(R.id.back_icon).setOnClickListener(v -> finish());
        recyclerView = findView(R.id.recycler_view);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        rootPath = UriUtilsPro.getRootDir(getContext());
        parentPath = rootPath;
        adapter = new FileListAdapter(getContext(), new ArrayList<>());
        adapter.setOnItemClickListener(new FileListAdapter.OnItemClickListener() {

            @Override
            public void onClick(View view, int position) {
                File file = adapter.getList().get(position);
                if ("b1".equals(file.getName())) {
                    getFileDir(rootPath);
                } else if ("b2".equals(file.getName())) {
                    getFileDir(parentPath);
                } else if (file.isDirectory()) {
                    getFileDir(file.getPath());
                } else {
                    String extension = FileUtil.getExtension(file.getName());
                    //网页
                    if ("xpi".equalsIgnoreCase(extension) || "crx".equalsIgnoreCase(extension)) {
                        new XPopup.Builder(getContext())
                                .asCenterList(null, new String[]{"安装扩展程序", "外部软件打开"}, null, 100, (po, te) -> {
                                    if (po == 0) {
                                        EventBus.getDefault().post(new WebExtensionsAddEvent(file.getAbsolutePath()));
                                        finish();
                                    } else {
                                        ShareUtil.findChooserToDeal(getContext(), file.getAbsolutePath());
                                    }
                                }).show();
                        return;
                    }
                    ShareUtil.findChooserToDeal(getContext(), file.getAbsolutePath());
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                File file = adapter.getList().get(position);
                if ("b1".equals(file.getName()) || "b2".equals(file.getName())) {
                    return;
                }
                String[] titles;
                if (file.isDirectory()) {
                    titles = new String[]{"目录详情", "删除目录", "复制路径"};
                } else {
                    titles = new String[]{"分享文件", "外部打开", "文件详情", "删除文件", "复制路径"};
                }
                new XPopup.Builder(getContext())
                        .asCenterList("选择操作", titles, (position1, text) -> {
                            switch (text) {
                                case "分享文件":
                                    ShareUtil.findChooserToSend(getContext(), file.getAbsolutePath());
                                    break;
                                case "外部打开":
                                    ShareUtil.findChooserToDeal(getContext(), "file://" + file.getAbsolutePath());
                                    break;
                                case "复制路径":
                                    ClipboardUtil.copyToClipboardForce(getContext(), file.getAbsolutePath());
                                    break;
                                case "目录详情":
                                    String size1 = FileUtil.getFormatedFileSize(FileUtil.getFolderSize(file));
                                    File[] children = file.listFiles();
                                    String[] list1 = new String[]{
                                            "目录名称：" + file.getName(),
                                            "目录大小：" + size1,
                                            "修改时间：" + TimeUtil.formatTime(file.lastModified()),
                                            "子文件数：" + (children == null ? 0 : children.length)
                                    };
                                    new XPopup.Builder(getContext())
                                            .asCustom(new FileDetailPopup(FileBrowserActivity.this, text, list1)).show();
                                    break;
                                case "文件详情":
                                    String size = FileUtil.getFormatedFileSize(FileUtil.getFolderSize(file));
                                    String[] list = new String[]{
                                            "文件名称：" + file.getName(),
                                            "文件大小：" + size,
                                            "修改时间：" + TimeUtil.formatTime(file.lastModified())
                                    };
                                    new XPopup.Builder(getContext())
                                            .asCustom(new FileDetailPopup(FileBrowserActivity.this, text, list)).show();
                                    break;
                                case "删除目录":
                                    new XPopup.Builder(getContext())
                                            .asConfirm("温馨提示", "确认删除该目录下所有文件吗？注意删除后无法恢复！", () -> {
                                                FileUtil.deleteDirs(file.getAbsolutePath());
                                                ToastMgr.shortBottomCenter(getContext(), "目录已删除");
                                                getFileDir(curPathTextView.getText().toString());
                                            }).show();
                                    break;
                                case "删除文件":
                                    new XPopup.Builder(getContext())
                                            .asConfirm("温馨提示", "确认删除该文件吗？注意删除后无法恢复！", () -> {
                                                FileUtil.deleteFile(file.getAbsolutePath());
                                                ToastMgr.shortBottomCenter(getContext(), "文件已删除");
                                                getFileDir(curPathTextView.getText().toString());
                                            }).show();
                                    break;
                            }
                        }).show();
            }
        });
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);
        getFileDir(rootPath);

        String dataPath = getIntent().getStringExtra("path");
        if (StringUtil.isNotEmpty(dataPath)) {
            getFileDir(dataPath);
        } else {
            getFileDir(rootPath);
        }
    }

    private void getFileDir(String filePath) {
        curPathTextView.setText(filePath);
        List<File> itemsList = adapter.getList();
        itemsList.clear();
        File file = new File(filePath);
        File[] files = file.listFiles();
        if (!filePath.equals("/")) {
            File file1 = new File("b1");
            itemsList.add(file1);
            File file2 = new File("b2");
            itemsList.add(file2);
            parentPath = file.getParent();
        } else {
            parentPath = filePath;
        }
        if (files == null) {
            files = new File[]{};
        }
        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        Collections.sort(fileList, (o1, o2) -> {
            if (StringUtils.isEmpty(o1.getName())) {
                return -1;
            }
            if (StringUtils.isEmpty(o2.getName())) {
                return 1;
            }
            if (o1.isDirectory() && o2.isFile()) {
                return -1;
            }
            if (o1.isFile() && o2.isDirectory()) {
                return 1;
            }
            return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        });
        itemsList.addAll(fileList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (StringUtils.equals(curPathTextView.getText(), rootPath) || StringUtils.equals(curPathTextView.getText(), getIntent().getStringExtra("path"))) {
            super.onBackPressed();
        } else {
            getFileDir(parentPath);
        }
    }
}
