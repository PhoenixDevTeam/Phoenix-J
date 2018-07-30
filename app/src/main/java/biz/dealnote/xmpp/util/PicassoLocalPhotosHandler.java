package biz.dealnote.xmpp.util;

import android.content.Context;
import android.graphics.Bitmap;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;

import biz.dealnote.xmpp.loader.PhotoGalleryImageProvider;

public class PicassoLocalPhotosHandler extends RequestHandler {

    private Context mContext;

    public PicassoLocalPhotosHandler(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        //content://media/external/images/media/7775
        return data.uri != null
                && data.uri.getScheme() != null
                && data.uri.getScheme().equals("content");
    }

    @Override
    public Result load(Request data, int networkPolicy) throws IOException {
        //Log.d(TAG, "load, data: " + data);

        long imageId = Long.parseLong(data.uri.getLastPathSegment());

        Bitmap bm = PhotoGalleryImageProvider.getThumbnail(mContext, imageId);
        return new RequestHandler.Result(bm, Picasso.LoadedFrom.DISK);
    }
}
