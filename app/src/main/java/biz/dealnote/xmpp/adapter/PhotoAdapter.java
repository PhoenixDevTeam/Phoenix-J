package biz.dealnote.xmpp.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.MemoryPolicy;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import biz.dealnote.xmpp.Constants;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.LocalPhoto;
import biz.dealnote.xmpp.util.PicassoInstance;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    private Context context;
    private List<LocalPhoto> data;
    private PhotoSelectionListener listener;

    public PhotoAdapter(Context context, List<LocalPhoto> data) {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.photo_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final LocalPhoto p = data.get(position);

        PicassoInstance.get()
                .load(p.buildUriForPicasso())
                .tag(Constants.PICASSO_TAG)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoSelected(p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setListener(PhotoSelectionListener listener) {
        this.listener = listener;
    }

    public interface PhotoSelectionListener {
        void onPhotoSelected(LocalPhoto localPhoto);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView photoImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.imageView);
        }
    }
}