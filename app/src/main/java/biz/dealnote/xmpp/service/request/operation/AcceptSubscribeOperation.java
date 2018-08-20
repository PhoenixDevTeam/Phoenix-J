package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.Storages;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.MessageBuilder;
import biz.dealnote.xmpp.model.MessageUpdate;
import biz.dealnote.xmpp.model.Msg;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;
import biz.dealnote.xmpp.util.Unixtime;
import biz.dealnote.xmpp.util.Utils;

public class AcceptSubscribeOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, InterruptedException, XmppStringprepException {
        Account account = (Account) request.getParcelable(Extra.ACCOUNT);
        String jid = Utils.getBareJid(request.getString(Extra.JID));
        BareJid bareJid = JidCreate.bareFrom(jid);
        int mid = request.getInt(Extra.MESSAGE_ID);

        XMPPConnection connection = assertConnectionFor(xmppContext, account.id);

        Presence subscribed = new Presence(Presence.Type.subscribed);
        subscribed.setFrom(account.buildBareJid());
        subscribed.setTo(jid);
        connection.sendStanza(subscribed);

        Storages.getINSTANCE()
                .getMessages()
                .updateMessage(mid, MessageUpdate.simpleStatusChange(Msg.STATUS_ACCEPTED))
                .blockingAwait();

        Roster roster = Roster.getInstanceFor(connection);
        RosterEntry rosterEntry = roster.getEntry(bareJid);

        if (rosterEntry == null || (rosterEntry.getType() != RosterPacket.ItemType.to && rosterEntry.getType() != RosterPacket.ItemType.both)) {
            // если мы не подписаны на пользователя - то отправляем ему subscribe
            Presence subscribe = new Presence(Presence.Type.subscribe);
            subscribe.setFrom(account.buildBareJid());
            subscribe.setTo(jid);
            connection.sendStanza(subscribe);

            MessageBuilder builder = new MessageBuilder(account.id)
                    .setType(Msg.TYPE_SUBSCRIBE)
                    .setSenderJid(account.buildBareJid())
                    .setDestination(jid)
                    .setDate(Unixtime.now())
                    .setOut(true)
                    .setStatus(Msg.STATUS_WAITING_FOR_REASON);

            Storages.getINSTANCE()
                    .getMessages()
                    .saveMessage(builder)
                    .blockingGet();
        }

        return null;
    }
}
