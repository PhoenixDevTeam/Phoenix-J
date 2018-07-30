package biz.dealnote.xmpp.service.request;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class AbsOperationManager {

    private static final String TAG = AbsOperationManager.class.getSimpleName();
    private static final long STOP_SELF_DELAY = TimeUnit.SECONDS.toMillis(30L);

    private final Runnable mStopSelfRunnable = new Runnable() {
        @Override
        public void run() {
            //stopSelf();
            Log.d(TAG, "All operations are done");
        }
    };

    protected Service service;
    private ExecutorService mThreadPool;
    private ArrayList<Future<?>> mFutureList;
    private Handler mHandler;

    private final Runnable mWorkDoneRunnable = new Runnable() {
        @Override
        public void run() {
            if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
                throw new IllegalStateException("This runnable can only be called in the Main thread!");
            }

            final ArrayList<Future<?>> futureList = mFutureList;
            for (int i = 0; i < futureList.size(); i++) {
                if (futureList.get(i).isDone()) {
                    futureList.remove(i);
                    i--;
                }
            }

            if (futureList.isEmpty()) {
                mHandler.postDelayed(mStopSelfRunnable, STOP_SELF_DELAY);
            }
        }
    };

    public AbsOperationManager(Service service) {
        this.service = service;
        int maximumNumberOfThreads = getMaximumNumberOfThreads();
        if (maximumNumberOfThreads <= 0) {
            throw new IllegalArgumentException("Maximum number of threads must be strictly positive");
        }

        mThreadPool = Executors.newFixedThreadPool(maximumNumberOfThreads);
        mHandler = new Handler();
        mFutureList = new ArrayList<>();
    }

    public void queue(Intent intent) {
        mHandler.removeCallbacks(mStopSelfRunnable);

        Future<?> future = mThreadPool.submit(new IntentRunnable(intent));
        mFutureList.add(future);
    }

    public void shutdown() {
        mThreadPool.shutdown();
    }

    /**
     * Define the maximum number of concurrent worker threads used to execute the incoming Intents.
     * <p/>
     * By default only one concurrent worker thread is used at the same time. Overrides this method
     * in subclasses to change this number.
     * <p/>
     * This method is called once in the {@link #onCreate()}. Modifying the value returned after the
     * {@link #onCreate()} is called will have no effect.
     *
     * @return The maximum number of concurrent worker threads
     */
    protected int getMaximumNumberOfThreads() {
        return 5;
    }

    /**
     * This method is invoked on the worker thread with a request to process. The processing happens
     * on a worker thread that runs independently from other application logic. When all requests
     * have been handled, the IntentService stops itself, so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link android.content.Context#startService(android.content.Intent)}.
     */
    abstract protected void onHandleIntent(Intent intent);

    private class IntentRunnable implements Runnable {

        private final Intent mIntent;

        public IntentRunnable(Intent intent) {
            mIntent = intent;
        }

        public void run() {
            onHandleIntent(mIntent);

            mHandler.removeCallbacks(mWorkDoneRunnable);
            mHandler.post(mWorkDoneRunnable);
        }
    }
}
