package org.mozilla.xiu.browser.download;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.mozilla.xiu.browser.R;

import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2017/9/10
 * 时间：At 17:26
 */

public class FileDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;

    public List<String> getList() {
        return list;
    }

    private List<String> list;
    private OnClickListener clickListener;

    public FileDetailAdapter(Context context, List<String> list, OnClickListener clickListener) {
        this.context = context;
        this.list = list;
        this.clickListener = clickListener;
    }

    public interface OnClickListener {
        void click(String text);
        void longClick(View view, String text);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TitleHolder(LayoutInflater.from(context).inflate(R.layout.item_file_detail, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int position) {
        if (viewHolder instanceof TitleHolder) {
            TitleHolder holder = (TitleHolder) viewHolder;
            String title = list.get(position);
            holder.title.setText(title);
            holder.bg.setOnClickListener(v -> clickListener.click(list.get(holder.getAdapterPosition())));
            holder.bg.setOnLongClickListener(v -> {
                clickListener.longClick(v, list.get(holder.getAdapterPosition()));
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private class TitleHolder extends RecyclerView.ViewHolder {
        TextView title;
        View bg;

        TitleHolder(View itemView) {
            super(itemView);
            bg = itemView.findViewById(R.id.bg);
            title = itemView.findViewById(R.id.textView);
        }
    }
}
