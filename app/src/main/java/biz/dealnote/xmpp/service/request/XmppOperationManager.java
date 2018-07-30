package biz.dealnote.xmpp.service.request;

import android.app.Service;
import android.os.Bundle;

import biz.dealnote.xmpp.exception.CustomAppException;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.operation.AcceptSubscribeOperation;
import biz.dealnote.xmpp.service.request.operation.AccountDeleteOperation;
import biz.dealnote.xmpp.service.request.operation.AddContactOperation;
import biz.dealnote.xmpp.service.request.operation.ChangeAvatarOperation;
import biz.dealnote.xmpp.service.request.operation.ChangePasswordOperation;
import biz.dealnote.xmpp.service.request.operation.ConnectToAccountsOperation;
import biz.dealnote.xmpp.service.request.operation.CreateAccountOperation;
import biz.dealnote.xmpp.service.request.operation.DeclineFileTransferOperation;
import biz.dealnote.xmpp.service.request.operation.DeclineSubscriptionOperation;
import biz.dealnote.xmpp.service.request.operation.EditVcardOperation;
import biz.dealnote.xmpp.service.request.operation.EndOTRSessionOperation;
import biz.dealnote.xmpp.service.request.operation.FileSendOperation;
import biz.dealnote.xmpp.service.request.operation.GetVcardOperation;
import biz.dealnote.xmpp.service.request.operation.IncomeFileAcceptOperation;
import biz.dealnote.xmpp.service.request.operation.MessageSendOperation;
import biz.dealnote.xmpp.service.request.operation.QuietMessageSendOperation;
import biz.dealnote.xmpp.service.request.operation.RefreshOTRSessionOperation;
import biz.dealnote.xmpp.service.request.operation.RemoveContactOperation;
import biz.dealnote.xmpp.service.request.operation.SendPresenseOperation;
import biz.dealnote.xmpp.service.request.operation.SignInOperation;
import biz.dealnote.xmpp.service.request.operation.StartOTRSessionOperation;

public class XmppOperationManager extends AbsXmppOperationManager {

    public static final String AGRUMENT_CUSTOM_ERROR = "custom_error";
    public static final String AGRUMENT_CUSTOM_ERROR_CODE = "custom_error_code";

    public XmppOperationManager(IXmppContext xmppContext, Service service) {
        super(xmppContext, service);
    }

    @Override
    public Operation getOperationForType(int requestType) {
        switch (requestType) {
            case RequestFactory.REQUEST_DELETE_ROSTER_ENTRY:
                return new RemoveContactOperation();
            case RequestFactory.REQUEST_GET_VCARD:
                return new GetVcardOperation();
            case RequestFactory.REQUEST_SEND_PRESENCE:
                return new SendPresenseOperation();
            case RequestFactory.REQUEST_SIGN_IN:
                return new SignInOperation();
            case RequestFactory.REQUEST_SEND_MESSAGE:
                return new MessageSendOperation();
            case RequestFactory.REQUEST_DELETE_ACCOUNT:
                return new AccountDeleteOperation();
            case RequestFactory.REQUEST_ACCEPT_FILE_TRANSFER:
                return new IncomeFileAcceptOperation();
            case RequestFactory.REQUEST_DECLINE_FILE_TRANSFER:
                return new DeclineFileTransferOperation();
            case RequestFactory.REQUEST_SEND_FILE:
                return new FileSendOperation();
            case RequestFactory.REQUEST_ADD_CONTACT:
                return new AddContactOperation();
            case RequestFactory.REQUEST_CREATE_ACCOUNT:
                return new CreateAccountOperation();
            case RequestFactory.REQUEST_CHANGE_AVATAR:
                return new ChangeAvatarOperation();
            case RequestFactory.REQUEST_CHANGE_PASSWORD:
                return new ChangePasswordOperation();
            case RequestFactory.REQUEST_CONNECT_TO_ACCOUNTS:
                return new ConnectToAccountsOperation();
            case RequestFactory.REQUEST_EDIT_VCARD:
                return new EditVcardOperation();
            case RequestFactory.REQUEST_ACCEPT_SUBSCRIPTION:
                return new AcceptSubscribeOperation();
            case RequestFactory.REQUEST_DECLINE_SUBSCRIPTION:
                return new DeclineSubscriptionOperation();
            case RequestFactory.REQUEST_START_OTR:
                return new StartOTRSessionOperation();
            case RequestFactory.REQUEST_END_OTR:
                return new EndOTRSessionOperation();
            case RequestFactory.REQUEST_REFRESH_OTR:
                return new RefreshOTRSessionOperation();
            case RequestFactory.REQUEST_SEND_MESSAGE_QUIET:
                return new QuietMessageSendOperation();
        }

        return null;
    }

    @Override
    protected Bundle onCustomRequestException(Request request, CustomRequestException exception) {
        Bundle bundle = super.onCustomRequestException(request, exception);
        if (exception instanceof CustomAppException) {
            CustomAppException customAppException = (CustomAppException) exception;
            if (bundle == null) {
                bundle = new Bundle();
            }

            bundle.putString(AGRUMENT_CUSTOM_ERROR, customAppException.getMessage());
            bundle.putInt(AGRUMENT_CUSTOM_ERROR_CODE, customAppException.code);
        }

        return bundle;
    }
}
