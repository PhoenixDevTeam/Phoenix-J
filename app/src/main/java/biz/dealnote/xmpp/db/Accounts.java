package biz.dealnote.xmpp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import biz.dealnote.xmpp.db.columns.AccountsColumns;
import biz.dealnote.xmpp.exception.AccountAlreadyExistException;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AccountContactPair;

public class Accounts {

    private static final String TAG = Accounts.class.getSimpleName();
    private static final String SQL_FALSE = "0";

    public static ArrayList<Account> getAll(Context context) {
        Cursor cursor = context.getContentResolver().query(ChatContentProvider.ACCOUNTS_CONTENT_URI, null, AccountsColumns.DISABLE + " = ?", new String[]{SQL_FALSE}, null);
        ArrayList<Account> accounts = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                accounts.add(map(cursor));
            }

            cursor.close();
        }

        return accounts;
    }

    public static void delete(Context context, int id) {
        context.getContentResolver().delete(ChatContentProvider.ACCOUNTS_CONTENT_URI,
                AccountsColumns._ID + " = ?", new String[]{String.valueOf(id)});
    }

    public static Account map(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(AccountsColumns._ID));
        String host = cursor.getString(cursor.getColumnIndex(AccountsColumns.HOST));
        int port = cursor.getInt(cursor.getColumnIndex(AccountsColumns.PORT));

        String login = cursor.getString(cursor.getColumnIndex(AccountsColumns.LOGIN));
        String password = cursor.getString(cursor.getColumnIndex(AccountsColumns.PASSWORD));
        boolean disable = cursor.getInt(cursor.getColumnIndex(AccountsColumns.DISABLE)) == 1;

        byte[] pubKeyBytes = cursor.getBlob(cursor.getColumnIndex(AccountsColumns.PUBLIC_KEY));
        byte[] privKeyBytes = cursor.getBlob(cursor.getColumnIndex(AccountsColumns.PRIVATE_KEY));

        KeyPair keyPair = restoreDSAKeyPairFrom(pubKeyBytes, privKeyBytes);
        return new Account(id, login, password, host, port, disable, keyPair);
    }

    public static KeyPair restoreDSAKeyPairFrom(byte[] pubKeyBytes, byte[] privKeyBytes) {
        if (privKeyBytes == null || pubKeyBytes == null) {
            return null;
        }

        try {
            KeyFactory pubKeyFactory = KeyFactory.getInstance("DSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKeyBytes);
            PublicKey publicKey = pubKeyFactory.generatePublic(publicKeySpec);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
            PrivateKey privateKey = pubKeyFactory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Unable to restore keys from DB, error: " + e.getMessage());
            return null;
        }
    }

    public static Account put(@NonNull Context context, @NonNull String login, @NonNull String password, @NonNull String host, int port, boolean disable, KeyPair keyPair) throws AccountAlreadyExistException {
        ContentValues cv = new ContentValues();
        cv.put(AccountsColumns.LOGIN, login.trim().toLowerCase());
        cv.put(AccountsColumns.PASSWORD, password);
        cv.put(AccountsColumns.PORT, port);
        cv.put(AccountsColumns.HOST, host);
        cv.put(AccountsColumns.DISABLE, disable);
        cv.put(AccountsColumns.PUBLIC_KEY, keyPair == null ? null : keyPair.getPublic().getEncoded());
        cv.put(AccountsColumns.PRIVATE_KEY, keyPair == null ? null : keyPair.getPrivate().getEncoded());

        Uri uri = context.getContentResolver().insert(ChatContentProvider.ACCOUNTS_CONTENT_URI, cv);

        int dbid = Integer.parseInt(uri.getPathSegments().get(1));

        if (dbid == -1) {
            throw new AccountAlreadyExistException("Account with server " + host + " and login " + login + " already exist");
        }

        return new Account(dbid, login, password, host, port, disable, keyPair);
    }

    public static ArrayList<AccountContactPair> getAllPairs(Context context) {
        ArrayList<Account> accounts = Accounts.getAll(context);
        ArrayList<AccountContactPair> data = new ArrayList<>(accounts.size());
        for (Account account : accounts) {
            AccountContactPair accountWithContact = new AccountContactPair(account);

            accountWithContact.user = Storages.getINSTANCE()
                    .getUsers()
                    .findByJid(account.buildBareJid())
                    .blockingGet()
                    .get();

            data.add(accountWithContact);
        }

        return data;
    }

    public static boolean hasActiveAccount(Context context) {
        Cursor cursor = context.getContentResolver().query(ChatContentProvider.ACCOUNTS_CONTENT_URI, null,
                AccountsColumns.DISABLE + " = ?", new String[]{SQL_FALSE}, null);
        boolean has = false;
        if (cursor != null) {
            has = cursor.getCount() > 0;
            cursor.close();
        }

        return has;
    }

    public static void enableAccount(Context context, int id, boolean enable) {
        ContentValues cv = new ContentValues();
        cv.put(AccountsColumns.DISABLE, !enable);
        context.getContentResolver().update(ChatContentProvider.ACCOUNTS_CONTENT_URI, cv,
                AccountsColumns._ID + " = ?", new String[]{String.valueOf(id)});
    }

    public static Account findByLogin(Context context, @NonNull String login, @NonNull String host, int port) {
        String normalLogin = login.trim();

        String where = AccountsColumns.LOGIN + " LIKE ? AND " + AccountsColumns.HOST + " LIKE ? AND " + AccountsColumns.PORT + " = ?";
        String[] args = {normalLogin, host, String.valueOf(port)};

        Cursor cursor = context.getContentResolver().query(ChatContentProvider.ACCOUNTS_CONTENT_URI, null, where, args, null);
        if (cursor == null) {
            return null;
        }

        Account account = null;

        if (cursor.moveToNext()) {
            account = map(cursor);
        }

        cursor.close();
        return account;
    }
}
