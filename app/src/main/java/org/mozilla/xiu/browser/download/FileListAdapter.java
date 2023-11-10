package org.mozilla.xiu.browser.download;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.mozilla.xiu.browser.R;

import java.io.File;
import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2017/9/10
 * 时间：At 17:26
 */

class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;

    public List<File> getList() {
        return list;
    }

    private List<File> list;
    private OnItemClickListener onItemClickListener;

    FileListAdapter(Context context, List<File> list) {
        this.context = context;
        this.list = list;
    }

    interface OnItemClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AvatarHolder(LayoutInflater.from(context).inflate(R.layout.item_file, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int position) {
        if (viewHolder instanceof AvatarHolder) {
            AvatarHolder holder = (AvatarHolder) viewHolder;
            File file = list.get(position);
            if ("b1".equals(file.getName())) {
                holder.textView.setText("返回根目录..");
                updateImg(holder.imageView, R.drawable.icon_back);
            } else if ("b2".equals(file.getName())) {
                holder.textView.setText("返回上一层..");
                updateImg(holder.imageView, R.drawable.icon_back02);
            } else {
                holder.textView.setText(file.getName());
                if (file.isDirectory()) {
                    updateImg(holder.imageView, R.drawable.icon_folder3);
                } else {
                    String type = DownloadChooser.smartFilm(file.getName(), true);
                    int id = R.drawable.icon_unknown;
                    if ("安装包".equals(type)) {
                        id = R.drawable.icon_app3;
                    } else if ("压缩包".equals(type)) {
                        id = R.drawable.icon_zip2;
                    } else if ("音乐/音频".equals(type)) {
                        id = R.drawable.icon_music3;
                    } else if ("文档/电子书".equals(type)) {
                        id = R.drawable.icon_txt2;
                    } else if ("其它格式".equals(type)) {
                        //id = R.drawable.icon_unknown;
                    } else if ("图片".equals(type)) {
                        id = R.drawable.icon_pic3;
                    } else if ("视频".equals(type)) {
                        id = R.drawable.icon_video2;
                    }
                    updateImg(holder.imageView, id);
                }
            }

            holder.resultBg.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    if (holder.getAdapterPosition() >= 0) {
                        onItemClickListener.onClick(v, holder.getAdapterPosition());
                    }
                }
            });

            holder.resultBg.setOnLongClickListener(v -> {
                if (onItemClickListener != null) {
                    if (holder.getAdapterPosition() >= 0) {
                        onItemClickListener.onLongClick(v, holder.getAdapterPosition());
                    }
                }
                return true;
            });
        }
    }

    private void updateImg(ImageView imageView, int id) {
        Glide.with(context)
                .load(id)
                .into(imageView);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    private static class AvatarHolder extends RecyclerView.ViewHolder {
        View resultBg;
        ImageView imageView;
        TextView textView;

        AvatarHolder(View itemView) {
            super(itemView);
            resultBg = itemView.findViewById(R.id.bg);
            imageView = itemView.findViewById(R.id.item_reult_img);
            textView = itemView.findViewById(R.id.textView);
        }
    }
}
