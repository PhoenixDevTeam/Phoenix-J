package biz.dealnote.xmpp.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.FileItem;

public class FileManagerAdapter extends RecyclerView.Adapter<FileManagerAdapter.Holder> {

    private Context context;
    private List<FileItem> data;
    private ClickListener clickListener;

    public FileManagerAdapter(Context context, List<FileItem> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_file, parent, false));
    }

    @Override
    public void onBindViewHolder(final Holder holder, int position) {
        final FileItem item = data.get(position);
        holder.icon.setImageResource(item.icon);
        holder.fileName.setText(item.file);
        holder.fileDetails.setText(item.details);
        holder.fileDetails.setVisibility(TextUtils.isEmpty(item.details) ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onClick(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface ClickListener {
        void onClick(FileItem item);
    }

    public class Holder extends RecyclerView.ViewHolder {

        TextView fileName;
        TextView fileDetails;
        ImageView icon;

        public Holder(View itemView) {
            super(itemView);
            fileName = (TextView) itemView.findViewById(R.id.item_file_name);
            fileDetails = (TextView) itemView.findViewById(R.id.item_file_details);
            icon = (ImageView) itemView.findViewById(R.id.item_file_icon);
        }
    }
}
