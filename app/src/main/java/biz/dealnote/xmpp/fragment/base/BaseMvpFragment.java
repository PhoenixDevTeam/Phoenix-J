package biz.dealnote.xmpp.fragment.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import biz.dealnote.mvp.compat.AbsMvpFragment;
import biz.dealnote.mvp.core.AbsPresenter;
import biz.dealnote.mvp.core.IMvpView;
import biz.dealnote.xmpp.mvp.view.IErrorView;
import biz.dealnote.xmpp.mvp.view.IToastView;
import biz.dealnote.xmpp.util.Objects;

/**
 * Created by ruslan.kolbasa on 01.11.2016.
 * phoenix_for_xmpp
 */
public abstract class BaseMvpFragment<P extends AbsPresenter<V>, V extends IMvpView>
        extends AbsMvpFragment<P, V> implements IMvpView, IErrorView, IToastView {

    @Override
    public void showError(String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showLongToast(String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showShortToast(String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    public Bundle requireArguments(){
        return Objects.requireNonNull(getArguments());
    }
}