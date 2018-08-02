package biz.dealnote.xmpp.db.impl

import android.content.ContentValues
import android.database.Cursor
import biz.dealnote.xmpp.db.DBHelper
import biz.dealnote.xmpp.db.Repositories
import biz.dealnote.xmpp.db.columns.AccountsColumns
import biz.dealnote.xmpp.db.columns.ContactsColumns
import biz.dealnote.xmpp.db.columns.ContactsColumns.*
import biz.dealnote.xmpp.db.columns.RosterColumns
import biz.dealnote.xmpp.db.interfaces.IUsersStorage
import biz.dealnote.xmpp.model.AccountId
import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.util.Optional
import biz.dealnote.xmpp.util.Utils
import biz.dealnote.xmpp.util.getInt
import biz.dealnote.xmpp.util.query
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import java.util.*

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
class UsersStorage(repositories: Repositories) : AbsRepository(repositories), IUsersStorage {

    private val addingPublisher: PublishProcessor<User> = PublishProcessor.create()
    private val updatesPublisher: PublishProcessor<User> = PublishProcessor.create()
    private val dbHelper: DBHelper = DBHelper.getInstance(repositories)

    override fun findById(id: Int): Single<Optional<User?>> {
        return Single.create { e ->
            synchronized(contactLock) {
                val cursor = dbHelper.readableDatabase.query(ContactsColumns.TABLENAME, columns, "$_ID = ?", arrayOf(id.toString()))

                var user: User? = null
                if (cursor != null) {
                    if (cursor.moveToNext()) {
                        user = map(cursor)
                    }

                    cursor.close()
                }

                e.onSuccess(Optional.wrap(user))
            }
        }
    }

    private val columns = arrayOf(_ID,
            JID,
            FIRST_NAME,
            LAST_NAME,
            MIDDLE_NAME,
            PREFIX,
            SUFFIX,
            EMAIL_HOME,
            EMAIL_WORK,
            ORGANIZATION,
            ORGANIZATION_UNIT,
            PHOTO_MIME_TYPE,
            PHOTO_HASH,
            PHOTO
    )

    override fun findByJid(jid: String): Single<Optional<User?>> {
        return Single.create { e ->
            synchronized(contactLock) {
                val cursor = dbHelper.readableDatabase.query(TABLENAME, columns, "$JID LIKE ?", arrayOf(jid))

                var user: User? = null
                if (cursor != null) {
                    if (cursor.moveToNext()) {
                        user = map(cursor)
                    }

                    cursor.close()
                }

                e.onSuccess(Optional.wrap(user))
            }
        }
    }

    private fun findIdByBareJid(jid: String): Int? {
        synchronized(contactLock) {
            val cursor = dbHelper.readableDatabase.query(ContactsColumns.TABLENAME, arrayOf(_ID), "$JID LIKE ?", arrayOf(jid))

            try {
                return if (cursor != null && cursor.moveToNext()) {
                    cursor.getInt(cursor.getColumnIndex(_ID))
                } else {
                    null
                }
            } finally {
                Utils.safelyCloseCursor(cursor)
            }
        }
    }

    private val contactLock: Any = Any()

    override fun upsert(bareJid: String, vCard: VCard): Completable {
        return Completable.create { e ->
            synchronized(contactLock) {
                val jid = prepareBareJid(bareJid)

                val id = findIdByBareJid(jid)

                val cv = ContentValues()
                cv.put(FIRST_NAME, vCard.firstName)
                cv.put(LAST_NAME, vCard.lastName)
                cv.put(MIDDLE_NAME, vCard.middleName)
                cv.put(PREFIX, vCard.prefix)
                cv.put(SUFFIX, vCard.suffix)
                cv.put(EMAIL_HOME, vCard.emailHome)
                cv.put(EMAIL_WORK, vCard.emailWork)
                cv.put(ORGANIZATION, vCard.organization)
                cv.put(ORGANIZATION_UNIT, vCard.organizationUnit)
                cv.put(PHOTO_MIME_TYPE, vCard.avatarMimeType)
                cv.put(PHOTO_HASH, vCard.avatarHash)
                cv.put(PHOTO, vCard.avatar)

                val contact = User()
                        .setJid(jid)
                        .setFirstName(vCard.firstName)
                        .setLastName(vCard.lastName)
                        .setMiddleName(vCard.middleName)
                        .setPrefix(vCard.prefix)
                        .setSuffix(vCard.suffix)
                        .setEmailHome(vCard.emailHome)
                        .setEmailWork(vCard.emailWork)
                        .setOrganization(vCard.organization)
                        .setOrganizationUnit(vCard.organizationUnit)
                        .setPhotoMimeType(vCard.avatarMimeType)
                        .setPhotoHash(vCard.avatarHash)

                val db = dbHelper.writableDatabase

                if (id != null) {
                    contact.id = id
                    db.update(TABLENAME, cv, "$_ID = ?", arrayOf(id.toString()))
                    updatesPublisher.onNext(contact)
                } else {
                    contact.id = db.insert(TABLENAME, null, cv).toInt()
                    addingPublisher.onNext(contact)
                }

                e.onComplete()
            }
        }
    }

