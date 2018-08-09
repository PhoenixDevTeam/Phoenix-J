package biz.dealnote.xmpp.db.interfaces;

import android.support.annotation.NonNull;

import java.util.List;
import java.util.Set;

import biz.dealnote.xmpp.model.MessageBuilder;
import biz.dealnote.xmpp.model.MessageCriteria;
import biz.dealnote.xmpp.model.MessageUpdate;
import biz.dealnote.xmpp.model.Msg;
import biz.dealnote.xmpp.util.AvatarResorce;
import biz.dealnote.xmpp.util.Optional;
import biz.dealnote.xmpp.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
public interface IMessagesStorage {
    Observable<Msg> createAddMessageObservable();

    Observable<Pair<Integer, MessageUpdate>> createMessageUpdateObservable();

    Observable<Pair<Integer, Set<Integer>>> createMessageDeleteObservable();

    Single<Msg> saveMessage(@NonNull MessageBuilder builder);

    Single<Boolean> hasMessageWithStanza(int accountId, @NonNull String destination, String stanzaId);

    Completable updateMessage(int messageId, @NonNull MessageUpdate update);

    Maybe<Msg> findLastMessage(int chatId);

    Single<Optional<Msg>> firstWithStatus(int status);

    Completable updateStatus(int chatId, int from, int to);

    Single<Boolean> deleteMessages(int chatId, Set<Integer> mids);

    Single<Pair<List<Msg>, List<AvatarResorce.Entry>>> load(MessageCriteria criteria);
}