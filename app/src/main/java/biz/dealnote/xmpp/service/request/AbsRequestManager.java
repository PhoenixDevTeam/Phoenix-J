/**
 * 2011 Foxykeep (http://datadroid.foxykeep.com)
 * <p>
 * Licensed under the Beerware License : <br />
 * As long as you retain this notice you can do whatever you want with this stuff. If we meet some
 * day, and you think this stuff is worth it, you can buy me a beer in return
 */

package biz.dealnote.xmpp.service.request;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import biz.dealnote.xmpp.service.XmppService;
import biz.dealnote.xmpp.util.XmppLogger;

/**
 * This class allows to send requests through a {@link AbsXmppOperationManager}.
 * <p>
 * This class needs to be subclassed in your project.
 * <p>
 * You can check the following page to see a tutorial on how to implement a webservice call using
 * the {@link AbsRequestManager} : <a
 * href="http://www.datadroidlib.com/installation">http://www.datadroidlib.com/installation</a>.
 *
 * @author Foxykeep
 */
public abstract class AbsRequestManager {

    public static final String RECEIVER_EXTRA_ERROR_TYPE = "com.foxykeep.datadroid.extra.error";
    public static final String RECEIVER_EXTRA_CONNECTION_ERROR_STATUS_CODE = "com.foxykeep.datadroid.extra.connectionErrorStatusCode";
    public static final int ERROR_TYPE_CONNEXION = 1;
    public static final int ERROR_TYPE_DATA = 2;
    public static final int ERROR_TYPE_CUSTOM = 3;

    private static final String TAG = AbsRequestManager.class.getSimpleName();
    private final Context mContext;
    private final Class<? extends XmppService> mRequestService;
    private final HashMap<Request, RequestReceiver> mRequestReceiverMap;

    protected AbsRequestManager(Context context, Class<? extends XmppService> requestService) {
        this.mContext = context.getApplicationContext();
        this.mRequestService = requestService;
        this.mRequestReceiverMap = new HashMap<>();
    }

    /**
     * Add a {@link AbsRequestManager.RequestListener} to this {@link AbsRequestManager} to a specific {@link Request}.
     * Clients may use it in order to be notified when the corresponding request is completed.
     * <p>
     * The listener is automatically removed when the request is completed and they are notified.
     * <p>
     * <b>Warning !! </b> If it's an {@link android.app.Activity} or a {@link android.app.Fragment} that is used as a
     * listener, it must be detached when {@link android.app.Activity#onPause} is called in an {@link android.app.Activity}.
     *
     * @param listener The listener called when the Request is completed.
     * @param request  The {@link Request} to listen to.
     */
    public final void addRequestListener(RequestListener listener, Request request) {
        if (listener == null) {
            return;
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null.");
        }

        RequestReceiver requestReceiver = mRequestReceiverMap.get(request);
        if (requestReceiver == null) {
            XmppLogger.INSTANCE.w(TAG, "You tried to add a listener to a non-existing request.");
            return;
        }

        requestReceiver.addListenerHolder(new ListenerHolder(listener));
    }

    /**
     * Remove a {@link AbsRequestManager.RequestListener} to this {@link AbsRequestManager} from every {@link Request}s
     * which it is listening to.
     *
     * @param listener The listener to remove.
     */
    public final void removeRequestListener(RequestListener listener) {
        removeRequestListener(listener, null);
    }

    /**
     * Remove a {@link AbsRequestManager.RequestListener} to this {@link AbsRequestManager} from a specific
     * {@link Request}.
     *
     * @param listener The listener to remove.
     * @param request  The {@link Request} associated with this listener. If null, the listener will
     *                 be removed from every request it is currently associated with.
     */
    public final void removeRequestListener(RequestListener listener, Request request) {
        if (listener == null) {
            return;
        }
        ListenerHolder holder = new ListenerHolder(listener);
        if (request != null) {
            RequestReceiver requestReceiver = mRequestReceiverMap.get(request);
            if (requestReceiver != null) {
                requestReceiver.removeListenerHolder(holder);
            }
        } else {
            for (RequestReceiver requestReceiver : mRequestReceiverMap.values()) {
                requestReceiver.removeListenerHolder(holder);
            }
        }
    }

    /**
     * Return whether a {@link Request} is still in progress or not.
     *
     * @param request The request.
     * @return Whether the request is still in progress or not.
     */
    public final boolean isRequestInProgress(Request request) {
        return mRequestReceiverMap.containsKey(request);
    }

