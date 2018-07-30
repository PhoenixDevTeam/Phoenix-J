package biz.dealnote.xmpp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Transformation;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.AppFile;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.util.AvatarResorce;
import biz.dealnote.xmpp.util.Avatars;
import biz.dealnote.xmpp.util.Objects;
import biz.dealnote.xmpp.util.PicassoInstance;
import biz.dealnote.xmpp.util.RoundTransformation;
import biz.dealnote.xmpp.util.Utils;
import biz.dealnote.xmpp.view.MySeekbarListener;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_INPUT = 0;
    private static final int TYPE_OUT = 1;
    private static final int TYPE_FILE_TRANSFER = 2;
    private static final int TYPE_AUDIO_MESSAGE = 3;
    private static final int TYPE_SUBSCRIPTION = 4;

    private final int secondaryTextColor;

    private List<AppMessage> data;
    private Context context;
    private AvatarResorce avatarResorce;
    private Transformation transformation;
    private ActionListener actionListener;
    private SubscriptionActionListener subscriptionActionListener;

    private SharedHolders<AudioMessageHolder> mAudioMessageHolders;

    public MessagesAdapter(@NonNull List<AppMessage> data, @NonNull AvatarResorce avatarResorce, @NonNull Context context) {
        this.data = data;
        this.context = context;
        this.avatarResorce = avatarResorce;
        this.transformation = new RoundTransformation();
        this.secondaryTextColor = Utils.getSecondaryTextColor(context);
        this.mAudioMessageHolders = new SharedHolders<>(false);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case TYPE_INPUT:
                return new MessageHolder(inflater.inflate(R.layout.item_input_message, parent, false));
            case TYPE_OUT:
                return new MessageHolder(inflater.inflate(R.layout.item_out_message, parent, false));
            case TYPE_FILE_TRANSFER:
                return new FileTransferHolder(inflater.inflate(R.layout.item_transfer_file_message, parent, false));
            case TYPE_AUDIO_MESSAGE:
                return new AudioMessageHolder(inflater.inflate(R.layout.item_message_audio, parent, false));
            case TYPE_SUBSCRIPTION:
                return new SubscriptionHolder(inflater.inflate(R.layout.item_subscription_message, parent, false));
        }

        throw new IllegalStateException();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case TYPE_OUT:
            case TYPE_INPUT:
                bindMessageHolder((MessageHolder) holder, position);
                break;
            case TYPE_FILE_TRANSFER:
                bindFileTransferHolder((FileTransferHolder) holder, data.get(position));
                break;
            case TYPE_AUDIO_MESSAGE:
                bindAudioMessageHolder((AudioMessageHolder) holder, data.get(position));
                break;
            case TYPE_SUBSCRIPTION:
                bindSubscriptionHolder((SubscriptionHolder) holder, data.get(position));
                break;
        }
    }

    private void bindSubscriptionHolder(SubscriptionHolder holder, final AppMessage message) {
        diplayAvatarFor(holder, message);

        String destination = message.getDestination();

        boolean hasReason = message.getStatus() == AppMessage.STATUS_ACCEPTED || message.getStatus() == AppMessage.STATUS_DECLINED;

        holder.reason.setVisibility(hasReason ? View.VISIBLE : View.GONE);
        holder.buttonsRoot.setVisibility(!message.isOut() && message.getType() == AppMessage.TYPE_SUBSCRIBE && !hasReason ? View.VISIBLE : View.GONE);

        switch (message.getStatus()) {
            case AppMessage.STATUS_ACCEPTED:
                holder.reason.setText(R.string.accepted);
                break;
            case AppMessage.STATUS_DECLINED:
                holder.reason.setText(R.string.declined);
                break;
        }

        String body = message.getMessageBody(context);

        Spannable spannable = Spannable.Factory.getInstance().newSpannable(body);
        int start = body.indexOf(destination);

        StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
        spannable.setSpan(styleSpan, start, start + destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.body.setText(spannable, TextView.BufferType.SPANNABLE);
        holder.time.setText(Utils.getDateFromUnixTime(message.getDate()));

        holder.acceptButton.setOnClickListener(v -> {
            if (subscriptionActionListener != null) {
                subscriptionActionListener.onSubscriptionAcceptClick(message);
            }
        });

        holder.declineButton.setOnClickListener(v -> {
            if (subscriptionActionListener != null) {
                subscriptionActionListener.onSubscriptionDeclineClick(message);
            }
        });
    }

    private void diplayAvatarFor(Avatars.AvatarWithLetter avatarWithLetter, AppMessage message) {
        AvatarResorce.Entry entry = avatarResorce.findById(message.getSenderId());
        String avatarHash = entry == null ? null : entry.hash;
        //byte[] avatar = entry == null ? null : entry.avatar;

        Avatars.displayAvatar(context, avatarWithLetter, message.getSenderJid(), avatarHash, transformation);
    }

    public interface AudioBindCallback {
        void onHolderCreate(int holderId, @NonNull AppMessage message);

        void onPlayButtonClick(int holderId, @NonNull AppMessage message);

        void onSeekbarMovedByUser(int position, @NonNull AppMessage message);
    }

    private AudioBindCallback mAudioBindCallback;

    public void setAudioBindCallback(AudioBindCallback audioBindCallback) {
        this.mAudioBindCallback = audioBindCallback;
    }

    private void bindAudioMessageHolder(AudioMessageHolder holder, final AppMessage message) {
        Objects.requireNonNull(mAudioBindCallback);
        mAudioMessageHolders.put(message.getId(), holder);

        diplayAvatarFor(holder, message);

        mAudioBindCallback.onHolderCreate(holder.getHolderId(), message);
        holder.playPause.setOnClickListener(v -> mAudioBindCallback.onPlayButtonClick(holder.getHolderId(), message));

        holder.seekbarListener.setCallback(new MySeekbarListener.Callback() {
            @Override
            public void onProgressChangedFromTouch(int progress) {
                holder.currentPosition.setText(Utils.getDurationString(progress / 1000));
                //mAudioBindCallback.onSeekbarMovedByUser(progress, message);
            }

            @Override
            public void onStopMovingByUserOn(int progress) {
                mAudioBindCallback.onSeekbarMovedByUser(progress, message);
            }
        });
    }

    public void bindAudioControls(Integer currentEntityId, boolean isPaused, int duration, int position) {
        SparseArray<Set<WeakReference<AudioMessageHolder>>> cache = mAudioMessageHolders.getCache();
        for (int i = 0; i < cache.size(); i++) {
            int entityId = cache.keyAt(i);
            Set<WeakReference<AudioMessageHolder>> weakReferences = cache.get(entityId);

            for (WeakReference<AudioMessageHolder> reference : weakReferences) {
                AudioMessageHolder holder = reference.get();
                if (holder == null) {
                    continue;
                }

                boolean isCurrent = currentEntityId != null && currentEntityId == entityId;
                bindAudioControls(holder, isCurrent, isPaused, duration, position);
            }
        }
    }

    public void bindAudioControls(int holderId, boolean isCurrent, boolean isPaused, int duration, int position) {
        AudioMessageHolder holder = mAudioMessageHolders.findHolderByHolderId(holderId);
        if (holder != null) {
            bindAudioControls(holder, isCurrent, isPaused, duration, position);
        }
    }

    private void bindAudioControls(AudioMessageHolder holder, boolean isCurrent, boolean isPaused, int duration, int position) {
        MySeekbarListener seekbarListener = holder.seekbarListener;

        if (holder.seekBar.getMax() != duration) {
            holder.seekBar.setMax(duration);
        }

        holder.seekBar.setEnabled(isCurrent);

        holder.playPause.setImageResource(isCurrent && !isPaused ? R.drawable.ic_pause_oval_vector : R.drawable.ic_play_oval_vector);

        if (!seekbarListener.isTouchNow() && isCurrent) {
            holder.seekBar.setProgress(position);
        }

        holder.currentPosition.setVisibility(isCurrent ? View.VISIBLE : View.INVISIBLE);
        holder.duration.setVisibility(isCurrent ? View.VISIBLE : View.INVISIBLE);

        if (isCurrent) {
            int displayPosition;
            if (seekbarListener.isTouchNow()) {
                displayPosition = seekbarListener.getTargetPosition();
            } else {
                displayPosition = position;
            }

            holder.currentPosition.setText(Utils.getDurationString(displayPosition / 1000));
            holder.duration.setText(Utils.getDurationString(duration / 1000));
        }
    }

    private void bindFileTransferHolder(FileTransferHolder holder, final AppMessage message) {
        diplayAvatarFor(holder, message);

        final AppFile appFile = message.getAttachedFile();

        String title = context.getString(R.string.file_transfer) + ": " + appFile.name;
        if (message.getType() == AppMessage.TYPE_INCOME_FILE) {
            String fileSizeMb = Utils.getSizeString(appFile.size);
            title = title + ", " + fileSizeMb;
        }

        holder.title.setText(title);
        holder.time.setText(Utils.getDateFromUnixTime(message.getDate()));
        holder.time.setTextColor(message.getStatus() == AppMessage.STATUS_CANCELLED ? Color.RED : secondaryTextColor);

        holder.acceptButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onFileTransferAcceptClick(message);
            }
        });

        holder.declineButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onFileTransferDeclineClick(message);
            }
        });

        /*holder.openFileButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onFileOpen(message.getId(), appFile);
            }
        });*/

        holder.progress.setProgress(message.getProgress());

        boolean canOpenFile = message.getStatus() == AppMessage.STATUS_DONE && !message.isOut();

        //holder.openFileButton.setVisibility(canOpenFile ? View.VISIBLE : View.GONE);

        holder.progress.setVisibility(message.getStatus() == AppMessage.STATUS_IN_PROGRESS ? View.VISIBLE : View.GONE);
        holder.buttonsRoot.setVisibility(message.getStatus() == AppMessage.STATUS_CANCELLED ? View.GONE : View.VISIBLE);

        switch (message.getStatus()) {
            case AppMessage.STATUS_WAITING_FOR_REASON:
                holder.acceptButton.setVisibility(message.isOut() ? View.GONE : View.VISIBLE);
                holder.declineButton.setVisibility(View.VISIBLE);
                break;
            case AppMessage.STATUS_CANCELLED:
                holder.time.setText(R.string.cancelled);
                break;
            case AppMessage.STATUS_IN_PROGRESS:
                holder.acceptButton.setVisibility(View.GONE);
                holder.declineButton.setVisibility(View.VISIBLE);

                String percent = message.getProgress() + "%";
                holder.time.setText(percent);
                break;
            case AppMessage.STATUS_DONE:
                holder.acceptButton.setVisibility(View.GONE);
                holder.declineButton.setVisibility(View.GONE);
                break;
        }
    }

    private void bindMessageHolder(MessageHolder holder, int position) {
        final AppMessage message = data.get(position);

        if (message.isSelected()) {
            PicassoInstance.get()
                    .cancelRequest(holder.avatar);
            holder.avatar.setVisibility(View.VISIBLE);
            holder.avatar.setImageResource(R.drawable.ic_check_oval);
        } else {
            diplayAvatarFor(holder, message);
        }

        holder.body.setText(message.getMessageBody(context), TextView.BufferType.SPANNABLE);
        holder.time.setTextColor(message.getStatus() == AppMessage.STATUS_ERROR ? Color.RED : secondaryTextColor);

        switch (message.getStatus()) {
            case AppMessage.STATUS_SENT:
                holder.time.setText(Utils.getDateFromUnixTime(message.getDate()));
                break;
            case AppMessage.STATUS_ERROR:
                holder.time.setText(R.string.error);
                break;
            case AppMessage.STATUS_IN_QUEUE:
                holder.time.setText(R.string.in_queue);
                break;
            case AppMessage.STATUS_SENDING:
                holder.time.setText(R.string.sending);
                break;
        }

        holder.itemView.setOnLongClickListener(v -> actionListener != null
                && actionListener.onMessageLongClick(holder.getAdapterPosition(), message));

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onMessageClicked(holder.getAdapterPosition(), message);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        AppMessage message = data.get(position);

        if (message.isVoiceMessage() && message.hasSavedFile() && message.getStatus() == AppMessage.STATUS_DONE) {
            return TYPE_AUDIO_MESSAGE;
        }

        if (message.getType() == AppMessage.TYPE_INCOME_FILE || message.getType() == AppMessage.TYPE_OUTGOING_FILE) {
            return TYPE_FILE_TRANSFER;
        }

        if (message.getType() == AppMessage.TYPE_SUBSCRIBE ||
                message.getType() == AppMessage.TYPE_SUBSCRIBED ||
                message.getType() == AppMessage.TYPE_UNSUBSCRIBE ||
                message.getType() == AppMessage.TYPE_UNSUBSCRIBED) {
            return TYPE_SUBSCRIPTION;
        }

        return message.isOut() ? TYPE_OUT : TYPE_INPUT;
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setSubscriptionActionListener(SubscriptionActionListener subscriptionActionListener) {
        this.subscriptionActionListener = subscriptionActionListener;
    }

    public void setData(List<AppMessage> data, AvatarResorce avatarResorce) {
        this.data = data;
        this.avatarResorce = avatarResorce;
        notifyDataSetChanged();
    }

    public interface ActionListener {
        void onFileTransferAcceptClick(AppMessage message);

        void onFileTransferDeclineClick(AppMessage message);

        void onFileOpen(int mid, AppFile request);

        void onMessageClicked(int position, AppMessage message);

        boolean onMessageLongClick(int position, AppMessage message);
    }

    public interface SubscriptionActionListener {
        void onSubscriptionAcceptClick(AppMessage message);

        void onSubscriptionDeclineClick(AppMessage message);
    }

    private class MessageHolder extends RecyclerView.ViewHolder implements Avatars.AvatarWithLetter {

        ImageView avatar;
        TextView avatarLetter;
        View bodyRoot;
        TextView body;
        TextView time;

        MessageHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            avatarLetter = itemView.findViewById(R.id.avatar_letter);
            bodyRoot = itemView.findViewById(R.id.message_body_root);
            body = itemView.findViewById(R.id.messge_body);
            time = itemView.findViewById(R.id.time);
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

    private class AudioMessageHolder extends RecyclerView.ViewHolder
            implements Avatars.AvatarWithLetter, IdentificableHolder {

        ImageView avatar;
        TextView avatarLetter;
        SeekBar seekBar;
        ImageView playPause;
        TextView currentPosition;
        TextView duration;
        MySeekbarListener seekbarListener;

        AudioMessageHolder(View itemView) {
            super(itemView);
            itemView.setTag(generateHolderId());

            avatar = itemView.findViewById(R.id.avatar);
            avatarLetter = itemView.findViewById(R.id.avatar_letter);
            seekBar = itemView.findViewById(R.id.seekbar);
            playPause = itemView.findViewById(R.id.play_pause_button);
            currentPosition = itemView.findViewById(R.id.current_position);
            duration = itemView.findViewById(R.id.duration);

            seekbarListener = new MySeekbarListener();
            seekBar.setOnSeekBarChangeListener(seekbarListener);
        }

        @Override
        public ImageView getAvatarView() {
            return avatar;
        }

        @Override
        public TextView getLetterView() {
            return avatarLetter;
        }

        @Override
        public int getHolderId() {
            return (int) itemView.getTag();
        }
    }

    private int mNextHolderId;

    private int generateHolderId() {
        mNextHolderId++;
        return mNextHolderId;
    }

    private class FileTransferHolder extends RecyclerView.ViewHolder implements Avatars.AvatarWithLetter {

        ImageView avatar;
        TextView avatarLetter;
        TextView title;
        TextView time;
        View buttonsRoot;
        View acceptButton;
        View declineButton;
        //View openFileButton;
        ProgressBar progress;

        FileTransferHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            avatarLetter = itemView.findViewById(R.id.avatar_letter);
            title = itemView.findViewById(R.id.title);
            time = itemView.findViewById(R.id.time);
            buttonsRoot = itemView.findViewById(R.id.buttons_root);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
            //openFileButton = itemView.findViewById(R.id.open_file_button);
            progress = itemView.findViewById(R.id.progressBar);
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

    private class SubscriptionHolder extends RecyclerView.ViewHolder implements Avatars.AvatarWithLetter {

        ImageView avatar;
        TextView avatarLetter;
        TextView body;
        TextView time;
        View buttonsRoot;
        View acceptButton;
        View declineButton;
        TextView reason;

        SubscriptionHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            avatarLetter = itemView.findViewById(R.id.avatar_letter);
            body = itemView.findViewById(R.id.body);
            time = itemView.findViewById(R.id.time);
            buttonsRoot = itemView.findViewById(R.id.buttons_root);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
            reason = itemView.findViewById(R.id.reason);
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