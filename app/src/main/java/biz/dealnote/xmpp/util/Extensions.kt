package biz.dealnote.xmpp.util

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.CountDownLatch

fun <T : Any> Single<T>.fromIOToMain(): Single<T> = subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

fun <T : Any> Flowable<T>.toMainThread(): Flowable<T> = observeOn(AndroidSchedulers.mainThread())

fun <T : Any> Observable<T>.toMainThread(): Observable<T> = observeOn(AndroidSchedulers.mainThread())

fun CountDownLatch.safelyWait(): Boolean {
    return try {
        this.await()
        true
    } catch (e: InterruptedException){
        false
    }
}

fun SQLiteDatabase.query(tablename: String, columns: Array<String>, where: String, args: Array<String>): Cursor? = query(tablename, columns, where, args, null, null, null)