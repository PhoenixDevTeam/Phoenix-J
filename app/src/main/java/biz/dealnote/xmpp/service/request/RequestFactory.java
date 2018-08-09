package biz.dealnote.xmpp.service.request;

import android.net.Uri;
import android.support.annotation.NonNull;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.Msg;
import biz.dealnote.xmpp.service.StringArray;

public class RequestFactory {

    public static final int REQUEST_DELETE_ROSTER_ENTRY = 1;
    public static final int REQUEST_GET_VCARD = 2;
    public static final int REQUEST_SEND_PRESENCE = 3;
    public static final int REQUEST_SIGN_IN = 4;
    public static final int REQUEST_SEND_MESSAGE = 5;
    public static final int REQUEST_DELETE_ACCOUNT = 6;
    public static final int REQUEST_ACCEPT_FILE_TRANSFER = 7;
    public static final int REQUEST_DECLINE_FILE_TRANSFER = 8;
    public static final int REQUEST_SEND_FILE = 9;
    public static final int REQUEST_ADD_CONTACT = 10;
    public static final int REQUEST_CREATE_ACCOUNT = 12;
    public static final int REQUEST_CHANGE_AVATAR = 13;
    public static final int REQUEST_CHANGE_PASSWORD = 14;
    public static final int REQUEST_CONNECT_TO_ACCOUNTS = 15;
    public static final int REQUEST_EDIT_VCARD = 16;
    public static final int REQUEST_ACCEPT_SUBSCRIPTION = 17;
    public static final int REQUEST_DECLINE_SUBSCRIPTION = 18;
    public static final int REQUEST_START_OTR = 20;
    public static final int REQUEST_REFRESH_OTR = 22;
    public static final int REQUEST_END_OTR = 23;
    public static final int REQUEST_SEND_MESSAGE_QUIET = 24;

    public static Request getMessageQuietSendRequest(@NonNull Account account, @NonNull String jid, @NonNull String message, int messageType){
        Request request = new Request(REQUEST_SEND_MESSAGE_QUIET);
        request.put(Extra.ACCOUNT, account);
        request.put(Extra.JID, jid);
        request.put(Extra.MESSAGE, message);
        request.put(Extra.TYPE, messageType);
        return request;
    }

    public static Request getEndOTRRequest(Account account, String jid){
        Request request = new Request(REQUEST_END_OTR);
        request.put(Extra.ACCOUNT, account);
        request.put(Extra.JID, jid);
        return request;
    }

    public static Request getRefreshOTRRequest(Account account, String jid){
        Request request = new Request(REQUEST_REFRESH_OTR);
        request.put(Extra.ACCOUNT, account);
        request.put(Extra.JID, jid);
        return request;
    }

    public static Request getStartOTRRequest(Account account, String jid){
        Request request = new Request(REQUEST_START_OTR);
        request.put(Extra.ACCOUNT, account);
        request.put(Extra.JID, jid);
        return request;
    }

    public static Request getDeclineSubscriptionRequest(Account account, String jid, int messageId) {
        Request request = new Request(REQUEST_DECLINE_SUBSCRIPTION);
        request.put(Extra.ACCOUNT, account);
        request.put(Extra.JID, jid);
        request.put(Extra.MESSAGE_ID, messageId);
        return request;
    }

    public static Request getAcceptSubscriptionRequest(Account account, String jid, int messageId) {
        Request request = new Request(REQUEST_ACCEPT_SUBSCRIPTION);
        request.put(Extra.ACCOUNT, account);
        request.put(Extra.JID, jid);
        request.put(Extra.MESSAGE_ID, messageId);
        return request;
    }

    public static Request getEditVcardRequest(int accountId, String jid, String firstName, String lastName) {
        Request request = new Request(REQUEST_EDIT_VCARD);
        request.put(Extra.ACCOUNT_ID, accountId);
        request.put(Extra.JID, jid);
        request.put(Extra.FIRST_NAME, firstName);
        request.put(Extra.LAST_NAME, lastName);
        return request;
    }

