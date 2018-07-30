package biz.dealnote.xmpp.callback;

import biz.dealnote.xmpp.fragment.FilesCriteria;
import biz.dealnote.xmpp.model.AccountContactPair;
import biz.dealnote.xmpp.model.AppRosterEntry;

public interface OnPlaceOpenCallback {
    void onContactCardOpen(AppRosterEntry entry);
    void onAccountManagerOpen(AccountContactPair pair);

    void showIncomeFiles(FilesCriteria criteria);
}
