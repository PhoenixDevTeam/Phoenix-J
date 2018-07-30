package biz.dealnote.xmpp.service.request;

import android.os.Bundle;

import biz.dealnote.xmpp.exception.Codes;

public class RequestAdapter implements AbsRequestManager.RequestListener {

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {

    }

    @Override
    public void onRequestConnectionError(Request request, int statusCode) {

    }

    @Override
    public void onRequestDataError(Request request) {

    }

    @Override
    public void onRequestCustomError(Request request, Bundle resultData) {
        if (resultData != null && resultData.containsKey(XmppOperationManager.AGRUMENT_CUSTOM_ERROR)) {
            String error = resultData.getString(XmppOperationManager.AGRUMENT_CUSTOM_ERROR, "Unknown error");
            int code = resultData.getInt(XmppOperationManager.AGRUMENT_CUSTOM_ERROR_CODE, Codes.UNDEFINED);
            onRequestCustomError(request, resultData, error, code);
        }
    }


    public void onRequestCustomError(Request request, Bundle resultData, String error, int code) {

    }
}
