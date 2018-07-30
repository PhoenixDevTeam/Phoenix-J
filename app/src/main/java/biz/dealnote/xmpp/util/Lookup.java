package biz.dealnote.xmpp.util;

import android.os.Handler;

/**
 * Created by ruslan.kolbasa on 04.10.2016.
 * phoenix
 */
public class Lookup {

    private static final int LOOKUP = 1540;

    private Handler mHandler;

    private int mDelay;

    public interface Callback {
        void onIterated();
    }

    private Callback mCallback;

    public Lookup(int initialDelay) {
        mDelay = initialDelay;
        mHandler = new Handler(msg -> {
            onLookupHandle();
            return true;
        });
    }

    public Callback getCallback() {
        return mCallback;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    private void onLookupHandle(){
        mHandler.sendEmptyMessageDelayed(LOOKUP, mDelay);

        if(mCallback != null){
            mCallback.onIterated();
        }
    }

    public void changeDelayTime(int delay, boolean startNow){
        mDelay = delay;
        if(startNow){
            mHandler.removeMessages(LOOKUP);
            mHandler.sendEmptyMessageDelayed(LOOKUP, mDelay);
        }
    }

    public void stop(){
        mHandler.removeMessages(LOOKUP);
    }

    public void start(){
        if(!mHandler.hasMessages(LOOKUP)){
            mHandler.sendEmptyMessageDelayed(LOOKUP, mDelay);
        }
    }
}