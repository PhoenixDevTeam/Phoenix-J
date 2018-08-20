package biz.dealnote.xmpp.db.impl;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.xmpp.db.ChatContentProvider;
import biz.dealnote.xmpp.db.Storages;
import biz.dealnote.xmpp.db.columns.ChatsColumns;
import biz.dealnote.xmpp.db.exception.DatabaseException;
import biz.dealnote.xmpp.db.exception.RecordDoesNotExistException;
import biz.dealnote.xmpp.db.interfaces.IChatsRepository;
import biz.dealnote.xmpp.model.Chat;
import biz.dealnote.xmpp.model.ChatUpdateModel;
import biz.dealnote.xmpp.model.HiddenUpdate;
import biz.dealnote.xmpp.model.LastMessageUpdate;
import biz.dealnote.xmpp.model.UnreadCountUpdate;
import biz.dealnote.xmpp.model.User;
import biz.dealnote.xmpp.util.Exestime;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

import static biz.dealnote.xmpp.util.Utils.safeCountOf;

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
public class ChatsRepository extends AbsRepository implements IChatsRepository {

    private final PublishSubject<Chat> creationPublisher;
    private final PublishSubject<ChatUpdateModel> updatesPublisher;
    private final PublishSubject<Integer> deletionPublisher;

    public ChatsRepository(Storages storages) {
        super(storages);
        this.creationPublisher = PublishSubject.create();
        this.updatesPublisher = PublishSubject.create();
        this.deletionPublisher = PublishSubject.create();
    }

    @Override
    public Observable<Chat> observeChatCreation() {
        return creationPublisher;
    }

    @Override
    public Observable<ChatUpdateModel> observeChatUpdate() {
        return updatesPublisher;
    }

    @Override
    public Observable<Integer> observeChatDeletion() {
        return deletionPublisher;
    }

    @Override
    public Single<Integer> updateChatHeaderWith(@NonNull IChatUpdateRequest request) {
        return Single.create(e -> {
            long start = System.currentTimeMillis();

            Integer targetChatId = request.getChatId();

            if (targetChatId == null) {
                targetChatId = findByDestination(request.getAccountId(), request.getDestination())
                        .blockingGet();
            }

            if (targetChatId == null) {
                int targerUnreadCount = request.isLastMessageOut() || request.isLastMessageRead() ? 0 : 1;

                ContentValues insertChatCv = new ContentValues();
                insertChatCv.put(ChatsColumns.ACCOUNT_ID, request.getAccountId());
                insertChatCv.put(ChatsColumns.DESTINATION, request.getDestination());
                insertChatCv.put(ChatsColumns.IS_GROUP_CHAT, request.isGroupChat());
                // insertChatCv.put(ChatsColumns.TITLE, null); // TODO: 23.12.2015
                insertChatCv.put(ChatsColumns.UNREAD_COUNT, targerUnreadCount);
                insertChatCv.put(ChatsColumns.INTERLOCUTOR_ID, request.getInterlocutorId()); // TODO: 23.12.2015
                insertChatCv.put(ChatsColumns.LAST_MESSAGE_OUT, request.isLastMessageOut());
                insertChatCv.put(ChatsColumns.LAST_MESSAGE_TIME, request.getLastMessageTime());
                insertChatCv.put(ChatsColumns.LAST_MESSAGE_TEXT, request.getLastMessageBody());
                insertChatCv.put(ChatsColumns.LAST_MESSAGE_TYPE, request.getLastMessageType());

                Uri chatUri = getContentResolver().insert(ChatContentProvider.CHATS_CONTENT_URI, insertChatCv);
                if (chatUri == null) {
                    throw new DatabaseException("Chat insert result URI in NULL");
                }

                targetChatId = Integer.parseInt(chatUri.getPathSegments().get(1));

                Chat chat = new Chat()
                        .setId(targetChatId)
                        .setAccountId(request.getAccountId())
                        .setDestination(request.getDestination())
                        .setGroupChat(request.isGroupChat())
                        //.setTitle() // TODO: 23.12.2015
                        .setUnreadCount(targerUnreadCount)
                        .setInterlocutorId(request.getInterlocutorId())
                        .setInterlocutor(getRepositories().getUsers().findById(request.getInterlocutorId()).blockingGet().get())
                        .setHidden(false)
                        .setLastMessageText(request.getLastMessageBody())
                        .setLastMessageTime(request.getLastMessageTime())
                        .setLastMessageOut(request.isLastMessageOut())
                        .setLastMessageType(request.getLastMessageType());

                notifyAboutChatCreated(chat);
            } else {
                int targerUnreadCount = request.isLastMessageOut() || request.isLastMessageRead()
                        ? 0 : getUnreadCount(targetChatId).blockingGet() + 1;

                ContentValues updateChatCv = new ContentValues();
                updateChatCv.put(ChatsColumns.LAST_MESSAGE_OUT, request.isLastMessageOut());
                updateChatCv.put(ChatsColumns.LAST_MESSAGE_TIME, request.getLastMessageTime());
                updateChatCv.put(ChatsColumns.LAST_MESSAGE_TEXT, request.getLastMessageBody());
                updateChatCv.put(ChatsColumns.LAST_MESSAGE_TYPE, request.getLastMessageType());
                updateChatCv.put(ChatsColumns.UNREAD_COUNT, targerUnreadCount);
                updateChatCv.put(ChatsColumns.HIDDEN, false); // ВАЖНО !!!

                getContentResolver().update(ChatContentProvider.CHATS_CONTENT_URI, updateChatCv,
                        ChatsColumns._ID + " = ?", new String[]{String.valueOf(targetChatId)});

                LastMessageUpdate lastMessageUpdate = new LastMessageUpdate(
                        request.getLastMessageBody(),
                        request.getLastMessageTime(),
                        request.isLastMessageOut(),
                        request.getLastMessageType());

                ChatUpdateModel update = new ChatUpdateModel(targetChatId, new UnreadCountUpdate(targerUnreadCount), lastMessageUpdate, new HiddenUpdate(false));

                notifyAboutChatUpdate(update);
            }

            //BusProvider.getINSTANCE().post(new ChatUpdateEvent(targetChatId, lastMessageTime, isLastMessageOut, lastMessageBody, lastMessageType));

            Exestime.log("updateChatHeaderWith", start, request.getChatId());
            e.onSuccess(targetChatId);
        });
    }

