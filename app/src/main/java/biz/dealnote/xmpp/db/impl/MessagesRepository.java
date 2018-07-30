package biz.dealnote.xmpp.db.impl;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biz.dealnote.xmpp.db.ChatContentProvider;
import biz.dealnote.xmpp.db.DBHelper;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.db.columns.ContactsColumns;
import biz.dealnote.xmpp.db.columns.MessagesColumns;
import biz.dealnote.xmpp.db.exception.AlreadyExistException;
import biz.dealnote.xmpp.db.exception.DataValidateException;
import biz.dealnote.xmpp.db.exception.DatabaseException;
import biz.dealnote.xmpp.db.exception.RecordDoesNotExistException;
import biz.dealnote.xmpp.db.interfaces.IChatsRepository;
import biz.dealnote.xmpp.db.interfaces.IContactsRepository;
import biz.dealnote.xmpp.db.interfaces.IMessagesRepository;
import biz.dealnote.xmpp.model.AppFile;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.model.MessageBuilder;
import biz.dealnote.xmpp.model.MessageCriteria;
import biz.dealnote.xmpp.model.MessageUpdate;
import biz.dealnote.xmpp.util.AvatarResorce;
import biz.dealnote.xmpp.util.Pair;
import biz.dealnote.xmpp.util.Unixtime;
import biz.dealnote.xmpp.util.Utils;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

import static biz.dealnote.xmpp.util.Utils.isEmpty;
import static biz.dealnote.xmpp.util.Utils.listEmptyIfNull;
import static biz.dealnote.xmpp.util.Utils.nonEmpty;
import static biz.dealnote.xmpp.util.Utils.safeCountOf;
import static biz.dealnote.xmpp.util.Utils.safelyCloseCursor;

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
public class MessagesRepository extends AbsRepository implements IMessagesRepository {

    private final PublishSubject<AppMessage> addingPublisher;
    private final PublishSubject<Pair<Integer, MessageUpdate>> statusChangePubisher;
    private final PublishSubject<Pair<Integer, Set<Integer>>> deletionPublisher;
    private final DBHelper dbHelper;

    public MessagesRepository(Repositories repositories) {
        super(repositories);
        this.dbHelper = DBHelper.getInstance(repositories);
        this.addingPublisher = PublishSubject.create();
        this.statusChangePubisher = PublishSubject.create();
        this.deletionPublisher = PublishSubject.create();
    }

    private final Object messageAddLock = new Object();

