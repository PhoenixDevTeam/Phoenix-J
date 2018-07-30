package biz.dealnote.xmpp.dialog;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import biz.dealnote.xmpp.fragment.FragmentRequestManager;
import biz.dealnote.xmpp.service.request.Request;

public abstract class AbsRequestSupportDialogFragment extends DialogFragment implements FragmentRequestManager.Callback {

    private FragmentRequestManager requestManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestManager = new FragmentRequestManager(getContext(), this);
        requestManager.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        requestManager.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        requestManager.onSaveInstanceState(outState);
    }

    public FragmentRequestManager getRequestManager() {
        return requestManager;
    }

    @Override
    public abstract void onRestoreConnectionToRequest(Request request);

    @Override
    public abstract void onRequestFinished(Request request, Bundle resultData);

    @Override
    public abstract void onCustromError(Request request, String errorText, int code);
}
