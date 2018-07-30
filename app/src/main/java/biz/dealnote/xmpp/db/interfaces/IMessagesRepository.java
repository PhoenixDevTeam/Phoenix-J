package biz.dealnote.xmpp.db.interfaces;

import android.support.annotation.NonNull;

import java.util.List;
import java.util.Set;

import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.model.MessageBuilder;
import biz.dealnote.xmpp.model.MessageCriteria;
import biz.dealnote.xmpp.model.MessageUpdate;
import biz.dealnote.xmpp.util.AvatarResorce;
import biz.dealnote.xmpp.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
public interface IMessagesRepository {
    Observable<AppMessage> createAddMessageObservable();

    Observable<Pair<Integer, MessageUpdate>> createMessageUpdateObservable();

    Observable<Pair<Integer, Set<Integer>>> createMessageDeleteObservable();

    Single<AppMessage> saveMessage(@NonNull MessageBuilder builder);

    Single<Boolean> hasMessageWithStanza(int accountId, @NonNull String destination, String stanzaId);

    Completable updateMessage(int messageId, @NonNull MessageUpdate update);

    Maybe<AppMessage> findLastMessage(int chatId);

    Single<Boolean> deleteMessages(int chatId, Set<Integer> mids);

    Single<Pair<List<AppMessage>, List<AvatarResorce.Entry>>> load(MessageCriteria criteria);
}