    @Override
    public Single<AppMessage> saveMessage(@NonNull final MessageBuilder builder) {
        return Single.create(e -> {
            // нельзя вставить более одного сообщения, только по очереди
            // потому что создаются и обновляются хидеры чатов
            synchronized (messageAddLock) {
                if (builder.getDestination() == null) {
                    throw new DataValidateException("Unknown message destination");
                }

                if (builder.getSenderJid() == null) {
                    throw new DataValidateException("Unknown message senderJid");
                }

                String destination = Utils.getBareJid(builder.getDestination());
                String senderJid = Utils.getBareJid(builder.getSenderJid());

                if (!TextUtils.isEmpty(builder.getUniqueServiceId())) {
                    boolean alreadyExist = hasMessageWithStanza(builder.getAccountId(), destination, builder.getUniqueServiceId())
                            .blockingGet();

                    if (alreadyExist) {
                        throw new AlreadyExistException("Message with stanzaid " + builder.getUniqueServiceId() + "already exist");
                    }
                }

                IContactsRepository contactsRepository = getRepositories().getContactsRepository();

                int targetInterlocutorId;
                if (builder.getInterlocutorId() != null) {
                    targetInterlocutorId = builder.getInterlocutorId();
                } else {
                    targetInterlocutorId = contactsRepository.getContactIdPutIfNotExist(destination)
                            .blockingGet();
                }

                int targetSenderId;
                if (senderJid.equalsIgnoreCase(destination)) {
                    targetSenderId = targetInterlocutorId;
                } else {
                    targetSenderId = contactsRepository.getContactIdPutIfNotExist(senderJid)
                            .blockingGet();
                }

                IChatsRepository.IChatUpdateRequest request = new ChatUpdateRequest()
                        .setChatId(builder.getChatId())
                        .setDestination(destination)
                        .setAccountId(builder.getAccountId())
                        .setGroupChat(false)
                        .setInterlocutorId(targetInterlocutorId)
                        .setLastMessageOut(builder.isOut())
                        .setLastMessageTime(builder.getDate())
                        .setLastMessageBody(builder.getBody())
                        .setLastMessageType(builder.getType())
                        .setLastMessageRead(builder.isReadState());

                int targetChatId = getRepositories().getChats()
                        .updateChatHeaderWith(request)
                        .blockingGet();

                ContentValues cv = new ContentValues();
                cv.put(MessagesColumns.ACCOUNT_ID, builder.getAccountId());
                cv.put(MessagesColumns.CHAT_ID, targetChatId);
                cv.put(MessagesColumns.DESTINATION, destination);
                cv.put(MessagesColumns.SENDER_ID, targetSenderId);
                cv.put(MessagesColumns.SENDER_JID, senderJid);
                cv.put(MessagesColumns.UNIQUE_SERVICE_ID, builder.getUniqueServiceId());
                cv.put(MessagesColumns.TYPE, builder.getType());
                cv.put(MessagesColumns.BODY, builder.getBody());
                cv.put(MessagesColumns.STATUS, builder.getStatus());
                cv.put(MessagesColumns.OUT, builder.isOut());
                cv.put(MessagesColumns.READ_STATE, builder.isReadState());
                cv.put(MessagesColumns.DATE, builder.getDate());
                cv.put(MessagesColumns.WAS_ENCRYPTED, builder.isWasEncrypted());

                if (builder.getAppFile() != null) {
                    AppFile file = builder.getAppFile();
                    cv.put(MessagesColumns.ATTACHED_FILE_PATH, file.getUri() != null ? file.getUri().getPath() : null);
                    cv.put(MessagesColumns.ATTACHED_FILE_NAME, file.getName());
                    cv.put(MessagesColumns.ATTACHED_FILE_SIZE, file.getSize());
                    cv.put(MessagesColumns.ATTACHED_FILE_MIME, file.getMime());
                    cv.put(MessagesColumns.ATTACHED_FILE_DESCRIPTION, file.getDescription());
                }

                Uri uri = getContentResolver().insert(ChatContentProvider.MESSAGES_CONTENT_URI, cv);
                if (uri == null) {
                    throw new DatabaseException("Insert result URI is NULL");
                }

                int mid = Integer.parseInt(uri.getPathSegments().get(1));

                AppMessage result = new AppMessage()
                        .setId(mid)
                        .setAccountId(builder.getAccountId())
                        .setChatId(targetChatId)
                        .setDestination(destination)
                        .setSenderId(targetSenderId)
                        .setSenderJid(senderJid)
                        .setStanzaId(builder.getUniqueServiceId())
                        .setType(builder.getType())
                        .setBody(builder.getBody())
                        .setStatus(builder.getStatus())
                        .setOut(builder.isOut())
                        .setReadState(builder.isReadState())
                        .setDate(builder.getDate())
                        .setAttachedFile(builder.getAppFile());

                notifyAboutNewMessageAdded(result);
                e.onSuccess(result);
            }
        });
    }

    @Override
    public Observable<AppMessage> createAddMessageObservable() {
        return addingPublisher;
    }

    @Override
    public Observable<Pair<Integer, MessageUpdate>> createMessageUpdateObservable() {
        return statusChangePubisher;
    }

    @Override
    public Observable<Pair<Integer, Set<Integer>>> createMessageDeleteObservable() {
        return deletionPublisher;
    }

    private void notifyAboutMessagesDeleted(int chatId, Set<Integer> mids) {
        deletionPublisher.onNext(new Pair<>(chatId, mids));
    }

    private void notifyAboutMessageStatusChange(int messageId, MessageUpdate update) {
        statusChangePubisher.onNext(new Pair<>(messageId, update));
    }

    private void notifyAboutNewMessageAdded(@NonNull AppMessage message) {
        addingPublisher.onNext(message);
    }

