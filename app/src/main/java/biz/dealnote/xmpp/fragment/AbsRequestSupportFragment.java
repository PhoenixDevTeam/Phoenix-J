package biz.dealnote.xmpp.fragment;

import android.os.Bundle;

import biz.dealnote.xmpp.fragment.base.BaseFragment;
import biz.dealnote.xmpp.service.request.Request;

public abstract class AbsRequestSupportFragment extends BaseFragment implements FragmentRequestManager.Callback {

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        requestManager.onDestroy();
    }
}
