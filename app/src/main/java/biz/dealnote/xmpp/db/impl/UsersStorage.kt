package biz.dealnote.xmpp.db.impl

import android.content.ContentValues
import android.database.Cursor
import biz.dealnote.xmpp.db.DBHelper
import biz.dealnote.xmpp.db.Repositories
import biz.dealnote.xmpp.db.columns.AccountsColumns
import biz.dealnote.xmpp.db.columns.UsersColumns
import biz.dealnote.xmpp.db.columns.UsersColumns.*
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
import org.jivesoftware.smack.roster.RosterEntry
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
                val cursor = dbHelper.readableDatabase.query(UsersColumns.TABLENAME, columns, "$_ID = ?", arrayOf(id.toString()))

                val user: User? = if (cursor.moveToNext()) map(cursor) else null
                cursor.close()

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

                val user: User? = if(cursor.moveToNext()) map(cursor) else null
                cursor.close()

                e.onSuccess(Optional.wrap(user))
            }
        }
    }

    override fun putContacts(accountId: Int, contacts: Collection<RosterEntry>): Completable {
        if(contacts.isEmpty()){
            return Completable.complete()
        }

        return Completable.create {emitter ->
            synchronized(contactLock){
                val db = dbHelper.writableDatabase

                db.beginTransaction()

                try {
                    for(contact in contacts){
                        val jid = contact.jid.asBareJid().toString()

                        val cv = ContentValues()
                        cv.put(RosterColumns.TYPE, Contact.apiTypeToAppType(contact.type))
                        cv.put(RosterColumns.NICK, contact.name)

                        val rows = db.update(RosterColumns.TABLENAME, cv,
                                RosterColumns.ACCOUNT_ID + " = ? AND " + RosterColumns.JID + " LIKE ?", arrayOf(accountId.toString(), jid))

                        if(rows == 0){
                            val userId = obtainUserId(jid)

                            cv.put(RosterColumns.ACCOUNT_ID, accountId)
                            cv.put(RosterColumns.JID, jid)
                            cv.put(RosterColumns.USER_ID, userId)

                            db.insert(RosterColumns.TABLENAME, null, cv)
                        }
                    }

                    db.setTransactionSuccessful()
                    db.endTransaction()
                    emitter.onComplete()
                } catch (e: Exception){
                    db.endTransaction()
                    emitter.onError(e)
                }
            }
        }
    }

    private fun obtainUserId(jid: String): Int {
        val cursor = dbHelper.readableDatabase.query(UsersColumns.TABLENAME, arrayOf(UsersColumns._ID), "$JID LIKE ?", arrayOf(jid))

        return cursor.use {
            if(it.moveToNext()){
                it.getInt(it.getColumnIndex(UsersColumns._ID))
            } else {
                val cv = ContentValues()
                cv.put(JID, jid)
                dbHelper.writableDatabase.insert(TABLENAME, null, cv).toInt()
            }
        }
    }

    private fun findIdByBareJid(jid: String): Int? {
        synchronized(contactLock) {
            val cursor = dbHelper.readableDatabase.query(UsersColumns.TABLENAME, arrayOf(_ID), "$JID LIKE ?", arrayOf(jid))

            try {
                return if (cursor.moveToNext()) cursor.getInt(cursor.getColumnIndex(_ID)) else null
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

        val cursor = dbHelper.readableDatabase.query(UsersColumns.TABLENAME, projection, "$PHOTO_HASH LIKE ?", arrayOf(hash))

        cursor.run {
            var photo: ByteArray? = null
            if (moveToNext()) {
                photo = getBlob(getColumnIndex(PHOTO))
            }

            close()
            return photo
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
                    " LEFT OUTER JOIN " + UsersColumns.TABLENAME +
                    " ON " + RosterColumns.TABLENAME + "." + RosterColumns.USER_ID + " = " + UsersColumns.TABLENAME + "." + UsersColumns._ID +
                    " LEFT OUTER JOIN " + AccountsColumns.TABLENAME +
                    " ON " + RosterColumns.TABLENAME + "." + RosterColumns.ACCOUNT_ID + " = " + AccountsColumns.TABLENAME + "." + AccountsColumns._ID

            val columns: Array<String> = arrayOf(
                    RosterColumns.TABLENAME + "." + RosterColumns._ID + " AS contact_id",
                    RosterColumns.ACCOUNT_ID,
                    RosterColumns.TABLENAME + "." + RosterColumns.JID + " AS user_jid",
                    RosterColumns.RESOURCE,
                    RosterColumns.USER_ID,
                    RosterColumns.FLAGS,
                    RosterColumns.AVAILABLE_RECEIVE_MESSAGES,
                    RosterColumns.IS_AWAY,
                    RosterColumns.PRESENSE_MODE,
                    RosterColumns.PRESENSE_TYPE,
                    RosterColumns.PRESENSE_STATUS,
                    RosterColumns.TYPE,
                    RosterColumns.NICK,
                    RosterColumns.PRIORITY,
                    UsersColumns.FIRST_NAME,
                    UsersColumns.LAST_NAME,
                    UsersColumns.MIDDLE_NAME,
                    UsersColumns.PREFIX,
                    UsersColumns.SUFFIX,
                    UsersColumns.EMAIL_HOME,
                    UsersColumns.EMAIL_WORK,
                    UsersColumns.ORGANIZATION,
                    UsersColumns.ORGANIZATION_UNIT,
                    UsersColumns.PHOTO_MIME_TYPE,
                    UsersColumns.PHOTO_HASH,
                    AccountsColumns.TABLENAME + "." + AccountsColumns.LOGIN + " AS account_jid"
            )

            val cursor = dbHelper.readableDatabase.query(table, columns)
            val entries = ArrayList<Contact>()
            while (cursor.moveToNext()) {
                if (emitter.isDisposed) break

                entries.add(mapContact(cursor))
            }

            cursor.close()
            emitter.onSuccess(entries)
        }
    }

    private fun mapContact(cursor: Cursor): Contact {
        val user = User().apply {
            id = cursor.getInt(cursor.getColumnIndex(RosterColumns.USER_ID))
            jid = cursor.getString(cursor.getColumnIndex("user_jid"))
            firstName = cursor.getString(cursor.getColumnIndex(UsersColumns.FIRST_NAME))
            lastName = cursor.getString(cursor.getColumnIndex(UsersColumns.LAST_NAME))
            middleName = cursor.getString(cursor.getColumnIndex(UsersColumns.MIDDLE_NAME))
            prefix = cursor.getString(cursor.getColumnIndex(UsersColumns.PREFIX))
            suffix = cursor.getString(cursor.getColumnIndex(UsersColumns.SUFFIX))
            emailHome = cursor.getString(cursor.getColumnIndex(UsersColumns.EMAIL_HOME))
            emailWork = cursor.getString(cursor.getColumnIndex(UsersColumns.EMAIL_WORK))
            organization = cursor.getString(cursor.getColumnIndex(UsersColumns.ORGANIZATION))
            organizationUnit = cursor.getString(cursor.getColumnIndex(UsersColumns.ORGANIZATION_UNIT))
            photoMimeType = cursor.getString(cursor.getColumnIndex(UsersColumns.PHOTO_MIME_TYPE))
            photoHash = cursor.getString(cursor.getColumnIndex(UsersColumns.PHOTO_HASH))
        }

        val entry = Contact()

        entry.id = cursor.getInt(cursor.getColumnIndex("contact_id"))

        entry.accountId = AccountId(
                cursor.getInt(cursor.getColumnIndex(RosterColumns.ACCOUNT_ID)),
                cursor.getString(cursor.getColumnIndex("account_jid"))
        )

        entry.jid = user.jid
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