package biz.dealnote.xmpp.db.interfaces

import biz.dealnote.xmpp.db.entity.ContactEntity
import biz.dealnote.xmpp.db.entity.UserEntity
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.util.Optional
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import org.jivesoftware.smack.roster.RosterEntry
import org.jivesoftware.smackx.vcardtemp.packet.VCard

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
interface IUsersStorage {
    fun findById(id: Int): Single<Optional<User?>>

    fun findByJid(jid: String): Single<Optional<User?>>

    fun upsert(bareJid: String, vCard: VCard?): Single<UserEntity>

    fun putContacts(accountId: Int, contacts: Collection<RosterEntry>): Completable

    fun getContactIdPutIfNotExist(bareJid: String): Single<Int>

    fun getByJid(bareJid: String): Single<User>

    fun observeAdding(): Flowable<User>

    fun observeUpdates(): Flowable<User>

    fun findPhotoByHash(hash: String): ByteArray?

    fun getContacts(): Single<List<ContactEntity>>

    fun delete(account: Int, jids: List<String>): Completable
}