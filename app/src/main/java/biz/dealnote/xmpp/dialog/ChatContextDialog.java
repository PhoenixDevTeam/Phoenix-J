package biz.dealnote.xmpp.dialog;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.db.DBHelper;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.db.columns.MessagesColumns;
import biz.dealnote.xmpp.model.Chat;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.util.Avatars;
import biz.dealnote.xmpp.util.RoundTransformation;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ChatContextDialog extends DialogFragment implements Avatars.AvatarWithLetter, View.OnClickListener {

    private Chat mChat;
    private ImageView mAvatar;
    private TextView mAvatarLetter;
    private TextView mMessagesCount;
    private TextView mFilesCount;

    public static ChatContextDialog newInstance(@NonNull Chat chat) {
        Bundle args = new Bundle();
        args.putParcelable(Extra.CHAT, chat);
        ChatContextDialog dialog = new ChatContextDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChat = getArguments().getParcelable(Extra.CHAT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(getDialog() != null && getDialog().getWindow() != null){
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        View root = inflater.inflate(R.layout.dialog_chat_context, container, false);

        mAvatar = (ImageView) root.findViewById(R.id.avatar);
        mAvatarLetter = (TextView) root.findViewById(R.id.avatar_letter);

        Avatars.displayAvatar(getActivity(), this, mChat.getInterlocutor(), new RoundTransformation());

        TextView jid = (TextView) root.findViewById(R.id.jid);
        jid.setText(mChat.getDestination());

        Contact contact = mChat.getInterlocutor();
        String firstLastName = null;

        if (contact != null) {
            if (!TextUtils.isEmpty(contact.getFirstName())) {
                firstLastName = contact.getFirstName();
            }

            if (!TextUtils.isEmpty(contact.getLastName())) {
                if (firstLastName == null) {
                    firstLastName = contact.getLastName();
                } else {
                    firstLastName = firstLastName + " " + contact.getLastName();
                }
            }
        }

        TextView name = (TextView) root.findViewById(R.id.first_last_name);
        name.setVisibility(TextUtils.isEmpty(firstLastName) ? View.GONE : View.VISIBLE);
        name.setText(firstLastName);

        mMessagesCount = (TextView) root.findViewById(R.id.messages_count);
        mFilesCount = (TextView) root.findViewById(R.id.files_count);

        root.findViewById(R.id.button_delete).setOnClickListener(this);
        root.findViewById(R.id.button_hide).setOnClickListener(this);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fetchCounters();
    }

    private void fetchCounters() {
        new AsyncTask<Void, Void, Counters>() {
            @Override
            protected Counters doInBackground(Void... params) {
                Counters counters = new Counters();
                counters.messsages = getMessagesCountByTypes(AppMessage.TYPE_CHAT, AppMessage.TYPE_GROUP_CHAT, AppMessage.TYPE_NORMAL);
                counters.files = getMessagesCountByTypes(AppMessage.TYPE_INCOME_FILE, AppMessage.TYPE_OUTGOING_FILE);
                return counters;
            }

            @Override
            protected void onPostExecute(Counters counters) {
                if (!isAdded()) return;
                mMessagesCount.setText(getString(R.string.messages_count, counters.messsages));
                mFilesCount.setText(getString(R.string.files_count, counters.files));
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private int getMessagesCountByTypes(@NonNull Integer... types) {
        Cursor cursor = DBHelper.getInstance(getActivity()).getReadableDatabase().rawQuery("SELECT COUNT(" + MessagesColumns._ID + ") " +
                " FROM " + MessagesColumns.TABLENAME +
                " WHERE " + MessagesColumns.CHAT_ID + " = ? " +
                " AND " + MessagesColumns.TYPE + " IN(" + TextUtils.join(",", types) + ")", new String[]{String.valueOf(mChat.getId())});

        int result = 0;
        if (cursor.moveToNext()) {
            result = cursor.getInt(0);
        }

        cursor.close();
        return result;
    }

    private void doHideChat() {
        boolean targetHideState = !mChat.isHidden();
        // TODO: 03.11.2016 append disposable
        Repositories.getInstance()
                .getChats()
                .setChatHidden(mChat.getId(), targetHideState)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    mChat.setHidden(targetHideState);
                });
    }

    private void doDeleteChat() {
        // TODO: 03.11.2016 append disposable
        Repositories.getInstance()
                .getChats()
                .removeChatWithMessages(mChat.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::dismiss, throwable -> Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_LONG).show());
    }

    private void showDeleteChatConfirmation() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.confirmation)
                .setMessage(R.string.delete_chat_confiramtion)
                .setPositiveButton(R.string.button_yes, (dialog, which) -> doDeleteChat())
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_hide:
                doHideChat();
                break;
            case R.id.button_delete:
                showDeleteChatConfirmation();
                break;
        }
    }

    @Override
    public ImageView getAvatarView() {
        return mAvatar;
    }

    @Override
    public TextView getLetterView() {
        return mAvatarLetter;
    }

    private static class Counters {
        int messsages;
        int files;
    }
}
