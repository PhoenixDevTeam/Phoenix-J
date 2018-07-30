package biz.dealnote.xmpp.callback;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import biz.dealnote.xmpp.util.PicassoInstance;

public class PicassoPauseOnScrollListener extends RecyclerView.OnScrollListener {

    private String tag;
    private Context context;

    public PicassoPauseOnScrollListener(Context context, String tag) {
        this.tag = tag;
        this.context = context.getApplicationContext();
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            PicassoInstance.get().resumeTag(tag);
        } else {
            PicassoInstance.get().pauseTag(tag);
        }
    }
}