    private fun insert(bareJid: String): Single<User> {
        return Single.fromCallable {
            val cv = ContentValues()
            cv.put(JID, bareJid)

            synchronized(contactLock) {
                val id = dbHelper.writableDatabase.insert(TABLENAME, null, cv).toInt()
                val contact = User()
                        .setJid(bareJid)
                        .setId(id)
                addingPublisher.onNext(contact)
                contact
            }
        }
    }

    override fun getContactIdPutIfNotExist(bareJid: String): Single<Int> {
        return Single.create { e ->
            synchronized(contactLock) {
                val jid = prepareBareJid(bareJid)
                var id = findIdByBareJid(jid)

                if (id != null) {
                    e.onSuccess(id)
                } else {
                    val cv = ContentValues()
                    cv.put(JID, jid)

                    id = dbHelper.writableDatabase.insert(TABLENAME, null, cv).toInt()

                    val contact = User()
                            .setJid(jid)
                            .setId(id)

                    addingPublisher.onNext(contact)
                    e.onSuccess(id)
                }
            }
        }
    }

    override fun getByJid(bareJid: String): Single<User> {
        val preparedJid = prepareBareJid(bareJid)

        return findByJid(preparedJid)
                .flatMap { found ->
                    if (found.nonEmpty()) {
                        Single.just<User>(found.get())
                    } else {
                        insert(preparedJid)
                    }
                }
    }

    override fun observeAdding(): Flowable<User> {
        return addingPublisher.onBackpressureBuffer()
    }

    override fun observeUpdates(): Flowable<User> {
        return updatesPublisher.onBackpressureBuffer()
    }

    override fun findPhotoByHash(hash: String): ByteArray? {
        val projection = arrayOf(PHOTO)

        val cursor = dbHelper.readableDatabase.query(ContactsColumns.TABLENAME, projection, "$PHOTO_HASH LIKE ?", arrayOf(hash))

        cursor?.run {
            var photo: ByteArray? = null
            if (moveToNext()) {
                photo = getBlob(getColumnIndex(PHOTO))
            }

            close()
            return photo
        } ?: run {
            return null
        }
    }

    private fun map(cursor: Cursor): User {
        return User()
                .setId(cursor.getInt(cursor.getColumnIndex(_ID)))
                .setJid(cursor.getString(cursor.getColumnIndex(JID)))
                .setFirstName(cursor.getString(cursor.getColumnIndex(FIRST_NAME)))
                .setLastName(cursor.getString(cursor.getColumnIndex(LAST_NAME)))
                .setMiddleName(cursor.getString(cursor.getColumnIndex(MIDDLE_NAME)))
                .setPrefix(cursor.getString(cursor.getColumnIndex(PREFIX)))
                .setSuffix(cursor.getString(cursor.getColumnIndex(SUFFIX)))
                .setEmailHome(cursor.getString(cursor.getColumnIndex(EMAIL_HOME)))
                .setEmailWork(cursor.getString(cursor.getColumnIndex(EMAIL_WORK)))
                .setOrganization(cursor.getString(cursor.getColumnIndex(ORGANIZATION)))
                .setOrganizationUnit(cursor.getString(cursor.getColumnIndex(ORGANIZATION_UNIT)))
                .setPhotoMimeType(cursor.getString(cursor.getColumnIndex(PHOTO_MIME_TYPE)))
                .setPhotoHash(cursor.getString(cursor.getColumnIndex(PHOTO_HASH)))
    }

