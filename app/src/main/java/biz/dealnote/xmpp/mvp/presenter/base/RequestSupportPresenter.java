package biz.dealnote.xmpp.mvp.presenter.base;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;

import biz.dealnote.mvp.core.IMvpView;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestAdapter;
import biz.dealnote.xmpp.service.request.XmppRequestManager;
import biz.dealnote.xmpp.util.Utils;

/**
 * Created by admin on 24.09.2016.
 * phoenix
 */
public abstract class RequestSupportPresenter<V extends IMvpView> extends RxSupportPresenter<V> {

    private static final String SAVE_CURRENT_REQUESTS = "save_current_requests";

    private ArrayList<Request> mCurrentRequests;
    private RequestAdapter mRequestAdapter;

    public RequestSupportPresenter(@Nullable Bundle savedInstanceState) {
        super(savedInstanceState);

        if(savedInstanceState != null){
            mCurrentRequests = savedInstanceState.getParcelableArrayList(SAVE_CURRENT_REQUESTS);
        } else {
            mCurrentRequests = new ArrayList<>();
        }

        mRequestAdapter = new RequestAdapter(){
            @Override
            public void onRequestCustomError(Request request, Bundle resultData, String error, int code) {
                if(mCurrentRequests.contains(request)){
                    mCurrentRequests.remove(request);
                    RequestSupportPresenter.this.onRequestError(request, resultData, error, code);
                }
            }

            @Override
            public void onRequestFinished(Request request, Bundle resultData) {
                if(mCurrentRequests.contains(request)){
                    mCurrentRequests.remove(request);
                    RequestSupportPresenter.this.onRequestFinished(request, resultData);
                }
            }
        };

        tryConnectToCurrentRequests();
    }

    /**
     * Что быдет делать фрагмент, когда запрос выполнился с ошибкой
     *
     * @param request   запрос
     */
    @CallSuper
    protected void onRequestError(@NonNull Request request, @Nullable Bundle resultData, String error, int code){

    }

    /**
     * Что будет делать фрагмент, когда запрос был выполнен успешно и вернул результат
     *
     * @param request    запрос
     * @param resultData результат выполения
     */
    @CallSuper
    protected void onRequestFinished(@NonNull Request request, @NonNull Bundle resultData) {

    }

    /**
     * Попытка переподключиться к коллекции запросов
     * (например, при перевороте экрана)
     */
    private void tryConnectToCurrentRequests() {
        XmppRequestManager manager = XmppRequestManager.from(getApplicationContext());

        Iterator<Request> iterator = mCurrentRequests.iterator();
        while (iterator.hasNext()){
            Request request = iterator.next();
            if (manager.isRequestInProgress(request)) {
                manager.execute(request, mRequestAdapter);
                onRestoreConnectionToRequest(request);
            } else {
                iterator.remove();
            }
        }
    }

    /**
     * Что будет делать фрагмент при восстановлении подключения к запросу (например, при перевороте экрана)
     *
     * @param request запрос, к которому восстановлено подключение
     */
    @CallSuper
    protected void onRestoreConnectionToRequest(Request request) {

    }

    /**
     * Выполнить запрос
     *
     * @param request обьект запроса
     */
    protected void executeRequest(@NonNull Request request) {
        if (!mCurrentRequests.contains(request)) {
            mCurrentRequests.add(request);
        }

        XmppRequestManager.from(getApplicationContext()).execute(request, mRequestAdapter);
    }

    /**
     * Игнорировать результаты всех запросов
     */
    protected void ignoreAll(){
        mCurrentRequests.clear();
    }

    /**
     * Игнорировать результат выполения запроса
     *
     * @param requestTypes типа запросов, которые необходимо игнорировать
     * @return количество "проигноренных" запросов
     */
    protected int ignoreRequestResult(int... requestTypes) {
        int removed = 0;
        Iterator<Request> iterator = mCurrentRequests.iterator();
        while (iterator.hasNext()) {
            Request request = iterator.next();

            for (int requestType : requestTypes) {
                if (request.getRequestType() == requestType) {
                    iterator.remove();
                    removed++;
                    break;
                }
            }
        }

        return removed;
    }

    /**
     * Присутсвуют ли запросы в списке ожидания
     *
     * @param requestTypes типы запросов
     * @return присутсвуют ли
     */
    protected boolean hasRequest(int... requestTypes) {
        for (Request request : mCurrentRequests) {
            for (int requestType : requestTypes) {
                if (request.getRequestType() == requestType) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Найти запрос по типу
     *
     * @param requestType тип искомого запроса
     * @return первый найденный запрос
     */
    protected Request findRequest(int requestType) {
        for (Request request : mCurrentRequests) {
            if (request.getRequestType() == requestType) {
                return request;
            }
        }

        return null;
    }

    protected Request getFirstRequestInQueue(){
        return Utils.isEmpty(mCurrentRequests) ? null : mCurrentRequests.get(0);
    }

    @Override
    public void saveState(@NonNull Bundle outState) {
        super.saveState(outState);
        outState.putParcelableArrayList(SAVE_CURRENT_REQUESTS, mCurrentRequests);
    }

    @Override
    public void onDestroyed() {
        XmppRequestManager.from(getApplicationContext()).removeRequestListener(mRequestAdapter);
        super.onDestroyed();
    }
}
