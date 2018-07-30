package biz.dealnote.xmpp.loader;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.xmpp.db.AppRoster;
import biz.dealnote.xmpp.db.ChatContentProvider;
import biz.dealnote.xmpp.model.AppRosterEntry;

public class RosterEntriesAsyncLoader extends AsyncTaskLoader<ArrayList<AppRosterEntry>> {

    private static final String EXTRA_ORDER_BY = "order_by";
    private ArrayList<AppRosterEntry> mData;
    private ContentObserver observer;
    private String orderBy;

    public RosterEntriesAsyncLoader(@NonNull Context context, Bundle args) {
        super(context);
        this.orderBy = args.getString(EXTRA_ORDER_BY);
        observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                forceLoad();
            }
        };
    }

    public static Bundle createArgs(String orderBy) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ORDER_BY, orderBy);
        return bundle;
    }

    @Override
    public ArrayList<AppRosterEntry> loadInBackground() {
        return AppRoster.getAllRosterEntries(getContext(), orderBy);
    }

    @Override
    public void deliverResult(ArrayList<AppRosterEntry> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            releaseResources(data);
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<AppRosterEntry> oldData = mData;
        mData = data;

        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            releaseResources(oldData);
        }
    }

    /*********************************************************/
    /** (3) Implement the Loader’s state-dependent behavior **/
    /*********************************************************/

    @Override
    protected void onStartLoading() {
        getContext().getContentResolver().registerContentObserver(ChatContentProvider.ROSTERS_ENTRIES_CONTENT_URI, true, observer);
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData);
        }

        if (takeContentChanged() || mData == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }

    @Override
    protected void onReset() {
        getContext().getContentResolver().unregisterContentObserver(observer);
        // Ensure the loader has been stopped.
        onStopLoading();

        // At this point we can release the resources associated with 'mData'.
        if (mData != null) {
            releaseResources(mData);
            mData = null;
        }
    }

    @Override
    public void onCanceled(ArrayList<AppRosterEntry> data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(List<AppRosterEntry> data) {
        // For a simple List, there is nothing to do. For something like a Cursor, we
        // would close it in this method. All resources associated with the Loader
        // should be released here.
    }
}
