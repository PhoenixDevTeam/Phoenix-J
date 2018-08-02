package biz.dealnote.xmpp.adapter;

import android.content.Context;
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
import biz.dealnote.xmpp.model.User;
import biz.dealnote.xmpp.settings.AbsSettings;
import biz.dealnote.xmpp.settings.AccountSettings;
import biz.dealnote.xmpp.settings.NotificationSettings;
import biz.dealnote.xmpp.settings.SimpleSetting;
import biz.dealnote.xmpp.util.Avatars;
import biz.dealnote.xmpp.util.RoundTransformation;

public class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ACCOUNT = 1;
    private static final int TYPE_NOTIFICATION = 2;
    private Context context;
    private List<AbsSettings> data;
    private Transformation transformation;
    private ActionListener actionListener;

    public SettingsAdapter(Context context, List<AbsSettings> data) {
        this.context = context;
        this.data = data;
        this.transformation = new RoundTransformation();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_ACCOUNT:
                return new AccountHolder(LayoutInflater.from(context).inflate(R.layout.item_account, parent, false));
            case TYPE_NOTIFICATION:
                return new NotificationHolder(LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false));
        }

        return null;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        AbsSettings current = data.get(position);
        AbsSettings previous = position == 0 ? null : data.get(position - 1);
        AbsSettings next = position == data.size() - 1 ? null : data.get(position + 1);

        boolean needShowHeader = previous == null || !previous.section.equals(current.section);
        boolean needShowFooter = next == null || !next.section.equals(current.section);

        if (holder instanceof HeaderFooter) {
            HeaderFooter headerFooter = (HeaderFooter) holder;
            if (headerFooter.footerView() != null) {
                headerFooter.footerView().setVisibility(needShowFooter ? View.VISIBLE : View.GONE);
            }

            if (headerFooter.headerView() != null) {
                headerFooter.headerView().setVisibility(needShowHeader ? View.VISIBLE : View.GONE);
            }
        }

        AbsSettings settings = data.get(position);

        switch (getItemViewType(position)) {
            case TYPE_ACCOUNT:
                bindAccountViewHolder((AccountHolder) holder, (AccountSettings) settings);
                break;
            case TYPE_NOTIFICATION:
                if (settings instanceof NotificationSettings) {
                    bindNotificatioViewHolder((NotificationHolder) holder, (NotificationSettings) settings);
                } else {
                    bindSimpleHolder((NotificationHolder) holder, (SimpleSetting) settings);
                }
                break;
        }
    }

    private void bindSimpleHolder(NotificationHolder holder, final SimpleSetting setting) {
        holder.headerText.setText(setting.section.titleRes);
        holder.title.setText(setting.titleRes);
        holder.value.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onSimpleOptionClick(setting);
                }
            }
        });
    }

    private void bindNotificatioViewHolder(NotificationHolder holder, final NotificationSettings settings) {
        holder.value.setVisibility(View.VISIBLE);

        holder.headerText.setText(settings.section.titleRes);

        holder.title.setText(settings.notifyTileRes);
        holder.value.setText(settings.value.buildInfoLine(context));

        holder.contentRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onNotificationClick(settings);
                }
            }
        });
    }

    private void bindAccountViewHolder(AccountHolder holder, final AccountSettings settings) {
        holder.headerText.setText(settings.section.titleRes);

        String jid = settings.accountContactPair.account.buildBareJid();

        User user = settings.accountContactPair.user;
        String firstLastName = null;

        if (user != null) {
            if (!TextUtils.isEmpty(user.getFirstName())) {
                firstLastName = user.getFirstName();
            }

            if (!TextUtils.isEmpty(user.getLastName())) {
                if (firstLastName == null) {
                    firstLastName = user.getLastName();
                } else {
                    firstLastName = firstLastName + " " + user.getLastName();
                }
            }
        }

        holder.name.setVisibility(TextUtils.isEmpty(firstLastName) ? View.GONE : View.VISIBLE);
        holder.name.setText(firstLastName);
        holder.jid.setText(jid);

        String photoHash = user == null ? null : user.getPhotoHash();
        //byte[] avatar = user == null ? null : user.getAvatar();

        Avatars.displayAvatar(context, holder, jid, photoHash, transformation);

        holder.contentRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onAccountClick(settings);
                }
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        AbsSettings settings = data.get(position);

        if (settings instanceof AccountSettings) {
            return TYPE_ACCOUNT;
        }

        if (settings instanceof NotificationSettings) {
            return TYPE_NOTIFICATION;
        }

        if (settings instanceof SimpleSetting) {
            return TYPE_NOTIFICATION;
        }

        return 0;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }
    public interface ActionListener {

        void onAccountClick(AccountSettings settings);

        void onNotificationClick(NotificationSettings settings);

        void onSimpleOptionClick(SimpleSetting setting);
    }

    public interface HeaderFooter {
        View headerView();

        View footerView();
    }

    public class AccountHolder extends RecyclerView.ViewHolder implements Avatars.AvatarWithLetter, HeaderFooter {

        View header;
        TextView headerText;

        View contentRoot;
        ImageView avatar;
        TextView letter;
        TextView jid;
        TextView name;

        public AccountHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header_root);
            headerText = (TextView) itemView.findViewById(R.id.header_text);

            contentRoot = itemView.findViewById(R.id.content_root);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            letter = (TextView) itemView.findViewById(R.id.avatar_letter);
            jid = (TextView) itemView.findViewById(R.id.jid);
            name = (TextView) itemView.findViewById(R.id.first_last_name);
        }

        @Override
        public ImageView getAvatarView() {
            return avatar;
        }

        @Override
        public TextView getLetterView() {
            return letter;
        }

        @Override
        public View headerView() {
            return header;
        }

        @Override
        public View footerView() {
            return null;
        }
    }

    public class NotificationHolder extends RecyclerView.ViewHolder implements HeaderFooter {

        View header;
        TextView headerText;

        View contentRoot;
        TextView title;
        TextView value;

        public NotificationHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header_root);
            headerText = (TextView) itemView.findViewById(R.id.header_text);
            contentRoot = itemView.findViewById(R.id.content_root);
            title = (TextView) itemView.findViewById(R.id.title);
            value = (TextView) itemView.findViewById(R.id.value);
        }

        @Override
        public View headerView() {
            return header;
        }

        @Override
        public View footerView() {
            return null;
        }
    }
}
