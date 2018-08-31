package biz.dealnote.xmpp.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.IncomeFileItem;
import biz.dealnote.xmpp.util.Utils;

public class IncomeFilesAdapter extends RecyclerView.Adapter<IncomeFilesAdapter.Holder> {

    private Context context;
    private List<IncomeFileItem> data;
    private ClickListener clickListener;

    public IncomeFilesAdapter(Context context, List<IncomeFileItem> data) {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_file, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, int position) {
        final IncomeFileItem item = data.get(position);
        holder.icon.setImageResource(item.icon);
        holder.fileName.setText(item.file.getName());
        holder.fileName.setTransformationMethod(null);

        holder.fileDetails.setText(Utils.formatBytes(item.file.length()));

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(holder.getAdapterPosition(), item);
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
        void onClick(int index, IncomeFileItem item);
    }

    class Holder extends RecyclerView.ViewHolder {

        TextView fileName;
        TextView fileDetails;
        ImageView icon;

        Holder(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.item_file_name);
            fileDetails = itemView.findViewById(R.id.item_file_details);
            icon = itemView.findViewById(R.id.item_file_icon);
        }
    }
}