    /**
     * Execute the {@link Request}.
     *
     * @param request  The request to execute.
     * @param listener The listener called when the Request is completed.
     */
    public final void execute(Request request, RequestListener listener) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null.");
        }

        if (mRequestReceiverMap.containsKey(request)) {
            XmppLogger.INSTANCE.d(TAG, "This request is already in progress. Adding the new listener to it.");
            // This exact request is already in progress. Adding the new listener.
            addRequestListener(listener, request);
            return;
        }

        XmppLogger.INSTANCE.d(TAG, "Creating a new request and adding the listener to it.");

        RequestReceiver requestReceiver = new RequestReceiver(request);
        mRequestReceiverMap.put(request, requestReceiver);

        addRequestListener(listener, request);

        Intent intent = new Intent(mContext, mRequestService);
        intent.setAction(XmppService.ACTION_EXECUTE_REQUEST);
        intent.putExtra(AbsXmppOperationManager.INTENT_EXTRA_RECEIVER, requestReceiver);
        intent.putExtra(AbsXmppOperationManager.INTENT_EXTRA_REQUEST, request);
        mContext.startService(intent);
    }

    /**
     * Clients may implements this interface to be notified when a request is finished.
     *
     * @author Foxykeep
     */
    public interface RequestListener extends EventListener {

        /**
         * Event fired when a request is finished.
         *
         * @param request    The {@link Request} defining the request.
         * @param resultData The result of the service execution.
         */
        void onRequestFinished(Request request, Bundle resultData);

        /**
         * Event fired when a request encountered a connection error.
         *
         * @param request    The {@link Request} defining the request.
         * @param statusCode The HTTP status code returned by the server (if the request succeeded
         *                   by the HTTP status code was not {@link org.apache.http.HttpStatus#SC_OK}) or -1 if it was a
         *                   connection problem
         */
        void onRequestConnectionError(Request request, int statusCode);

        /**
         * Event fired when a request encountered a data error.
         *
         * @param request The {@link Request} defining the request.
         */
        void onRequestDataError(Request request);

        /**
         * Event fired when a request encountered a custom error.
         *
         * @param request    The {@link Request} defining the request.
         * @param resultData The result of the service execution.
         */
        void onRequestCustomError(Request request, Bundle resultData);
    }

    @SuppressLint("ParcelCreator")
    private final class RequestReceiver extends ResultReceiver {

        private final Request mRequest;
        private final Set<ListenerHolder> mListenerHolderSet;

        RequestReceiver(Request request) {
            super(new Handler(Looper.getMainLooper()));
            mRequest = request;
            mListenerHolderSet = Collections.synchronizedSet(new HashSet<ListenerHolder>());
        }

        void addListenerHolder(ListenerHolder listenerHolder) {
            synchronized (mListenerHolderSet) {
                mListenerHolderSet.add(listenerHolder);
            }
        }

        void removeListenerHolder(ListenerHolder listenerHolder) {
            synchronized (mListenerHolderSet) {
                mListenerHolderSet.remove(listenerHolder);
            }
        }

        @Override
        public void onReceiveResult(int resultCode, Bundle resultData) {
            mRequestReceiverMap.remove(mRequest);

            // Call the available listeners
            synchronized (mListenerHolderSet) {
                for (ListenerHolder listenerHolder : mListenerHolderSet) {
                    listenerHolder.onRequestFinished(mRequest, resultCode, resultData);
                }
            }
        }
    }

    private final class ListenerHolder {

        private final WeakReference<RequestListener> mListenerRef;
        private final int mHashCode;

        ListenerHolder(RequestListener listener) {
            mListenerRef = new WeakReference<>(listener);
            mHashCode = 31 + listener.hashCode();
        }

        void onRequestFinished(Request request, int resultCode, Bundle resultData) {
            mRequestReceiverMap.remove(request);

            RequestListener listener = mListenerRef.get();
            if (listener != null) {
                if (resultCode == AbsXmppOperationManager.ERROR_CODE) {
                    switch (resultData.getInt(RECEIVER_EXTRA_ERROR_TYPE)) {
                        case ERROR_TYPE_DATA:
                            listener.onRequestDataError(request);
                            break;

                        case ERROR_TYPE_CONNEXION:
                            int statusCode = resultData.getInt(RECEIVER_EXTRA_CONNECTION_ERROR_STATUS_CODE);
                            listener.onRequestConnectionError(request, statusCode);
                            break;

                        case ERROR_TYPE_CUSTOM:
                            listener.onRequestCustomError(request, resultData);
                            break;
                    }
                } else {
                    listener.onRequestFinished(request, resultData);
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ListenerHolder) {
                ListenerHolder oHolder = (ListenerHolder) o;
                return mListenerRef != null && oHolder.mListenerRef != null && mHashCode == oHolder.mHashCode;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return mHashCode;
        }
    }
}