    @Override
    public Single<Integer> getUnreadCount(int chatId) {
        return Single.create(e -> {
            Cursor cursor = getContentResolver().query(ChatContentProvider.CHATS_CONTENT_URI,
                    new String[]{ChatsColumns.UNREAD_COUNT},
                    ChatsColumns.FULL_ID + " = ?", new String[]{String.valueOf(chatId)}, null);

            Integer count = null;
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    count = cursor.getInt(cursor.getColumnIndex(ChatsColumns.UNREAD_COUNT));
                }

                cursor.close();
            }

            if (count == null) {
                throw new RecordDoesNotExistException();
            } else {
                e.onSuccess(count);
            }
        });
    }

    @Override
    public Single<List<Chat>> getAll(boolean includeHidden) {
        return Single.create(e -> {
            String where = includeHidden ? null : ChatsColumns.HIDDEN + " IS NULL OR " + ChatsColumns.HIDDEN + " != ?";
            String[] whereArgs = includeHidden ? null : new String[]{"1"};
            Cursor cursor = getContentResolver().query(ChatContentProvider.CHATS_CONTENT_URI, null, where, whereArgs, ChatsColumns.LAST_MESSAGE_TIME + " DESC");

            List<Chat> chats = new ArrayList<>(safeCountOf(cursor));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed()) {
                        break;
                    }

                    chats.add(map(cursor));
                }
            }

            e.onSuccess(chats);
        });
    }

    @Override
    public Maybe<Integer> findByDestination(int accountId, String destination) {
        return Maybe.create(e -> {
            String where = ChatsColumns.ACCOUNT_ID + " = ? AND " + ChatsColumns.DESTINATION + " LIKE ?";
            String[] args = new String[]{String.valueOf(accountId), destination};

            Cursor chatCursor = getContentResolver().query(ChatContentProvider.CHATS_CONTENT_URI, new String[]{ChatsColumns._ID},
                    where, args, null);

            Integer targetChatId = null;
            if (chatCursor != null) {
                if (chatCursor.moveToNext()) {
                    targetChatId = chatCursor.getInt(chatCursor.getColumnIndex(ChatsColumns._ID));
                }

                chatCursor.close();
            }

            if (targetChatId != null) {
                e.onSuccess(targetChatId);
            }

            e.onComplete();
        });
    }

    @Override
    public Maybe<Chat> findById(int id) {
        return Maybe.create(e -> {
            Cursor cursor = getContentResolver().query(ChatContentProvider.CHATS_CONTENT_URI,
                    null, ChatsColumns.FULL_ID + " = ?", new String[]{String.valueOf(id)}, null);

            Chat chat = null;
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    chat = map(cursor);
                }

                cursor.close();
            }

            e.onSuccess(chat);
        });
    }

    @Override
    public Completable setUnreadCount(int chatId, int unreadCount) {
        return Completable.create(e -> {
            ContentValues cv = new ContentValues();
            cv.put(ChatsColumns.UNREAD_COUNT, unreadCount);

            int count = getContentResolver().update(ChatContentProvider.CHATS_CONTENT_URI, cv,
                    ChatsColumns._ID + " = ?", new String[]{String.valueOf(chatId)});
            if (count > 0) {
                ChatUpdateModel update = new ChatUpdateModel(chatId, new UnreadCountUpdate(unreadCount), null, null);

                notifyAboutChatUpdate(update);
                e.onComplete();
            } else {
                e.onError(new RecordDoesNotExistException());
            }
        });
    }

    @Override
    public Completable setChatHidden(int chatId, boolean hidden) {
        return Completable.create(e -> {
            ContentValues cv = new ContentValues();
            cv.put(ChatsColumns.HIDDEN, hidden);

            int count = getContentResolver().update(ChatContentProvider.CHATS_CONTENT_URI, cv,
                    ChatsColumns._ID + " = ?", new String[]{String.valueOf(chatId)});
            if (count > 0) {
                ChatUpdateModel update = new ChatUpdateModel(chatId, null, null, new HiddenUpdate(hidden));

                notifyAboutChatUpdate(update);
                e.onComplete();
            } else {
                e.onError(new RecordDoesNotExistException());
            }
        });
    }

    @Override
    public Completable removeChatWithMessages(int chatId) {
        return Completable.create(e -> {
            String[] args = {String.valueOf(chatId)};

            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            operations.add(ContentProviderOperation.newDelete(ChatContentProvider.CHATS_CONTENT_URI)
                    .withSelection(ChatsColumns._ID + " = ?", args)
                    .build());

            try {
                getContentResolver().applyBatch(ChatContentProvider.AUTHORITY, operations);
                notifyAboutChatDelete(chatId);
                e.onComplete();
            } catch (RemoteException | OperationApplicationException error) {
                e.onError(error);
            }
        });
    }

    public static Chat map(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndex(ChatsColumns.INTERLOCUTOR_ID)))
                .setFirstName(cursor.getString(cursor.getColumnIndex(ChatsColumns.FOREIGN_INTERLOCUTOR_FIRST_NAME)))
                .setLastName(cursor.getString(cursor.getColumnIndex(ChatsColumns.FOREIGN_INTERLOCUTOR_LAST_NAME)))
                .setJid(cursor.getString(cursor.getColumnIndex(ChatsColumns.FOREIGN_INTERLOCUTOR_JID)))
                .setPhotoHash(cursor.getString(cursor.getColumnIndex(ChatsColumns.FOREIGN_INTERLOCUTOR_PHOTO_HASH)));
                //.setAvatar(cursor.getBlob(cursor.getColumnIndex(ChatsColumns.FOREIGN_INTERLOCUTOR_PHOTO)));

        //byte[] pubKeyBytes = cursor.getBlob(cursor.getColumnIndex(ChatsColumns.FOREIGN_ACCOUNT_PUBLIC_KEY));
        //byte[] privKeyBytes = cursor.getBlob(cursor.getColumnIndex(ChatsColumns.FOREIGN_ACCOUNT_PRIVATE_KEY));
        //KeyPair keyPair = Accounts.restoreDSAKeyPairFrom(pubKeyBytes, privKeyBytes);

        //Account account = new Account(
        //        cursor.getInt(cursor.getColumnIndex(ChatsColumns.ACCOUNT_ID)),
        //        cursor.getString(cursor.getColumnIndex(ChatsColumns.FOREIGN_ACCOUNT_LOGIN)),
        //        cursor.getString(cursor.getColumnIndex(ChatsColumns.FOREIGN_ACCOUNT_PASSWORD)),
        //        cursor.getString(cursor.getColumnIndex(ChatsColumns.FOREIGN_ACCOUNT_HOST)),
        //        cursor.getInt(cursor.getColumnIndex(ChatsColumns.FOREIGN_ACCOUNT_PORT)),
        //        cursor.getInt(cursor.getColumnIndex(ChatsColumns.FOREIGN_ACCOUNT_DISABLE)) == 1,
        //        keyPair);

        return new Chat()
                .setId(cursor.getInt(cursor.getColumnIndex(ChatsColumns._ID)))
                .setAccountId(cursor.getInt(cursor.getColumnIndex(ChatsColumns.ACCOUNT_ID)))
                .setDestination(cursor.getString(cursor.getColumnIndex(ChatsColumns.DESTINATION)))
                .setGroupChat(cursor.getInt(cursor.getColumnIndex(ChatsColumns.IS_GROUP_CHAT)) == 1)
                .setHidden(cursor.getInt(cursor.getColumnIndex(ChatsColumns.HIDDEN)) == 1)
                .setTitle(cursor.getString(cursor.getColumnIndex(ChatsColumns.TITLE)))
                .setUnreadCount(cursor.getInt(cursor.getColumnIndex(ChatsColumns.UNREAD_COUNT)))
                .setInterlocutorId(user.getId())
                .setInterlocutor(user)
                .setLastMessageText(cursor.getString(cursor.getColumnIndex(ChatsColumns.LAST_MESSAGE_TEXT)))
                .setLastMessageTime(cursor.getLong(cursor.getColumnIndex(ChatsColumns.LAST_MESSAGE_TIME)))
                .setLastMessageOut(cursor.getInt(cursor.getColumnIndex(ChatsColumns.LAST_MESSAGE_OUT)) == 1)
                .setLastMessageType(cursor.getInt(cursor.getColumnIndex(ChatsColumns.LAST_MESSAGE_TYPE)));
    }

    private void notifyAboutChatDelete(int id){
        deletionPublisher.onNext(id);
    }

    private void notifyAboutChatCreated(@NonNull Chat chat) {
        creationPublisher.onNext(chat);
    }

    private void notifyAboutChatUpdate(@NonNull ChatUpdateModel update) {
        updatesPublisher.onNext(update);
    }
}