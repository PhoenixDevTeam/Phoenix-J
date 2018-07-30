package biz.dealnote.xmpp.service.request;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.util.Log;

import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.service.request.exception.DataException;
import biz.dealnote.xmpp.util.XmppLogger;

public abstract class AbsXmppOperationManager extends AbsOperationManager {

    public static final String INTENT_EXTRA_RECEIVER = "com.foxykeep.datadroid.extra.receiver";
    public static final String INTENT_EXTRA_REQUEST = "com.foxykeep.datadroid.extra.request";
    public static final int ERROR_CODE = -1;
    private static final String LOG_TAG = AbsXmppOperationManager.class.getSimpleName();
    private static final int SUCCESS_CODE = 0;
    private IXmppContext mXmppContext;

    public AbsXmppOperationManager(IXmppContext xmppContext, Service service) {
        super(service);
        this.mXmppContext = xmppContext;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Request request = intent.getParcelableExtra(INTENT_EXTRA_REQUEST);
        request.setClassLoader(service.getClassLoader());

        ResultReceiver receiver = intent.getParcelableExtra(INTENT_EXTRA_RECEIVER);
        Operation operation = getOperationForType(request.getRequestType());

        try {
            Bundle result = operation.execute(service, mXmppContext, request);
            sendSuccess(receiver, result);
        } catch (DataException e) {
            XmppLogger.INSTANCE.e(LOG_TAG, "DataException", e);
            sendDataFailure(receiver);
        } catch (CustomRequestException e) {
            XmppLogger.INSTANCE.e(LOG_TAG, "Custom Exception", e);
            sendCustomFailure(receiver, onCustomRequestException(request, e));
        } catch (RuntimeException e) {
            XmppLogger.INSTANCE.e(LOG_TAG, "RuntimeException", e);
            sendDataFailure(receiver);
        }
    }

    /**
     * Proxy method for {@link #sendResult(android.os.ResultReceiver, android.os.Bundle, int)} when the work is a
     * success.
     *
     * @param receiver The result receiver received inside the {@link android.content.Intent}.
     * @param data     A {@link android.os.Bundle} with the data to send back.
     */
    private void sendSuccess(ResultReceiver receiver, Bundle data) {
        sendResult(receiver, data, SUCCESS_CODE);
    }

    /**
     * Proxy method for {@link #sendResult(android.os.ResultReceiver, android.os.Bundle, int)} when the work is a failure
     * due to the data (parsing for example).
     *
     * @param receiver The result receiver received inside the {@link android.content.Intent}.
     */
    private void sendDataFailure(ResultReceiver receiver) {
        Bundle data = new Bundle();
        data.putInt(AbsRequestManager.RECEIVER_EXTRA_ERROR_TYPE, AbsRequestManager.ERROR_TYPE_DATA);
        sendResult(receiver, data, ERROR_CODE);
    }

    /**
     * Proxy method for {@link #sendResult(android.os.ResultReceiver, android.os.Bundle, int)} when the work is a failure
     * due to {@link CustomRequestException} being thrown.
     *
     * @param receiver The result receiver received inside the {@link android.content.Intent}.
     * @param data     A {@link android.os.Bundle} the data to send back.
     */
    private void sendCustomFailure(ResultReceiver receiver, Bundle data) {
        if (data == null) {
            data = new Bundle();
        }
        data.putInt(AbsRequestManager.RECEIVER_EXTRA_ERROR_TYPE, AbsRequestManager.ERROR_TYPE_CUSTOM);
        sendResult(receiver, data, ERROR_CODE);
    }

    /**
     * Method used to send back the result to the {@link AbsRequestManager}.
     *
     * @param receiver The result receiver received inside the {@link android.content.Intent}.
     * @param data     A {@link android.os.Bundle} the data to send back.
     * @param code     The success/error code to send back.
     */
    private void sendResult(ResultReceiver receiver, Bundle data, int code) {
        XmppLogger.INSTANCE.d(LOG_TAG, "sendResult : " + ((code == SUCCESS_CODE) ? "Success" : "Failure"));

        Log.d(LOG_TAG, "receiver=" + receiver);

        if (receiver != null) {
            if (data == null) {
                data = new Bundle();
            }

            receiver.send(code, data);
        }
    }

    /**
     * Get the {@link com.foxykeep.datadroid.service.RequestService.Operation} corresponding to the given request type.
     *
     * @param requestType The request type (extracted from {@link Request}).
     * @return The corresponding {@link com.foxykeep.datadroid.service.RequestService.Operation}.
     */
    public abstract Operation getOperationForType(int requestType);

    /**
     * Call if a {@link CustomRequestException} is thrown by an {@link com.foxykeep.datadroid.service.RequestService.Operation}. You may return a
     * Bundle containing data to return to the {@link AbsRequestManager}.
     * <p/>
     * Default implementation return null. You may want to override this method in your
     * implementation of {@link com.foxykeep.datadroid.service.RequestService} to execute specific action and/or return specific
     * data.
     *
     * @param request   The {@link Request} which execution threw the exception.
     * @param exception The {@link CustomRequestException} thrown.
     * @return A {@link android.os.Bundle} containing data to return to the {@link AbsRequestManager}. Default
     * implementation return null.
     */
    protected Bundle onCustomRequestException(Request request, CustomRequestException exception) {
        return null;
    }

    /**
     * Interface to implement by your operations
     *
     * @author Foxykeep
     */
    public interface Operation {
        /**
         * Execute the request and returns a {@link android.os.Bundle} containing the data to return.
         *
         * @param context The context to use for your operation.
         * @param request The request to execute.
         * @return A {@link android.os.Bundle} containing the data to return. If no data to return, null.
         * @throws ConnectionException    Thrown when a connection error occurs. It will be propagated
         *                                to the {@link AbsRequestManager} as a
         *                                {@link AbsRequestManager#ERROR_TYPE_CONNEXION}.
         * @throws DataException          Thrown when a problem occurs while managing the data of the
         *                                webservice. It will be propagated to the {@link AbsRequestManager} as a
         *                                {@link AbsRequestManager#ERROR_TYPE_DATA}.
         * @throws CustomRequestException Any other exception you may have to throw. A call to
         *                                {@link com.foxykeep.datadroid.service.RequestService#onCustomRequestException(Request,
         *                                CustomRequestException)} will be made with the Exception thrown.
         */
        Bundle execute(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws DataException, CustomRequestException;
    }
}
