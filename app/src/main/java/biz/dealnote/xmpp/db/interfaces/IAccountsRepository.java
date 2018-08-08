package biz.dealnote.xmpp.db.interfaces;

import android.support.annotation.CheckResult;

import java.util.List;

import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Created by ruslan.kolbasa on 01.11.2016.
 * phoenix_for_xmpp
 */
public interface IAccountsRepository {

    @CheckResult
    Single<List<Account>> getAllActive();

    @CheckResult
    Maybe<Account> findById(int id);

    Single<Account> getById(int id);

    @CheckResult
    Completable deleteById(int id);

    Observable<Integer> observeDeletion();

    Completable changePassword(int id, String pass);

    Observable<Pair<Integer, String>> observePasswordChanges();
}
