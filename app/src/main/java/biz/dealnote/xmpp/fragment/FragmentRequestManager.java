package biz.dealnote.xmpp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;

import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestAdapter;
import biz.dealnote.xmpp.service.request.XmppRequestManager;

public class FragmentRequestManager {

    private static final String SAVE_CURRENT_REQUESTS = "save_current_requests";

    private Callback mCallback;
    private Context mContext;

    private ArrayList<Request> mQueue;
    private RequestAdapter mRequestAdapter;

    public FragmentRequestManager(Context context, Callback callback) {
        this.mCallback = callback;
        this.mContext = context;
    }

    public void onCreate(Bundle savedInstanceState) {
        mRequestAdapter = new RequestAdapter() {
            @Override
            public void onRequestFinished(Request request, Bundle resultData) {
                if (mQueue.contains(request)) {
                    mQueue.remove(request);
                    mCallback.onRequestFinished(request, resultData);
                }
            }

            @Override
            public void onRequestCustomError(Request request, Bundle resultData, String error, int code) {
                if (mQueue.contains(request)) {
                    mQueue.remove(request);
                    mCallback.onCustromError(request, error, code);
                }
            }
        };

        if (savedInstanceState != null) {
            restoreCurrentRequests(savedInstanceState);
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            restoreCurrentRequests(savedInstanceState);
        }

        tryConnectToCurrentRequests();
    }

    /**
     * Выполнить запрос
     *
     * @param request обьект запроса
     */
    public void executeRequest(@NonNull Request request) {
        if (mQueue == null) {
            mQueue = new ArrayList<>();
        }

        if (!mQueue.contains(request)) {
            mQueue.add(request);
        }

        XmppRequestManager.from(mContext).execute(request, mRequestAdapter);
    }

    /**
     * Попытка переподключиться к коллекции запросов
     * (например, при перевороте экрана)
     */
    private void tryConnectToCurrentRequests() {
        if (mQueue == null) {
            return;
        }

        XmppRequestManager requestManager = XmppRequestManager.from(mContext);
        Iterator<Request> iterator = mQueue.iterator();

        while (iterator.hasNext()){
            Request request = iterator.next();
            if(requestManager.isRequestInProgress(request)){
                requestManager.execute(request, mRequestAdapter);
                mCallback.onRestoreConnectionToRequest(request);
            } else {
                iterator.remove();
            }
        }
    }

    /**
     * Получить первый в списке запрос
     *
     * @return обьект первого в списке запроса
     */
    public Request getFirstRequestInQueue() {
        if (mQueue == null || mQueue.isEmpty()) {
            return null;
        }

        return mQueue.get(0);
    }

    /**
     * Получить последний добавленный запрос
     *
     * @return обьект запроса
     */
    public Request getLastRequestInQueue() {
        if (mQueue == null || mQueue.isEmpty()) {
            return null;
        }

        return mQueue.get(mQueue.size() - 1);
    }

    /**
     * Игнорировать результат выполения запроса
     *
     * @param requestTypes типы запросов, которые необходимо игнорировать
     * @return количество "проигноренных" запросов
     */
    public int ignoreRequestResult(int... requestTypes) {
        if (mQueue == null) {
            return 0;
        }

        int removed = 0;
        Iterator<Request> iterator = mQueue.iterator();
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
    public boolean hasRequest(int... requestTypes) {
        if (mQueue == null) {
            return false;
        }

        for (Request request : mQueue) {
            for (int requestType : requestTypes) {
                if (request.getRequestType() == requestType) {
                    return true;
                }
            }
        }

        return false;
    }

    private void restoreCurrentRequests(Bundle savedInstanceState) {
        mQueue = savedInstanceState.getParcelableArrayList(SAVE_CURRENT_REQUESTS);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(SAVE_CURRENT_REQUESTS, mQueue);
    }

    public void onDestroy(){
        XmppRequestManager.from(mContext).removeRequestListener(mRequestAdapter);
    }

    public interface Callback {
        /**
         * Что будет делать фрагмент при восстановлении подключения к запросу (например, при перевороте экрана)
         *
         * @param request запрос, к которому восстановлено подключение
         */
        void onRestoreConnectionToRequest(Request request);

        /**
         * Что будет делать фрагмент, когда запрос был выполнен успешно и вернул результат
         *
         * @param request    запрос
         * @param resultData результат выполения
         */
        void onRequestFinished(Request request, Bundle resultData);

        /**
         * Что быдет делать фрагмент, когда запрос выполнился с ошибкой
         *
         * @param request   запрос
         * @param errorText локализированный текст ошибки
         */
        void onCustromError(Request request, String errorText, int code);
    }
}
