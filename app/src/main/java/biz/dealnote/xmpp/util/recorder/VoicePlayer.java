package biz.dealnote.xmpp.util.recorder;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Created by admin on 09.10.2016.
 * phoenix
 */
public class VoicePlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private int mStatus;
    private MediaPlayer mPlayer;
    private Callback mCallback;

    public boolean toggle(@NonNull Context context, int id, @NonNull Uri path) throws PrepareException {
        if (mPlayingEntry != null && mPlayingEntry.getId() == id) {
            setSupposedToPlay(!isSupposedToPlay());
            return false;
        }

        stop();

        mPlayingEntry = new Entry(id, path);
        mPlayer = new MediaPlayer();

        try {
            mPlayer.setDataSource(context, path);
        } catch (IOException e) {
            throw new PrepareException();
        }

        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnCompletionListener(this);

        mSupposedToPlay = true;
        changeStatusTo(Status.PREPARING);

        //mPlayer.prepareAsync();

        try {
            mPlayer.prepare();
            changeStatusTo(Status.PREPARED);
        } catch (IOException e) {
            throw new PrepareException();
        }

        mPlayer.start();
        return true;
    }

    public void seekTo(int position) {
        mPlayer.seekTo(position);
    }

    public int getDuration() {
        return mPlayer != null ? mPlayer.getDuration() : -1;
    }

    public int getPosition() {
        return mPlayer != null ? mPlayer.getCurrentPosition() : -1;
    }

    public void setCallback(@Nullable Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mp != mPlayer) return;
        changeStatusTo(Status.PREPARED);

        if (mSupposedToPlay) {
            mPlayer.start();
        }
    }

    public void stop() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }

        changeStatusTo(Status.NO_PLAYBACK);
    }

    public void release() {
        stop();
    }

    private void changeStatusTo(int status) {
        if (mStatus == status) {
            return;
        }

        mStatus = status;

        if (mCallback != null) {
            mCallback.onPlayerStatusChange(status);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp != mPlayer) {
            return;
        }

        mSupposedToPlay = false;
    }

    public class PrepareException extends Exception {

    }

    public boolean isSupposedToPlay() {
        return mSupposedToPlay;
    }

    private void setSupposedToPlay(boolean supposedToPlay) {
        if (supposedToPlay == mSupposedToPlay) {
            return;
        }

        this.mSupposedToPlay = supposedToPlay;

        if (mStatus == Status.PREPARED) {
            if (supposedToPlay) {
                mPlayer.start();
            } else {
                mPlayer.pause();
            }
        }
    }

    private static class Entry {

        int id;
        Uri audio;

        Entry(int id, @NonNull Uri audio) {
            this.id = id;
            this.audio = audio;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return id == entry.id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        public int getId() {
            return id;
        }
    }

    private Entry mPlayingEntry;

    public Integer getPlayingVoiceId() {
        return mPlayingEntry == null ? null : mPlayingEntry.getId();
    }

    private boolean mSupposedToPlay;

    public interface Callback {
        void onPlayerStatusChange(int status);
    }

    private static final class Status {
        static final int NO_PLAYBACK = 0;
        static final int PREPARING = 1;
        static final int PREPARED = 2;
    }
}
