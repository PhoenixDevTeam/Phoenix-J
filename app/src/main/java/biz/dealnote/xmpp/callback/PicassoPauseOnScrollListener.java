package biz.dealnote.xmpp.callback;

import android.support.annotation.NonNull;

import androidx.recyclerview.widget.RecyclerView;
import biz.dealnote.xmpp.util.PicassoInstance;

public class PicassoPauseOnScrollListener extends RecyclerView.OnScrollListener {

    private String tag;

    public PicassoPauseOnScrollListener(String tag) {
        this.tag = tag;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            PicassoInstance.get().resumeTag(tag);
        } else {
            PicassoInstance.get().pauseTag(tag);
        }
    }
}