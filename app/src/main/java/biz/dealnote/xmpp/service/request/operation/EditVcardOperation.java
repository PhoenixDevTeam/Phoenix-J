package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

public class EditVcardOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, InterruptedException {
        int accountId = request.getInt(Extra.ACCOUNT_ID);
        String jid = request.getString(Extra.JID);
        String firstName = request.getString(Extra.FIRST_NAME);
        String lastName = request.getString(Extra.LAST_NAME);

        AbstractXMPPConnection connection = assertConnectionFor(xmppContext, accountId);

        VCardManager manager = VCardManager.getInstanceFor(connection);

        VCard vCard;
        try {
            vCard = manager.loadVCard();
        } catch (XMPPException.XMPPErrorException e) {
            vCard = new VCard();
        } catch (ClassCastException e){
            //java.lang.ClassCastException: org.jivesoftware.smack.packet.EmptyResultIQ cannot be cast to org.jivesoftware.smackx.vcardtemp.packet.VCard
            vCard = new VCard();
        }

        vCard.setFirstName(firstName);
        vCard.setLastName(lastName);
        manager.saveVCard(vCard);

        VCard updated = manager.loadVCard();

        Repositories.getInstance()
                .getContactsRepository()
                .upsert(jid, updated)
                .blockingAwait();
        return null;
    }
}
