package biz.dealnote.xmpp.util

import biz.dealnote.xmpp.BuildConfig
import biz.dealnote.xmpp.Injection
import io.reactivex.Completable
import io.reactivex.CompletableTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService

/**
 * Created by admin on 19.03.2017.
 * phoenix
 */
object RxUtils {

    private val DUMMY = Action { }

    fun dummy() = DUMMY

    fun <T> ignore(): Consumer<T> {
        return Consumer { t ->
            if (BuildConfig.DEBUG && t is Throwable) {
                (t as Throwable).printStackTrace()
            }
        }
    }

    fun <T> applySingleIOToMainSchedulers(): SingleTransformer<T, T> {
        return SingleTransformer { upstream ->
            upstream.subscribeOn(Schedulers.io())
                    .observeOn(Injection.provideMainThreadScheduler())
        }
    }

    fun applyCompletableIOToMainSchedulers(): CompletableTransformer {
        return CompletableTransformer { completable ->
            completable.subscribeOn(Schedulers.io())
                    .observeOn(Injection.provideMainThreadScheduler())
        }
    }

    fun comletableFromRunnable(executorService: ExecutorService, r: Runnable): Completable {
        return Completable.create { emitter ->
            val latch = CountDownLatch(1)
            emitter.setCancellable { latch.countDown() }

            var error: Throwable? = null
            var complete = false

            val runnable = Runnable {
                try {
                    r.run()
                    complete = true
                } catch (e: Exception) {
                    error = e
                }

                latch.countDown()
            }

            executorService.submit(runnable)

            if (latch.safelyWait() && !emitter.isDisposed) {
                error?.run {
                    emitter.onError(this)
                }

                if (complete) {
                    emitter.onComplete()
                }
            }
        }
    }

    fun <T : Any> singleFromCallable(executorService: ExecutorService, callable: Callable<T>): Single<T> {
        return Single.create { emitter ->
            val latch = CountDownLatch(1)
            emitter.setCancellable { latch.countDown() }

            var result: T? = null
            var error: Throwable? = null

            val runnable = Runnable {
                try {
                    result = callable.call()
                } catch (e: Exception) {
                    error = e
                }

                latch.countDown()
            }

            executorService.submit(runnable)

            if (latch.safelyWait() && !emitter.isDisposed) {
                result?.run {
                    emitter.onSuccess(this)
                }

                error?.run {
                    emitter.onError(this)
                }
            }
        }
    }
}