    @Override
    public Single<Boolean> hasMessageWithStanza(int accountId, @NonNull String destination, String stanzaId) {
        return Single.create(e -> {
            String where = MessagesColumns.ACCOUNT_ID + " = ? AND " + MessagesColumns.DESTINATION + " LIKE ? AND " + MessagesColumns.UNIQUE_SERVICE_ID + " LIKE ?";
            String[] args = {String.valueOf(accountId), destination, stanzaId};

            Cursor cursor = getContext().getContentResolver().query(ChatContentProvider.MESSAGES_CONTENT_URI, new String[]{MessagesColumns._ID}, where, args, null);
            boolean has = cursor != null && cursor.getCount() > 0;

            if (cursor != null) {
                cursor.close();
            }

            e.onSuccess(has);
        });
    }

    @Override
    public Completable updateMessage(int messageId, @NonNull MessageUpdate update) {
        return Completable.create(e -> {
            ContentValues cv = new ContentValues();

            if (update.getStatusUpdate() != null) {
                int messageStatus = update.getStatusUpdate().getStatus();
                cv.put(MessagesColumns.STATUS, messageStatus);

                if (messageStatus == AppMessage.STATUS_SENT) {
                    // обновляем дату сообщения
                    cv.put(MessagesColumns.DATE, Unixtime.now());
                }
            }

            if (update.getFileUriUpdate() != null) {
                Uri uri = update.getFileUriUpdate().getUri();
                cv.put(MessagesColumns.ATTACHED_FILE_PATH, uri == null ? null : uri.toString());
            }

            int count = getContentResolver().update(ChatContentProvider.MESSAGES_CONTENT_URI, cv,
                    MessagesColumns._ID + " = ?", new String[]{String.valueOf(messageId)});

            if (count > 0) {
                notifyAboutMessageStatusChange(messageId, update);
                e.onComplete();
            } else {
                e.onError(new RecordDoesNotExistException());
            }
        });
    }

    @Override
    public Maybe<AppMessage> findLastMessage(int chatId) {
        return Maybe.create(e -> {
            Cursor cursor = getContentResolver().query(ChatContentProvider.MESSAGES_CONTENT_URI,
                    null, MessagesColumns.CHAT_ID + " = ?", new String[]{String.valueOf(chatId)}, MessagesColumns._ID + " DESC LIMIT 1");
            AppMessage message = null;

            try {
                if (cursor != null && cursor.moveToNext()) {
                    message = map(cursor);
                }
            } finally {
                safelyCloseCursor(cursor);
            }

            if (message != null) {
                e.onSuccess(message);
            }

            e.onComplete();
        });
    }

    @Override
    public Single<Boolean> deleteMessages(int chatId, Set<Integer> mids) {
        return Single.create(e -> {
            int count = getContentResolver().delete(ChatContentProvider.MESSAGES_CONTENT_URI,
                    MessagesColumns._ID + " IN(" + TextUtils.join(",", mids) + ")", null);

            if (count == 0) {
                e.onError(new RecordDoesNotExistException());
                return;
            }

            notifyAboutMessagesDeleted(chatId, mids);

            AppMessage message = findLastMessage(chatId).blockingGet();
            if (message == null) {
                getRepositories().getChats()
                        .removeChatWithMessages(chatId)
                        .blockingAwait();
                e.onSuccess(true);
            } else {
                IChatsRepository.IChatUpdateRequest request = new ChatUpdateRequest()
                        .setChatId(chatId)
                        .setDestination(message.getDestination())
                        .setAccountId(message.getAccountId())
                        .setGroupChat(false)
                        .setInterlocutorId(message.getSenderId())
                        .setLastMessageOut(message.isOut())
                        .setLastMessageTime(message.getDate())
                        .setLastMessageBody(message.getBody())
                        .setLastMessageType(message.getType())
                        .setLastMessageRead(message.isReadState());

                getRepositories().getChats()
                        .updateChatHeaderWith(request)
                        .blockingGet();
                e.onSuccess(false);
            }
        });
    }