    public static Request getConnectToAccountsRequest() {
        return new Request(REQUEST_CONNECT_TO_ACCOUNTS);
    }

    public static Request getChangePasswordRequest(int accountId, String newPassword) {
        Request request = new Request(REQUEST_CHANGE_PASSWORD);
        request.put(Extra.PASSWORD, newPassword);
        request.put(Extra.ACCOUNT_ID, accountId);
        return request;
    }

    public static Request getChangeAvatarRequest(int accountId, String myJid, Uri avaUri) {
        Request request = new Request(REQUEST_CHANGE_AVATAR);
        request.put(Extra.ACCOUNT_ID, accountId);
        request.put(Extra.URI, avaUri);
        request.put(Extra.JID, myJid);
        return request;
    }

    public static Request getCreateAccountRequest(String host, int port, String login, String password) {
        Request request = new Request(REQUEST_CREATE_ACCOUNT);
        request.put(Extra.HOST, host);
        request.put(Extra.PORT, port);
        request.put(Extra.LOGIN, login);
        request.put(Extra.PASSWORD, password);
        return request;
    }

    public static Request getAddContactRequest(Account account, String jid) {
        Request request = new Request(REQUEST_ADD_CONTACT);
        request.put(Extra.ACCOUNT, account);
        request.put(Extra.JID, jid);
        return request;
    }

    public static Request getSignInRequest(String login, String password, String host, int port) {
        Request request = new Request(REQUEST_SIGN_IN);
        request.put(Extra.LOGIN, login);
        request.put(Extra.PASSWORD, password);
        request.put(Extra.HOST, host);
        request.put(Extra.PORT, port);
        return request;
    }

    public static Request getDeleteRosterEntryRequest(Account account, String jid) {
        Request request = new Request(REQUEST_DELETE_ROSTER_ENTRY);
        request.put(Extra.ACCOUNT, account);
        request.put(Extra.JID, jid);
        return request;
    }

    public static Request getAcceptFileTranferRequest(int messageId) {
        Request request = new Request(REQUEST_ACCEPT_FILE_TRANSFER);
        request.put(Extra.MESSAGE_ID, messageId);
        return request;
    }

    public static Request getCancelFileTranferRequest(int messageId) {
        Request request = new Request(REQUEST_DECLINE_FILE_TRANSFER);
        request.put(Extra.MESSAGE_ID, messageId);
        return request;
    }

    public static Request getDeleteAccountRequest(int accountId) {
        Request request = new Request(REQUEST_DELETE_ACCOUNT);
        request.put(Extra.ACCOUNT_ID, accountId);
        return request;
    }

    public static Request getVcardRequest(int accountId, @NonNull StringArray stringArray) {
        Request request = new Request(REQUEST_GET_VCARD);
        request.put(Extra.ACCOUNT_ID, accountId);
        request.put(Extra.JIDS, stringArray);
        return request;
    }

    public static Request getSendPresenceRequest(int accountId, String from, String to, int type) {
        Request request = new Request(REQUEST_SEND_PRESENCE);
        request.put(Extra.ACCOUNT_ID, accountId);
        request.put(Extra.TO, to);
        request.put(Extra.FROM, from);
        request.put(Extra.TYPE, type);
        return request;
    }

    public static Request getSendMessageRequest(@NonNull Msg message) {
        Request request = new Request(REQUEST_SEND_MESSAGE);
        request.put(Extra.MESSAGE, message);
        return request;
    }

    public static Request getSendFileRequest(int accountId, int messageId, String to, Uri uri, String filename, String mime) {
        Request request = new Request(REQUEST_SEND_FILE);
        request.put(Extra.ACCOUNT_ID, accountId);
        request.put(Extra.TO, to);
        request.put(Extra.URI, uri);
        request.put(Extra.FILENAME, filename);
        request.put(Extra.TYPE, mime);
        request.put(Extra.MESSAGE_ID, messageId);
        return request;
    }
}
