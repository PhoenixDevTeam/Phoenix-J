package biz.dealnote.xmpp.util;

import android.graphics.BitmapFactory;
import android.net.Uri;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.ByteArrayInputStream;

import biz.dealnote.xmpp.db.interfaces.IContactsRepository;

/**
 * Created by admin on 24.04.2017.
 * phoenix-for-xmpp
 */
public class PicassoAvatarHandler extends RequestHandler {

    //avatars://hash/3h843384r4hh3

    private final IContactsRepository repository;

    public PicassoAvatarHandler(IContactsRepository repository) {
        this.repository = repository;
    }

    public static Uri generateUri(String hash) {
        return Uri.parse("avatars://hash/" + hash);
    }

    @Override
    public boolean canHandleRequest(Request data) {
        return data.uri != null && "avatars".equals(data.uri.getScheme());
    }

    @Override
    public Result load(Request request, int networkPolicy){
        String segment = request.uri.getLastPathSegment();

        byte[] photoBytes = repository.findPhotoByHash(segment);
        return new Result(BitmapFactory.decodeStream(new ByteArrayInputStream(photoBytes)), Picasso.LoadedFrom.DISK);
    }
}