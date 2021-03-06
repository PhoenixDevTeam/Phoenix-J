package biz.dealnote.xmpp.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.LocalImageAlbum;
import biz.dealnote.xmpp.util.PicassoInstance;

public class PhotoAlbumsRecyclerAdapter extends RecyclerView.Adapter<PhotoAlbumsRecyclerAdapter.ViewHolder> {

    private Context context;
    private List<LocalImageAlbum> data;
    private ClickListner mClickListner;

    public PhotoAlbumsRecyclerAdapter(Context context, List<LocalImageAlbum> data) {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.local_album_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final LocalImageAlbum album = data.get(position);

        PicassoInstance.get()
                .load(album.buildUriForPicasso())
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView);


        holder.tvName.setText(album.getName().toUpperCase());
        holder.itemView.setOnClickListener(v -> {
            if (mClickListner != null) {
                mClickListner.onAlbumSelected(album);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setmClickListner(ClickListner mClickListner) {
        this.mClickListner = mClickListner;
    }

    public interface ClickListner {
        void onAlbumSelected(LocalImageAlbum album);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView photoImageView;
        TextView tvName;

        public ViewHolder(View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.image);
            tvName = itemView.findViewById(R.id.title);
        }
    }
}