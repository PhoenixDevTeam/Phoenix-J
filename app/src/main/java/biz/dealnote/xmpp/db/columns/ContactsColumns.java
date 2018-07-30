package biz.dealnote.xmpp.db.columns;

import android.provider.BaseColumns;

public class ContactsColumns implements BaseColumns {

    public static final String TABLENAME = "contacts";
    public static final String JID = "jid";

    // vcard fields
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String MIDDLE_NAME = "middle_name";
    public static final String PREFIX = "prefix";
    public static final String SUFFIX = "suffix";
    public static final String EMAIL_HOME = "email_home";
    public static final String EMAIL_WORK = "email_work";
    public static final String ORGANIZATION = "organization";
    public static final String ORGANIZATION_UNIT = "organization_unit";
    public static final String PHOTO_MIME_TYPE = "photo_mime_type";
    public static final String PHOTO_HASH = "photo_hash";
    public static final String PHOTO = "photo";
    public static final String FULL_ID = TABLENAME + "." + _ID;
    public static final String FULL_JID = TABLENAME + "." + JID;
    public static final String FULL_FIRST_NAME = TABLENAME + "." + FIRST_NAME;
    public static final String FULL_LAST_NAME = TABLENAME + "." + LAST_NAME;
    public static final String FULL_MIDDLE_NAME = TABLENAME + "." + MIDDLE_NAME;
    public static final String FULL_PREFIX = TABLENAME + "." + PREFIX;
    public static final String FULL_SUFFIX = TABLENAME + "." + SUFFIX;
    public static final String FULL_EMAIL_HOME = TABLENAME + "." + EMAIL_HOME;
    public static final String FULL_EMAIL_WORK = TABLENAME + "." + EMAIL_WORK;
    public static final String FULL_ORGANIZATION = TABLENAME + "." + ORGANIZATION;
    public static final String FULL_ORGANIZATION_UNIT = TABLENAME + "." + ORGANIZATION_UNIT;
    public static final String FULL_PHOTO_MIME_TYPE = TABLENAME + "." + PHOTO_MIME_TYPE;
    public static final String FULL_PHOTO_HASH = TABLENAME + "." + PHOTO_HASH;
    public static final String FULL_PHOTO = TABLENAME + "." + PHOTO;

    private ContactsColumns() {
    }
}