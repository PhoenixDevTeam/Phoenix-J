package biz.dealnote.xmpp.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Transformation;

import java.util.List;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.AppRosterEntry;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.util.Avatars;
import biz.dealnote.xmpp.util.RoundTransformation;

public class RosterEntriesAdapter extends RecyclerView.Adapter<RosterEntriesAdapter.Holder> {

    private Context context;
    private List<AppRosterEntry> data;
    private Transformation transformation;
    private ClickListener clickListener;

    public RosterEntriesAdapter(List<AppRosterEntry> data, Context context) {
        this.data = data;
        this.context = context;
        this.transformation = new RoundTransformation();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false));
    }

    @Override
    public void onBindViewHolder(final Holder holder, int position) {
        final AppRosterEntry entry = data.get(position);
        Contact contact = entry.contact;
        String ownerJid = entry.account.buildBareJid();

        AppRosterEntry previous = position == 0 ? null : data.get(position - 1);

        boolean needShowHeader = previous == null || !previous.account.buildBareJid().equalsIgnoreCase(ownerJid);

        holder.header.setVisibility(needShowHeader ? View.VISIBLE : View.GONE);
        holder.headerText.setText(ownerJid);

        holder.name.setText(contact.getDispayName());

        String statusText = getStatusText(entry);
        holder.textStatus.setVisibility(TextUtils.isEmpty(statusText) ? View.GONE : View.VISIBLE);
        holder.textStatus.setText(statusText);
        holder.textStatus.setTextColor(getSubtextColor(entry));

        Avatars.displayAvatar(context, holder, contact, transformation);

        holder.main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onClick(holder.getAdapterPosition(), entry);
                }
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

    private int getSubtextColor(AppRosterEntry entry){
        int secondaryColor = ContextCompat.getColor(context, R.color.textColorSecondary);
        int disabledColor = ContextCompat.getColor(context, R.color.textColorDisabled);

        return entry.presenceType == null || entry.presenceType != AppRosterEntry.PRESENCE_TYPE_AVAILABLE
                ? disabledColor : secondaryColor;
    }

    private String getStatusText(AppRosterEntry entry){
        if(entry == null || entry.presenceType == null) return null;

        if(!TextUtils.isEmpty(entry.presenceStatus)){
            return entry.presenceStatus;
        }

        if(entry.presenceType == AppRosterEntry.PRESENCE_TYPE_UNAVAILABLE){
            return context.getString(R.string.unavailable);
        }

        if(entry.presenceType == AppRosterEntry.PRESENCE_TYPE_AVAILABLE && entry.presenceMode != null){

            switch (entry.presenceMode){
                case AppRosterEntry.PRESENSE_MODE_AVAILABLE:
                    return context.getString(R.string.available);
                case AppRosterEntry.PRESENSE_MODE_AWAY:
                    return context.getString(R.string.away);
                case AppRosterEntry.PRESENSE_MODE_CHAT:
                    return context.getString(R.string.ready_for_chat);
                case AppRosterEntry.PRESENSE_MODE_DND:
                    return context.getString(R.string.dnd);
                case AppRosterEntry.PRESENSE_MODE_XA:
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

    public interface ClickListener {
        void onClick(int position, AppRosterEntry entry);
    }

    public class Holder extends RecyclerView.ViewHolder implements Avatars.AvatarWithLetter {

        public View header;
        public TextView headerText;

        public View main;
        public ImageView avatar;
        public TextView avatarLetter;
        public TextView name;
        public TextView textStatus;
        public View status;
        public ImageView favorite;

        public Holder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header_root);
            headerText = (TextView) itemView.findViewById(R.id.header_text);

            main = itemView.findViewById(R.id.main);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            avatarLetter = (TextView) itemView.findViewById(R.id.avatar_letter);
            name = (TextView) itemView.findViewById(R.id.name);
            textStatus = (TextView) itemView.findViewById(R.id.text_status);
            status = itemView.findViewById(R.id.status);
            favorite = (ImageView) itemView.findViewById(R.id.favorite);
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