    private static final String[] AVATARS_COLUMNS = {ContactsColumns._ID, ContactsColumns.PHOTO, ContactsColumns.PHOTO_HASH};

    @Override
    public Single<Pair<List<AppMessage>, List<AvatarResorce.Entry>>> load(MessageCriteria criteria) {
        if (criteria.getChatId() == null) {
            if (criteria.getAccountId() == null || criteria.getDestination() == null) {
                return Single.error(new IllegalArgumentException("Invalid criteria"));
            }
        }

        return Single.create(emitter -> {
            String where;
            String[] whereArgs;

            if (criteria.getStartMessageId() == null) {
                if (criteria.getChatId() != null) {
                    where = MessagesColumns.CHAT_ID + " = ?";
                    whereArgs = new String[]{String.valueOf(criteria.getChatId())};
                } else {
                    where = MessagesColumns.ACCOUNT_ID + " = ? AND " + MessagesColumns.DESTINATION + " LIKE ?";
                    whereArgs = new String[]{String.valueOf(criteria.getAccountId()), criteria.getDestination()};
                }
            } else {
                if (criteria.getChatId() != null) {
                    where = MessagesColumns.CHAT_ID + " = ? AND " + MessagesColumns._ID + " < ?";
                    whereArgs = new String[]{String.valueOf(criteria.getChatId()), String.valueOf(criteria.getStartMessageId())};
                } else {
                    where = MessagesColumns.ACCOUNT_ID + " = ? AND " + MessagesColumns.DESTINATION + " LIKE ? AND " + MessagesColumns._ID + " < ?";
                    whereArgs = new String[]{String.valueOf(criteria.getAccountId()), criteria.getDestination(), String.valueOf(criteria.getStartMessageId())};
                }
            }

            Cursor cursor = getContentResolver().query(ChatContentProvider.MESSAGES_CONTENT_URI,
                    null, where, whereArgs, MessagesColumns._ID + " DESC LIMIT " + criteria.getCount());

            Set<Integer> contactIds;
            if (nonEmpty(criteria.getForceLoadContactIds())) {
                contactIds = new HashSet<>(criteria.getForceLoadContactIds());
            } else {
                contactIds = new HashSet<>();
            }

            List<AppMessage> messages = new ArrayList<>(safeCountOf(cursor));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed()) break;

                    AppMessage message = map(cursor);
                    messages.add(message);

                    if (isEmpty(criteria.getIgnoreContactIds()) || !criteria.getIgnoreContactIds().contains(message.getSenderId())) {
                        contactIds.add(message.getSenderId());
                    }
                }

                cursor.close();
            }

            if (emitter.isDisposed()) return;

            List<AvatarResorce.Entry> avatarEntries = null;
            if (nonEmpty(contactIds)) {
                String cWhere = ContactsColumns.FULL_ID + " IN (" + TextUtils.join(",", contactIds) + ")";
                Cursor cCursor = dbHelper.getReadableDatabase().query(ContactsColumns.TABLENAME, AVATARS_COLUMNS, cWhere, null, null, null, null);

                if (cCursor != null) {
                    avatarEntries = new ArrayList<>(cCursor.getCount());
                    while (cCursor.moveToNext()) {
                        if (emitter.isDisposed()) break;

                        int cid = cCursor.getInt(cCursor.getColumnIndex(ContactsColumns._ID));
                        String hash = cCursor.getString(cCursor.getColumnIndex(ContactsColumns.PHOTO_HASH));

                        if (nonEmpty(hash)) {
                            avatarEntries.add(new AvatarResorce.Entry(cid, hash));
                        }
                    }

                    cCursor.close();
                }
            }

