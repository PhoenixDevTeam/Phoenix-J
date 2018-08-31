package biz.dealnote.xmpp.adapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.adapter.bindable.RecyclerBindableAdapter;
import biz.dealnote.xmpp.model.UserAttribute;

public class UserAttributesRecyclerAdapter extends RecyclerBindableAdapter<UserAttribute, UserAttributesRecyclerAdapter.Holder> {

    private static final String EMPTY = "--";
    private List<UserAttribute> mData;
    private ActionListener actionListener;

    public UserAttributesRecyclerAdapter(List<UserAttribute> items) {
        super(items);
        this.mData = items;
    }

    @Override
    protected void onBindItemViewHolder(Holder viewHolder, int position, int type) {
        final UserAttribute attribute = mData.get(position);
        viewHolder.value.setText(TextUtils.isEmpty(attribute.value) ? EMPTY : attribute.value);
        viewHolder.title.setText(attribute.title);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onClick(attribute);
                }
            }
        });
    }

    @Override
    protected Holder viewHolder(View view, int type) {
        return new Holder(view);
    }

    @Override
    protected int layoutId(int type) {
        return R.layout.item_user_attribute;
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public interface ActionListener {
        void onClick(UserAttribute attribute);
    }

    class Holder extends RecyclerView.ViewHolder {

        TextView title;
        TextView value;

        Holder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            value = itemView.findViewById(R.id.value);
        }
    }
}