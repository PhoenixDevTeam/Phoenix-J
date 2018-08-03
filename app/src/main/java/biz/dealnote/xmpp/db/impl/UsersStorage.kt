package biz.dealnote.xmpp.db.impl

import android.content.ContentValues
import android.database.Cursor
import biz.dealnote.xmpp.db.DBHelper
import biz.dealnote.xmpp.db.Repositories
import biz.dealnote.xmpp.db.columns.AccountsColumns
import biz.dealnote.xmpp.db.columns.RosterColumns
import biz.dealnote.xmpp.db.columns.UsersColumns
import biz.dealnote.xmpp.db.columns.UsersColumns.*
import biz.dealnote.xmpp.db.entity.ContactEntity
import biz.dealnote.xmpp.db.entity.UserEntity
import biz.dealnote.xmpp.db.interfaces.IUsersStorage
import biz.dealnote.xmpp.model.Contact
import biz.dealnote.xmpp.model.User
import biz.dealnote.xmpp.util.*
import biz.dealnote.xmpp.util.Optional
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

                val user: User? = if (cursor.moveToNext()) map(cursor) else null
                cursor.close()

                e.onSuccess(Optional.wrap(user))
            }
        }
    }

    override fun putContacts(accountId: Int, contacts: Collection<RosterEntry>): Completable {
        if (contacts.isEmpty()) {
            return Completable.complete()
        }

        return Completable.create { emitter ->
            synchronized(contactLock) {
                val start = System.currentTimeMillis()
                val db = dbHelper.writableDatabase

                db.beginTransaction()

                try {
                    for (contact in contacts) {
                        val jid = contact.jid.asBareJid().toString()

                        val cv = ContentValues()
                        cv.put(RosterColumns.TYPE, Contact.apiTypeToAppType(contact.type))
                        cv.put(RosterColumns.NICK, contact.name)

                        val rows = db.update(RosterColumns.TABLENAME, cv,
                                RosterColumns.ACCOUNT_ID + " = ? AND " + RosterColumns.JID + " LIKE ?", arrayOf(accountId.toString(), jid))

                        if (rows == 0) {
                            val userId = obtainUserId(jid)

                            cv.put(RosterColumns.ACCOUNT_ID, accountId)
                            cv.put(RosterColumns.JID, jid)
                            cv.put(RosterColumns.USER_ID, userId)

                            db.insert(RosterColumns.TABLENAME, null, cv)
                        }
                    }

                    db.setTransactionSuccessful()
                    db.endTransaction()
                    Logger.d("sqlite.putContacts", "time: ${System.currentTimeMillis() - start} ms, count: ${contacts.size}")
                    emitter.onComplete()
                } catch (e: Exception) {
                    db.endTransaction()
                    emitter.onError(e)
                }
            }
        }
    }

    private fun obtainUserId(jid: String): Int {
        val cursor = dbHelper.readableDatabase.query(UsersColumns.TABLENAME, arrayOf(UsersColumns._ID), "$JID LIKE ?", arrayOf(jid))

        return cursor.use {
            if (it.moveToNext()) {
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

    override fun upsert(bareJid: String, vCard: VCard): Single<UserEntity> {
        return Single.create { e ->
            synchronized(contactLock) {
                val start = System.currentTimeMillis()
                val jid = prepareBareJid(bareJid)
                val id = findIdByBareJid(jid)

                val cv = ContentValues()
                cv.put(UsersColumns.FIRST_NAME, vCard.firstName)
                cv.put(UsersColumns.LAST_NAME, vCard.lastName)
                cv.put(UsersColumns.MIDDLE_NAME, vCard.middleName)
                cv.put(UsersColumns.PREFIX, vCard.prefix)
                cv.put(UsersColumns.SUFFIX, vCard.suffix)
                cv.put(UsersColumns.EMAIL_HOME, vCard.emailHome)
                cv.put(UsersColumns.EMAIL_WORK, vCard.emailWork)
                cv.put(UsersColumns.ORGANIZATION, vCard.organization)
                cv.put(UsersColumns.ORGANIZATION_UNIT, vCard.organizationUnit)
                cv.put(UsersColumns.PHOTO_MIME_TYPE, vCard.avatarMimeType)
                cv.put(UsersColumns.PHOTO_HASH, vCard.avatarHash)
                cv.put(UsersColumns.PHOTO, vCard.avatar)
                cv.put(UsersColumns.LAST_VCARD_UPDATE_TIME, System.currentTimeMillis())

                val db = dbHelper.writableDatabase

                val entity: UserEntity

                if (id != null) {
                    entity = UserEntity(id, jid)
                    db.update(TABLENAME, cv, "$_ID = ?", arrayOf(id.toString()))
                } else {
                    entity = UserEntity(db.insert(TABLENAME, null, cv).toInt(), jid)
                }

                entity.apply {
                    firstName = vCard.firstName
                    lastName = vCard.lastName
                    middleName = vCard.middleName
                    prefix = vCard.prefix
                    suffix = vCard.suffix
                    emailHome = vCard.emailHome
                    emailWork = vCard.emailWork
                    organization = vCard.organization
                    organizationUnit = vCard.organizationUnit
                    photoMimeType = vCard.avatarMimeType
                    photoHash = vCard.avatarHash
                    lastVcardUpdateTime = System.currentTimeMillis()
                }

                Logger.d("sqlite.upsert", "time: " + (System.currentTimeMillis() - start) + " ms, id: " + entity.jid)
                e.onSuccess(entity)
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
        val cursor = dbHelper.readableDatabase.query(UsersColumns.TABLENAME, arrayOf(PHOTO), "$PHOTO_HASH LIKE ?", arrayOf(hash))
        cursor.use {
            return if (it.moveToNext()) {
                it.getBlob(it.getColumnIndex(PHOTO))
            } else {
                null
            }
        }
    }

    private fun map(cursor: Cursor): User {
        return User().apply {
            id = cursor.getInt(UsersColumns._ID)
            jid = cursor.getString(UsersColumns.JID)
            firstName = cursor.getString(UsersColumns.FIRST_NAME)
            lastName = cursor.getString(UsersColumns.LAST_NAME)
            middleName = cursor.getString(UsersColumns.MIDDLE_NAME)
            prefix = cursor.getString(UsersColumns.PREFIX)
            suffix = cursor.getString(UsersColumns.SUFFIX)
            emailHome = cursor.getString(UsersColumns.EMAIL_HOME)
            emailWork = cursor.getString(UsersColumns.EMAIL_WORK)
            organization = cursor.getString(UsersColumns.ORGANIZATION)
            organizationUnit = cursor.getString(UsersColumns.ORGANIZATION_UNIT)
            photoMimeType = cursor.getString(UsersColumns.PHOTO_MIME_TYPE)
            photoHash = cursor.getString(UsersColumns.PHOTO_HASH)
        }
    }

    override fun delete(account: Int, jids: List<String>): Completable {
        return Completable.create { emitter ->
            synchronized(contactLock) {
                val db = dbHelper.writableDatabase
                db.beginTransaction()

                try {
                    for (jid in jids) {
                        val where = RosterColumns.ACCOUNT_ID + " = ? AND " + RosterColumns.JID + " LIKE ?"
                        val args = arrayOf(account.toString(), jid)
                        dbHelper.writableDatabase.delete(RosterColumns.TABLENAME, where, args)
                    }
                    db.setTransactionSuccessful()
                    db.endTransaction()
                    emitter.onComplete()
                } catch (e: Exception) {
                    db.endTransaction()
                    emitter.onError(e)
                }
            }
        }
    }

    override fun getContacts(): Single<List<ContactEntity>> {
        return Single.create { emitter ->
            val start = System.currentTimeMillis()

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
                    UsersColumns.LAST_VCARD_UPDATE_TIME,
                    AccountsColumns.TABLENAME + "." + AccountsColumns.LOGIN + " AS account_jid"
            )

            val cursor = dbHelper.readableDatabase.query(table, columns)
            val entries = ArrayList<ContactEntity>()
            while (cursor.moveToNext()) {
                if (emitter.isDisposed) break

                entries.add(mapContact(cursor))
            }

            cursor.close()
            Logger.d("sqlite.getContacts", "time: " + (System.currentTimeMillis() - start) + " ms, count: " + entries.size)
            emitter.onSuccess(entries)
        }
    }

    private fun mapContact(cursor: Cursor): ContactEntity {
        val user = UserEntity(
                cursor.getInt(RosterColumns.USER_ID),
                cursor.getString("user_jid")!!
        )

        user.apply {
            firstName = cursor.getString(UsersColumns.FIRST_NAME)
            lastName = cursor.getString(UsersColumns.LAST_NAME)
            middleName = cursor.getString(UsersColumns.MIDDLE_NAME)
            prefix = cursor.getString(UsersColumns.PREFIX)
            suffix = cursor.getString(UsersColumns.SUFFIX)
            emailHome = cursor.getString(UsersColumns.EMAIL_HOME)
            emailWork = cursor.getString(UsersColumns.EMAIL_WORK)
            organization = cursor.getString(UsersColumns.ORGANIZATION)
            organizationUnit = cursor.getString(UsersColumns.ORGANIZATION_UNIT)
            photoMimeType = cursor.getString(UsersColumns.PHOTO_MIME_TYPE)
            photoHash = cursor.getString(UsersColumns.PHOTO_HASH)
            lastVcardUpdateTime = cursor.getLong(UsersColumns.LAST_VCARD_UPDATE_TIME)
        }

        val entity = ContactEntity(
                cursor.getInt("contact_id"),
                user.jid,
                cursor.getInt(RosterColumns.ACCOUNT_ID),
                cursor.getString("account_jid")!!, user
        )

        entity.apply {
            flags = cursor.getInt(RosterColumns.FLAGS)
            availableToReceiveMessages = cursor.getBoolean(RosterColumns.AVAILABLE_RECEIVE_MESSAGES)
            away = cursor.getBoolean(RosterColumns.IS_AWAY)

            presenceMode = cursor.getNullableInt(RosterColumns.PRESENSE_MODE)
            presenceType = cursor.getNullableInt(RosterColumns.PRESENSE_TYPE)
            presenceStatus = cursor.getString(RosterColumns.PRESENSE_STATUS)

            type = cursor.getNullableInt(RosterColumns.TYPE)
            nick = cursor.getString(RosterColumns.NICK)
            priority = cursor.getInt(RosterColumns.PRIORITY)
        }

        return entity
    }

    private fun prepareBareJid(input: String): String {
        return Utils.getBareJid(input).trim { it <= ' ' }
    }
}