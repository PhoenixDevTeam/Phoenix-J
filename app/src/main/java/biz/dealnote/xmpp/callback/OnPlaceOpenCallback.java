package biz.dealnote.xmpp.callback;

import biz.dealnote.xmpp.fragment.FilesCriteria;
import biz.dealnote.xmpp.model.AccountContactPair;
import biz.dealnote.xmpp.model.Contact;

public interface OnPlaceOpenCallback {
    void onContactCardOpen(Contact entry);
    void onAccountManagerOpen(AccountContactPair pair);

    void showIncomeFiles(FilesCriteria criteria);
}
