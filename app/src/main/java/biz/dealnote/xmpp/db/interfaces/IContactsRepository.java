package biz.dealnote.xmpp.db.interfaces;

import android.support.annotation.NonNull;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.util.Optional;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
public interface IContactsRepository {

    Single<Optional<Contact>> findById(int id);

    Single<Optional<Contact>> findByJid(@NonNull String jid);

    Completable upsert(String bareJid, VCard vCard);

    Single<Integer> getContactIdPutIfNotExist(String bareJid);

    Single<Contact> getByJid(String bareJid);

    Flowable<Contact> observeAdding();

    Flowable<Contact> observeUpdates();

    byte[] findPhotoByHash(String hash);
}