package biz.dealnote.xmpp.mvp.presenter.base;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import biz.dealnote.mvp.core.AbsPresenter;
import biz.dealnote.mvp.core.IMvpView;
import biz.dealnote.xmpp.App;
import biz.dealnote.xmpp.mvp.view.IErrorView;
import biz.dealnote.xmpp.mvp.view.IToastView;
import biz.dealnote.xmpp.util.Utils;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Created by admin on 24.09.2016.
 * phoenix
 */
public abstract class RxSupportPresenter<V extends IMvpView> extends AbsPresenter<V> {

    protected CompositeDisposable mCompositeSubscription = new CompositeDisposable();

    public RxSupportPresenter(@Nullable Bundle savedInstanceState) {
        super(savedInstanceState);
    }

    @Override
    public void onDestroyed() {
        mCompositeSubscription.dispose();
        super.onDestroyed();
    }

    protected void appendDisposable(@NonNull Disposable subscription){
        mCompositeSubscription.add(subscription);
    }

    protected Context getApplicationContext(){
        return App.getInstance();
    }

    protected String getString(@StringRes int res, Object... params){
        return getApplicationContext().getString(res, params);
    }

    protected static void showError(IErrorView view, String error){
        if(view != null){
            view.showError(error);
        }
    }

    protected void showError(IErrorView view, Throwable throwable){
        Throwable targetThrowable = Utils.getCauseIfRuntime(throwable);

        showError(view, targetThrowable.getMessage());
    }

    protected void showError(IErrorView view, @StringRes int res, Object ... params){
        showError(view, getString(res, params));
    }

    protected static void showShortToast(IToastView view, String text){
        if(view != null){
            view.showShortToast(text);
        }
    }

    protected void showShortToast(IToastView view, @StringRes int res, Object ... params){
        showShortToast(view, getString(res, params));
    }

    protected static void showLongToast(IToastView view, String text){
        if(view != null){
            view.showLongToast(text);
        }
    }

    protected void showLongToast(IToastView view, @StringRes int res, Object ... params){
        showLongToast(view, getString(res, params));
    }
}
