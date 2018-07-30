package biz.dealnote.xmpp.callback;

import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public abstract class EndlessRecyclerOnScrollListener extends RecyclerView.OnScrollListener {

    private boolean allowLoading = true;
    private LinearLayoutManager manager;
    private Handler mHandler;

    public EndlessRecyclerOnScrollListener(LinearLayoutManager manager) {
        this.manager = manager;
        this.mHandler = new Handler();
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        if (!allowLoading) {
            return;
        }

        boolean need = analizeLinearManager(manager);

        if (need) {
            onLoadMore();
            allowLoading = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    allowLoading = true;
                }
            }, 1000);
        }
    }

    private boolean analizeLinearManager(LinearLayoutManager linearLayoutManager) {
        int visibleItemCount = linearLayoutManager.getChildCount();
        int totalItemCount = linearLayoutManager.getItemCount();
        int pastVisibleItems = linearLayoutManager.findFirstVisibleItemPosition();
        return (visibleItemCount + pastVisibleItems) >= totalItemCount;
    }

    public abstract void onLoadMore();
}