package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.AppRoster;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.model.MessageBuilder;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;
import biz.dealnote.xmpp.util.Unixtime;
import biz.dealnote.xmpp.util.Utils;

public class RemoveContactOperation extends AbsXmppOperation {

    private static final String TAG = RemoveContactOperation.class.getSimpleName();

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, XmppStringprepException, InterruptedException {
        Account account = (Account) request.getParcelable(Extra.ACCOUNT);
        String jid = Utils.getBareJid(request.getString(Extra.JID));

        BareJid bareJid = JidCreate.bareFrom(jid);

        AbstractXMPPConnection connection = assertConnectionFor(xmppContext, account.id);
        Roster roster = Roster.getInstanceFor(connection);
        RosterEntry entry = roster.getEntry(bareJid);

        boolean deletedFromDd = false;

        if (entry != null) {
            boolean needUnsubcribe = false;
            boolean needUnsubcribed = false;

            switch (entry.getType()) {
                case none:
                    needUnsubcribe = false;
                    needUnsubcribed = false;
                    break;
                case to:
                    needUnsubcribe = true;
                    needUnsubcribed = false;
                    break;
                case from:
                    needUnsubcribe = false;
                    needUnsubcribed = true;
                    break;
                case both:
                    needUnsubcribe = true;
                    needUnsubcribed = true;
                    break;
            }

            Log.d(TAG, "account.id: " + account.id + ", jid: " + jid + ", needUnsubcribe: " + needUnsubcribe + ", needUnsubcribed: " + needUnsubcribed);

            roster.removeEntry(entry);

            if (needUnsubcribed) {
                Presence unsubcribed = new Presence(Presence.Type.unsubscribed);
                unsubcribed.setFrom(account.buildBareJid());
                unsubcribed.setTo(jid);
                connection.sendStanza(unsubcribed);

                Repositories.getInstance()
                        .getMessages()
                        .saveMessage(new MessageBuilder(account.id)
                                .setDestination(jid)
                                .setSenderJid(account.buildBareJid())
                                .setDate(Unixtime.now())
                                .setOut(true)
                                .setType(AppMessage.TYPE_UNSUBSCRIBE))
                        .blockingGet();
            }

            if (needUnsubcribe) {
                Presence unsubcribe = new Presence(Presence.Type.unsubscribe);
                unsubcribe.setFrom(account.buildBareJid());
                unsubcribe.setTo(jid);
                connection.sendStanza(unsubcribe);

                Repositories.getInstance()
                        .getMessages()
                        .saveMessage(new MessageBuilder(account.id)
                                .setDestination(jid)
                                .setSenderJid(account.buildBareJid())
                                .setType(AppMessage.TYPE_UNSUBSCRIBE)
                                .setOut(true)
                                .setDate(Unixtime.now()))
                        .blockingGet();
            }
        } else {
            deletedFromDd = AppRoster.deleteRosterEntry(context, account.id, jid);
            Log.d(TAG, "No entry in roster, try to delete from db, deletedFromDd: " + deletedFromDd);
        }

        // результат положительный, если мы удалили из ростера или из бд
        return buildSimpleSuccessResult(entry != null || deletedFromDd);
    }
}
