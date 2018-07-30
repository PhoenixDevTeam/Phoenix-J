/*
 * Copyright (c) 2014 Rex St. John on behalf of AirPair.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package biz.dealnote.xmpp.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.util.List;

import biz.dealnote.xmpp.model.LocalImageAlbum;

/**
 * AsyncTask loader used to load image resources in the background of a fragment.
 * Reference: http://developer.android.com/reference/android/content/AsyncTaskLoader.html
 * <p>
 * Created by Rex St. John (on behalf of AirPair.com) on 3/4/14.
 */
public class LocalImageAlbumsAsyncLoader extends AsyncTaskLoader<List<LocalImageAlbum>> {

    // Persistent list of photo list item
    private List<LocalImageAlbum> localImageAlbumList;

    public LocalImageAlbumsAsyncLoader(Context context) {
        super(context);
    }

    /**
     * Load photo album image items in the background
     * <p>
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override
    public List<LocalImageAlbum> loadInBackground() {
        final Context context = getContext();
        return PhotoGalleryImageProvider.getLocalImagesAlbums(context);
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(List<LocalImageAlbum> newPhotoListItems) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (newPhotoListItems != null) {
                onReleaseResources(newPhotoListItems);
            }
        }

        List<LocalImageAlbum> oldPhotos = localImageAlbumList;
        localImageAlbumList = newPhotoListItems;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(newPhotoListItems);
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldPhotos != null) {
            onReleaseResources(oldPhotos);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {

        if (localImageAlbumList != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(localImageAlbumList);
        } else {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<LocalImageAlbum> photoListItems) {
        super.onCanceled(photoListItems);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(photoListItems);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (localImageAlbumList != null) {
            onReleaseResources(localImageAlbumList);
            localImageAlbumList = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<LocalImageAlbum> photoListItems) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }
}