            emitter.onSuccess(new Pair<>(messages, listEmptyIfNull(avatarEntries, true)));
        });
    }

    private static AppMessage map(Cursor cursor) {
        int type = cursor.getInt(cursor.getColumnIndex(MessagesColumns.TYPE));

        AppFile file = null;
        if (type == AppMessage.TYPE_INCOME_FILE || type == AppMessage.TYPE_OUTGOING_FILE) {
            String path = cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_PATH));

            file = new AppFile(TextUtils.isEmpty(path) ? null : Uri.parse(path),
                    cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_NAME)),
                    cursor.getLong(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_SIZE)));
            file.mime = cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_MIME));
            file.description = cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_DESCRIPTION));
        }

        return new AppMessage()
                .setId(cursor.getInt(cursor.getColumnIndex(MessagesColumns._ID)))
                .setAccountId(cursor.getInt(cursor.getColumnIndex(MessagesColumns.ACCOUNT_ID)))
                .setChatId(cursor.getInt(cursor.getColumnIndex(MessagesColumns.CHAT_ID)))
                .setSenderId(cursor.getInt(cursor.getColumnIndex(MessagesColumns.SENDER_ID)))
                .setSenderJid(cursor.getString(cursor.getColumnIndex(MessagesColumns.SENDER_JID)))
                .setStanzaId(cursor.getString(cursor.getColumnIndex(MessagesColumns.UNIQUE_SERVICE_ID)))
                .setType(type)
                .setDestination(cursor.getString(cursor.getColumnIndex(MessagesColumns.DESTINATION)))
                .setBody(cursor.getString(cursor.getColumnIndex(MessagesColumns.BODY)))
                .setStatus(cursor.getInt(cursor.getColumnIndex(MessagesColumns.STATUS)))
                .setOut(cursor.getInt(cursor.getColumnIndex(MessagesColumns.OUT)) == 1)
                .setReadState(cursor.getInt(cursor.getColumnIndex(MessagesColumns.READ_STATE)) == 1)
                .setDate(cursor.getLong(cursor.getColumnIndex(MessagesColumns.DATE)))
                .setAttachedFile(file);
    }


    private static class ChatUpdateRequest implements IChatsRepository.IChatUpdateRequest {

        Integer chatId;
        String destination;
        int accountId;
        boolean groupChat;
        int interlocutorId;
        boolean lastMessageOut;
        long lastMessageTime;
        String lastMessageBody;
        int lastMessageType;
        boolean lastMessageRead;

        @Nullable
        @Override
        public Integer getChatId() {
            return chatId;
        }

        @Override
        public String getDestination() {
            return destination;
        }

        @Override
        public int getAccountId() {
            return accountId;
        }

        @Override
        public boolean isGroupChat() {
            return groupChat;
        }

        @Override
        public int getInterlocutorId() {
            return interlocutorId;
        }

        @Override
        public boolean isLastMessageOut() {
            return lastMessageOut;
        }

        @Override
        public long getLastMessageTime() {
            return lastMessageTime;
        }

        @Override
        public String getLastMessageBody() {
            return lastMessageBody;
        }

        @Override
        public int getLastMessageType() {
            return lastMessageType;
        }

        @Override
        public boolean isLastMessageRead() {
            return lastMessageRead;
        }

        ChatUpdateRequest setChatId(Integer chatId) {
            this.chatId = chatId;
            return this;
        }

        ChatUpdateRequest setDestination(String destination) {
            this.destination = destination;
            return this;
        }

        ChatUpdateRequest setAccountId(int accountId) {
            this.accountId = accountId;
            return this;
        }

        ChatUpdateRequest setGroupChat(boolean groupChat) {
            this.groupChat = groupChat;
            return this;
        }

        ChatUpdateRequest setInterlocutorId(int interlocutorId) {
            this.interlocutorId = interlocutorId;
            return this;
        }

        ChatUpdateRequest setLastMessageOut(boolean lastMessageOut) {
            this.lastMessageOut = lastMessageOut;
            return this;
        }

        ChatUpdateRequest setLastMessageTime(long lastMessageTime) {
            this.lastMessageTime = lastMessageTime;
            return this;
        }

        ChatUpdateRequest setLastMessageBody(String lastMessageBody) {
            this.lastMessageBody = lastMessageBody;
            return this;
        }

        ChatUpdateRequest setLastMessageType(int lastMessageType) {
            this.lastMessageType = lastMessageType;
            return this;
        }

        ChatUpdateRequest setLastMessageRead(boolean lastMessageRead) {
            this.lastMessageRead = lastMessageRead;
            return this;
        }
    }
}
