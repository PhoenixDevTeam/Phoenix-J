package biz.dealnote.xmpp.db.interfaces;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import biz.dealnote.xmpp.model.Chat;
import biz.dealnote.xmpp.model.ChatUpdateModel;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
public interface IChatsRepository {

    Observable<Chat> observeChatCreation();

    Observable<ChatUpdateModel> observeChatUpdate();

    Observable<Integer> observeChatDeletion();

    Single<Integer> updateChatHeaderWith(@NonNull IChatUpdateRequest request);

    Single<Integer> getUnreadCount(int chatId);

    Single<List<Chat>> getAll(boolean includeHidden);

    Maybe<Integer> findByDestination(int accountId, String destination);

    Maybe<Chat> findById(int id);

    Completable setUnreadCount(int chatId, int unreadCount);

    Completable setChatHidden(int chatId, boolean hidden);

    Completable removeChatWithMessages(int chatId);

    interface IChatUpdateRequest {

        @Nullable
        Integer getChatId();

        String getDestination();

        int getAccountId();

        boolean isGroupChat();

        int getInterlocutorId();

        boolean isLastMessageOut();

        long getLastMessageTime();

        String getLastMessageBody();

        int getLastMessageType();

        boolean isLastMessageRead();
    }
}