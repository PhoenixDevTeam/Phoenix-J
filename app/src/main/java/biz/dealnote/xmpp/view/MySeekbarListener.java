package biz.dealnote.xmpp.view;

import android.os.SystemClock;
import android.widget.SeekBar;

import java.util.EventListener;

public class MySeekbarListener implements SeekBar.OnSeekBarChangeListener {

    private long mLastSeekEventTime;
    private int mTargetPosition = -1;
    private boolean mFromTouch;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }

        mTargetPosition = progress;

        final long now = SystemClock.elapsedRealtime();
        if (now - mLastSeekEventTime > 250) {
            mLastSeekEventTime = now;

            if (mCallback != null) {
                mCallback.onProgressChangedFromTouch(mTargetPosition);
            }
        }
    }

    public int getTargetPosition() {
        return mTargetPosition;
    }

    private Callback mCallback;

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public interface Callback extends EventListener {
        void onProgressChangedFromTouch(int progress);

        void onStopMovingByUserOn(int progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mLastSeekEventTime = 0;
        mFromTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mTargetPosition != -1) {
            if (mCallback != null) {
                mCallback.onStopMovingByUserOn(mTargetPosition);
            }
        }

        mTargetPosition = -1;
        mFromTouch = false;
    }

    public boolean isTouchNow() {
        return mFromTouch;
    }
}