package biz.dealnote.xmpp.service.request

import biz.dealnote.xmpp.service.IConnectionManager
import biz.dealnote.xmpp.util.safelyWait
import io.reactivex.Completable
import io.reactivex.Single
import org.jivesoftware.smack.AbstractXMPPConnection
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import org.jxmpp.jid.impl.JidCreate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class XmppRxApiImpl(private val connectionManager: IConnectionManager) : IXmppRxApi {

    override fun getVCard(acccount: Int, jid: String): Single<VCard> {
        return singleFromCallable(xmppExecutor, Callable {
            val connection: AbstractXMPPConnection = connectionManager.findConnectionFor(acccount)!!
            val entityBareJid = JidCreate.entityBareFrom(jid)
            return@Callable VCardManager.getInstanceFor(connection).loadVCard(entityBareJid)
        })
    }

    companion object {
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

        private val xmppExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    }
}