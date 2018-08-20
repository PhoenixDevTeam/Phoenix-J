package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.Storages;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.MessageBuilder;
import biz.dealnote.xmpp.model.Msg;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;
import biz.dealnote.xmpp.util.Unixtime;
import biz.dealnote.xmpp.util.Utils;

public class AddContactOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, XmppStringprepException, InterruptedException {
        Account account = (Account) request.getParcelable(Extra.ACCOUNT);
        String jid = Utils.getBareJid(request.getString(Extra.JID));
        BareJid bareJid = JidCreate.bareFrom(jid);

        AbstractXMPPConnection connection = assertConnectionFor(xmppContext, account.id);

        Roster roster = Roster.getInstanceFor(connection);
        roster.createEntry(bareJid, jid, null);

        // отправляем сначала subscribe
        Presence subscribe = new Presence(Presence.Type.subscribe);
        //subscribe.setFrom(account.buildBareJid());
        subscribe.setTo(jid);
        connection.sendStanza(subscribe);

        MessageBuilder subscribeBuilder = new MessageBuilder(account.id)
                .setStatus(Msg.STATUS_WAITING_FOR_REASON)
                .setType(Msg.TYPE_SUBSCRIBE)
                .setDestination(jid)
                .setSenderJid(account.buildBareJid())
                .setOut(true)
                .setDate(Unixtime.now());

        Storages.getINSTANCE()
                .getMessages()
                .saveMessage(subscribeBuilder)
                .blockingGet();

        // теперь отправляем subscribed
        Presence subscribed = new Presence(Presence.Type.subscribed);
        //subscribed.setFrom(account.buildBareJid());
        subscribed.setTo(JidCreate.bareFrom(jid));
        connection.sendStanza(subscribed);

        MessageBuilder subscribedBuilder = new MessageBuilder(account.id)
                .setStatus(Msg.STATUS_WAITING_FOR_REASON)
                .setType(Msg.TYPE_SUBSCRIBED)
                .setDestination(jid)
                .setSenderJid(account.buildBareJid())
                .setOut(true)
                .setDate(Unixtime.now());

        Storages.getINSTANCE()
                .getMessages()
                .saveMessage(subscribedBuilder)
                .blockingGet();

        return null;
    }
}
