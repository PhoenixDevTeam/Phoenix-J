package biz.dealnote.xmpp.db.impl;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import biz.dealnote.xmpp.db.ChatContentProvider;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.db.columns.AccountsColumns;
import biz.dealnote.xmpp.db.exception.RecordDoesNotExistException;
import biz.dealnote.xmpp.db.interfaces.IAccountsRepository;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by ruslan.kolbasa on 01.11.2016.
 * phoenix_for_xmpp
 */
public class AccountsRepository extends AbsRepository implements IAccountsRepository {

    private PublishSubject<Integer> deletionPublisher;
    private PublishSubject<Pair<Integer, String>> passwordChangePublisher;

    public AccountsRepository(Repositories repositories) {
        super(repositories);
        this.deletionPublisher = PublishSubject.create();
        this.passwordChangePublisher = PublishSubject.create();
    }

    @CheckResult
    @Override
    public Single<List<Account>> getAllActive() {
        return Single.create(emitter -> {
            Cursor cursor = getContext().getContentResolver().query(ChatContentProvider.ACCOUNTS_CONTENT_URI,
                    null, AccountsColumns.DISABLE + " = ?", new String[]{SQL_FALSE}, null);

            ArrayList<Account> accounts = new ArrayList<>(cursor == null ? 0 : cursor.getCount());

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed()) {
                        break;
                    }

                    Account account = map(cursor);
                    accounts.add(account);
                }

                cursor.close();
            }

            emitter.onSuccess(accounts);
        });
    }

    @Override
    public Maybe<Account> findById(int id) {
        return Maybe.create(e -> {
            Cursor cursor = getContentResolver().query(ChatContentProvider.ACCOUNTS_CONTENT_URI,
                    null, AccountsColumns._ID + " = ?", new String[]{String.valueOf(id)}, null);

            Account account = null;
            if(cursor != null){
                if(cursor.moveToNext()){
                    account = map(cursor);
                }

                cursor.close();
            }

            if(account != null){
                e.onSuccess(account);
            }

            e.onComplete();
        });
    }

    @Override
    public Completable deleteById(int id) {
        return Completable.create(e -> {
            int count = getContentResolver().delete(ChatContentProvider.ACCOUNTS_CONTENT_URI,
                    AccountsColumns._ID + " = ?", new String[]{String.valueOf(id)});
            if (count == 0) {
                e.onError(new RecordDoesNotExistException());
            } else {
                notifyAboutAccountDelete(id);
                e.onComplete();
            }
        });
    }

    private void notifyAboutAccountDelete(int accountId) {
        deletionPublisher.onNext(accountId);
    }

    @Override
    public Observable<Integer> observeDeletion() {
        return deletionPublisher;
    }

    @Override
    public Completable changePassword(int id, String pass) {
        return Completable.fromAction(() -> {
            ContentValues cv = new ContentValues();
            cv.put(AccountsColumns.PASSWORD, pass);
            int count = getContentResolver().update(ChatContentProvider.ACCOUNTS_CONTENT_URI, cv,
                    AccountsColumns._ID + " = ?", new String[]{String.valueOf(id)});

            if(count == 0){
                throw new RecordDoesNotExistException();
            } else {
                passwordChangePublisher.onNext(Pair.create(id, pass));
            }
        });
    }

    @Override
    public Observable<Pair<Integer, String>> observePasswordChanges() {
        return passwordChangePublisher;
    }

    public static Account map(Cursor cursor) throws InvalidKeySpecException, NoSuchAlgorithmException {
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

    @Nullable
    private static KeyPair restoreDSAKeyPairFrom(@Nullable byte[] pubKeyBytes, @Nullable byte[] privKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (privKeyBytes == null || pubKeyBytes == null) {
            return null;
        }

        KeyFactory pubKeyFactory = KeyFactory.getInstance("DSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKeyBytes);
        PublicKey publicKey = pubKeyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
        PrivateKey privateKey = pubKeyFactory.generatePrivate(privateKeySpec);
        return new KeyPair(publicKey, privateKey);
    }
}