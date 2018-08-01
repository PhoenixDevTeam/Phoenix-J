package biz.dealnote.xmpp.service.request

import biz.dealnote.xmpp.service.IConnectionManager
import biz.dealnote.xmpp.util.safelyWait
import io.reactivex.Single
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import org.jxmpp.jid.impl.JidCreate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class XmppRxApiImpl(private val connectionManager: IConnectionManager) : IXmppRxApi {

    override fun getVCard(acccount: Int, jid: String): Single<VCard> {
        return Single.create { emitter ->
            val connection = connectionManager.findConnectionFor(acccount)

            connection?.run {
                val vcardManager = VCardManager.getInstanceFor(connection)
                val entityBareJid = JidCreate.entityBareFrom(jid)

                val latch = CountDownLatch(1)
                emitter.setCancellable { latch.countDown() }

                var result: Result<VCard> = Result()
                val runnable = Runnable {
                    result = try {
                        Result(data = vcardManager.loadVCard(entityBareJid))
                    } catch (e: Exception) {
                        Result(error = e)
                    }

                    latch.countDown()
                }

                xmppExecutor.submit(runnable)

                if(latch.safelyWait() && !emitter.isDisposed){
                    result.data?.run {
                        emitter.onSuccess(this)
                    }

                    result.error?.run {
                        emitter.onError(this)
                    }
                }
            }
        }
    }

    class Result<T> (val data: T? = null, val error: Throwable? = null)

    companion object {
        private val xmppExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    }
}