    override fun getContacts(): Single<List<Contact>> {
        return Single.create { emitter ->
            val table = RosterColumns.TABLENAME +
                    " LEFT OUTER JOIN " + ContactsColumns.TABLENAME +
                    " ON " + RosterColumns.TABLENAME + "." + RosterColumns.CONTACT_ID + " = " + ContactsColumns.TABLENAME + "." + ContactsColumns._ID +
                    " LEFT OUTER JOIN " + AccountsColumns.TABLENAME +
                    " ON " + RosterColumns.TABLENAME + "." + RosterColumns.CONTACT_ID + " = " + AccountsColumns.TABLENAME + "." + AccountsColumns._ID

            val columns: Array<String> = arrayOf(
                    RosterColumns.ACCOUNT_ID,
                    RosterColumns.TABLENAME + "." + RosterColumns.JID + " AS user_jid",
                    RosterColumns.RESOURCE,
                    RosterColumns.CONTACT_ID,
                    RosterColumns.FLAGS,
                    RosterColumns.AVAILABLE_RECEIVE_MESSAGES,
                    RosterColumns.IS_AWAY,
                    RosterColumns.PRESENSE_MODE,
                    RosterColumns.PRESENSE_TYPE,
                    RosterColumns.PRESENSE_STATUS,
                    RosterColumns.TYPE,
                    RosterColumns.NICK,
                    RosterColumns.PRIORITY,
                    ContactsColumns.FIRST_NAME,
                    ContactsColumns.LAST_NAME,
                    ContactsColumns.MIDDLE_NAME,
                    ContactsColumns.PREFIX,
                    ContactsColumns.SUFFIX,
                    ContactsColumns.EMAIL_HOME,
                    ContactsColumns.EMAIL_WORK,
                    ContactsColumns.ORGANIZATION,
                    ContactsColumns.ORGANIZATION_UNIT,
                    ContactsColumns.PHOTO_MIME_TYPE,
                    ContactsColumns.PHOTO_HASH,
                    AccountsColumns.TABLENAME + "." + AccountsColumns.LOGIN + "AS account_jid"
            )

            val cursor = dbHelper.readableDatabase.query(table, columns, null, null, null, null, null)
            val entries = ArrayList<Contact>()
            while (cursor.moveToNext()) {
                if(emitter.isDisposed) break

                entries.add(mapContact(cursor))
            }

            cursor.close()
            emitter.onSuccess(entries)
        }
    }

    private fun mapContact(cursor: Cursor): Contact {
        val jid = cursor.getString(cursor.getColumnIndex("user_jid"))

        val user = User()
                .setId(cursor.getInt(cursor.getColumnIndex(RosterColumns.CONTACT_ID)))
                .setJid(jid)
                .setFirstName(cursor.getString(cursor.getColumnIndex(ContactsColumns.FIRST_NAME)))
                .setLastName(cursor.getString(cursor.getColumnIndex(ContactsColumns.LAST_NAME)))
                .setMiddleName(cursor.getString(cursor.getColumnIndex(ContactsColumns.MIDDLE_NAME)))
                .setPrefix(cursor.getString(cursor.getColumnIndex(ContactsColumns.PREFIX)))
                .setSuffix(cursor.getString(cursor.getColumnIndex(ContactsColumns.SUFFIX)))
                .setEmailHome(cursor.getString(cursor.getColumnIndex(ContactsColumns.EMAIL_HOME)))
                .setEmailWork(cursor.getString(cursor.getColumnIndex(ContactsColumns.EMAIL_WORK)))
                .setOrganization(cursor.getString(cursor.getColumnIndex(ContactsColumns.ORGANIZATION)))
                .setOrganizationUnit(cursor.getString(cursor.getColumnIndex(ContactsColumns.ORGANIZATION_UNIT)))
                .setPhotoMimeType(cursor.getString(cursor.getColumnIndex(ContactsColumns.PHOTO_MIME_TYPE)))
                .setPhotoHash(cursor.getString(cursor.getColumnIndex(ContactsColumns.PHOTO_HASH)))

        val entry = Contact()

        entry.id = cursor.getInt(cursor.getColumnIndex(RosterColumns._ID))

        entry.accountId = AccountId(
                cursor.getInt(cursor.getColumnIndex(RosterColumns.ACCOUNT_ID)),
                cursor.getString(cursor.getColumnIndex("account_jid"))
        )

        entry.jid = jid
        entry.user = user
        entry.flags = cursor.getInt(cursor.getColumnIndex(RosterColumns.FLAGS))
        entry.availableToReceiveMessages = cursor.getInt(cursor.getColumnIndex(RosterColumns.AVAILABLE_RECEIVE_MESSAGES)) == 1
        entry.away = cursor.getInt(cursor.getColumnIndex(RosterColumns.IS_AWAY)) == 1

        entry.presenceMode = cursor.getInt(RosterColumns.PRESENSE_MODE)
        entry.presenceType = cursor.getInt(RosterColumns.PRESENSE_TYPE)
        entry.presenceStatus = cursor.getString(cursor.getColumnIndex(RosterColumns.PRESENSE_STATUS))

        entry.type = cursor.getInt(RosterColumns.TYPE)
        entry.nick = cursor.getString(cursor.getColumnIndex(RosterColumns.NICK))
        entry.priority = cursor.getInt(cursor.getColumnIndex(RosterColumns.PRIORITY))
        return entry
    }

    private fun prepareBareJid(input: String): String {
        return Utils.getBareJid(input).trim { it <= ' ' }
    }
}