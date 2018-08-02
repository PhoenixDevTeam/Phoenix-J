package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.StringArray;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;

public class GetVcardOperation extends AbsXmppOperation {

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException, SmackException.NotConnectedException, SmackException.NoResponseException, XMPPException.XMPPErrorException {
        int accountId = request.getInt(Extra.ACCOUNT_ID);
        StringArray jids = (StringArray) request.getParcelable(Extra.JIDS);

        AbstractXMPPConnection connection = assertConnectionFor(xmppContext, accountId);

        try {
            for (String jid : jids.getArray()) {
                EntityBareJid entityBareJid = JidCreate.entityBareFrom(jid);
                VCard vCard = VCardManager.getInstanceFor(connection).loadVCard(entityBareJid);

                if (vCard != null) {
                    Repositories.getInstance()
                            .getUsersStorage()
                            .upsert(jid, vCard)
                            .blockingAwait();
                }

                Log.d(TAG, "success, vCard: " + vCard);
            }

        } catch (InterruptedException | XmppStringprepException e) {
            e.printStackTrace();
        }

        //User user = Contacts.findByJid(context, jid);
        //Bundle bundle = new Bundle();
        //bundle.putParcelable(Extra.CONTACT, user);
        return null;
    }
}
