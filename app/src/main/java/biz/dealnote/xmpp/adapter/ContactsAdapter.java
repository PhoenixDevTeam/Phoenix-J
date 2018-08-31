package biz.dealnote.xmpp.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Transformation;

import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.model.User;
import biz.dealnote.xmpp.util.Avatars;
import biz.dealnote.xmpp.util.RoundTransformation;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.Holder> {

    private Context context;
    private List<Contact> data;
    private Transformation transformation;
    private ClickListener clickListener;

    public ContactsAdapter(List<Contact> data, Context context) {
        this.data = data;
        this.context = context;
        this.transformation = new RoundTransformation();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, int position) {
        final Contact entry = data.get(position);
        User user = entry.user;
        String ownerJid = entry.accountId.getJid();

        Contact previous = position == 0 ? null : data.get(position - 1);

        boolean needShowHeader = previous == null || !previous.accountId.getJid().equalsIgnoreCase(ownerJid);

        holder.header.setVisibility(needShowHeader ? View.VISIBLE : View.GONE);
        holder.headerText.setText(ownerJid);

        holder.name.setText(user.getDispayName());

        String statusText = getStatusText(entry);
        holder.textStatus.setVisibility(TextUtils.isEmpty(statusText) ? View.GONE : View.VISIBLE);
        holder.textStatus.setText(statusText);
        holder.textStatus.setTextColor(getSubtextColor(entry));

        Avatars.displayAvatar(context, holder, user, transformation);

        holder.main.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(holder.getAdapterPosition(), entry);
            }
        });

        holder.favorite.setVisibility(entry.priority > 0 ? View.VISIBLE : View.GONE);

        if (entry.needSendSubscribePresence()) {
            holder.status.setVisibility(View.INVISIBLE);
        } else {
            holder.status.setVisibility(View.VISIBLE);

            if (!entry.availableToReceiveMessages) {
                holder.status.setBackgroundResource(R.drawable.status_unavaivable_background);
            } else {
                if (entry.away) {
                    holder.status.setBackgroundResource(R.drawable.status_away_background);
                } else {
                    holder.status.setBackgroundResource(R.drawable.status_avaivable_background);
                }
            }
        }
    }

    private int getSubtextColor(Contact entry){
        int secondaryColor = ContextCompat.getColor(context, R.color.textColorSecondary);
        int disabledColor = ContextCompat.getColor(context, R.color.textColorDisabled);

        return entry.presenceType == null || entry.presenceType != Contact.PRESENCE_TYPE_AVAILABLE
                ? disabledColor : secondaryColor;
    }

    private String getStatusText(Contact entry){
        if(entry == null || entry.presenceType == null) return null;

        if(!TextUtils.isEmpty(entry.presenceStatus)){
            return entry.presenceStatus;
        }

        if(entry.presenceType == Contact.PRESENCE_TYPE_UNAVAILABLE){
            return context.getString(R.string.unavailable);
        }

        if(entry.presenceType == Contact.PRESENCE_TYPE_AVAILABLE && entry.presenceMode != null){

            switch (entry.presenceMode){
                case Contact.PRESENSE_MODE_AVAILABLE:
                    return context.getString(R.string.available);
                case Contact.PRESENSE_MODE_AWAY:
                    return context.getString(R.string.away);
                case Contact.PRESENSE_MODE_CHAT:
                    return context.getString(R.string.ready_for_chat);
                case Contact.PRESENSE_MODE_DND:
                    return context.getString(R.string.dnd);
                case Contact.PRESENSE_MODE_XA:
                    return context.getString(R.string.xa);
            }
        }

        return null;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setData(List<Contact> contacts) {
        this.data = contacts;
        notifyDataSetChanged();
    }

    public interface ClickListener {
        void onClick(int position, Contact entry);
    }

    class Holder extends RecyclerView.ViewHolder implements Avatars.AvatarWithLetter {

        View header;
        TextView headerText;
        View main;
        ImageView avatar;
        TextView avatarLetter;
        TextView name;
        TextView textStatus;
        View status;
        ImageView favorite;

        Holder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header_root);
            headerText = itemView.findViewById(R.id.header_text);

            main = itemView.findViewById(R.id.main);
            avatar = itemView.findViewById(R.id.avatar);
            avatarLetter = itemView.findViewById(R.id.avatar_letter);
            name = itemView.findViewById(R.id.name);
            textStatus = itemView.findViewById(R.id.text_status);
            status = itemView.findViewById(R.id.status);
            favorite = itemView.findViewById(R.id.favorite);
        }

        @Override
        public ImageView getAvatarView() {
            return avatar;
        }

        @Override
        public TextView getLetterView() {
            return avatarLetter;
        }
    }
}