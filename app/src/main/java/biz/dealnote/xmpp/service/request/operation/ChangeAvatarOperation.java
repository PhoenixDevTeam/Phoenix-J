package biz.dealnote.xmpp.service.request.operation;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.DBHelper;
import biz.dealnote.xmpp.db.columns.UsersColumns;
import biz.dealnote.xmpp.exception.CustomAppException;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

public class ChangeAvatarOperation extends AbsXmppOperation {

    private static final String TAG = ChangeAvatarOperation.class.getSimpleName();

    public static byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException,
            SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, InterruptedException {

        int accountId = request.getInt(Extra.ACCOUNT_ID);
        String myJid = request.getString(Extra.JID);
        Uri uri = (Uri) request.getParcelable(Extra.URI);

        AbstractXMPPConnection connection = assertConnectionFor(xmppContext, accountId);

        VCardManager manager = VCardManager.getInstanceFor(connection);

        VCard vCard;
        try {
            vCard = manager.loadVCard();
        } catch (XMPPException.XMPPErrorException e) {
            vCard = new VCard();
        }

        Log.d(TAG, "accountId: " + accountId + ", uri: " + uri + ", myJid: " + myJid + ", photohash: " + vCard.getAvatarHash());

        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            byte[] array = readBytes(is);
            vCard.setAvatar(array);

            manager.saveVCard(vCard);

            VCard updated = manager.loadVCard();
            Log.d(TAG, "updated hash: " + updated.getAvatarHash() + ", equals: " + Arrays.equals(array, updated.getAvatar()));

            ContentValues cv = new ContentValues();
            cv.put(UsersColumns.PHOTO, array);
            cv.put(UsersColumns.PHOTO_HASH, updated.getAvatarHash());
            cv.put(UsersColumns.PHOTO_MIME_TYPE, updated.getAvatarMimeType());

            String where = UsersColumns.JID + " LIKE ?";
            String[] args = {myJid.toLowerCase()};

            DBHelper.getInstance(context).getWritableDatabase().update(UsersColumns.TABLENAME, cv, where, args);

            Log.d(TAG, "Saved to DB");
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomAppException(e.getMessage());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ignored) {
            }
        }

        return null;
    }
}
