package biz.dealnote.xmpp.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Transformation;

import java.util.List;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.AccountContactPair;
import biz.dealnote.xmpp.model.User;
import biz.dealnote.xmpp.util.Avatars;
import biz.dealnote.xmpp.util.RoundTransformation;

public class AccountsListAdapter extends ArrayAdapter<AccountContactPair> {

    private Context context;
    private List<AccountContactPair> data;
    private Transformation transformation;

    public AccountsListAdapter(Context context, List<AccountContactPair> objects) {
        super(context, R.layout.item_account_lite, objects);
        this.context = context;
        this.data = objects;
        this.transformation = new RoundTransformation();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public AccountContactPair getItem(int position) {
        return data.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_account_lite, parent, false);
            final Holder viewHolder = new Holder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
        }

        final Holder holder = (Holder) view.getTag();
        final AccountContactPair item = data.get(position);

        String jid = item.account.buildBareJid();

        User user = item.user;
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

        Avatars.displayAvatar(getContext(), holder, jid, photoHash, transformation);

        return view;
    }

    public class Holder implements Avatars.AvatarWithLetter {

        ImageView avatar;
        TextView letter;
        TextView jid;
        TextView name;

        Holder(View itemView) {
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
    }
}
