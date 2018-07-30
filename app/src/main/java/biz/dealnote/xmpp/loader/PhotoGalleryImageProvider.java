package biz.dealnote.xmpp.loader;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import biz.dealnote.xmpp.model.LocalImageAlbum;
import biz.dealnote.xmpp.model.LocalPhoto;

/**
 * This is a helper utility which automatically fetches paths to full size and thumbnail sized gallery images.
 * <p>
 * Created by Rex St. John (on behalf of AirPair.com) on 3/4/14.
 */
public class PhotoGalleryImageProvider {

    /**
     * Fetch both full sized images and thumbnails via a single query.
     * Returns all images not in the Camera Roll.
     */
    @Nullable
    public static List<LocalPhoto> getAlbumThumbnails(Context context, long bucketID) {
        final String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, MediaStore.Images.Media.BUCKET_ID + " = ?",
                new String[]{String.valueOf(bucketID)},
                MediaStore.Images.ImageColumns.DATE_ADDED + " DESC");
        if (cursor == null) {
            return null;
        }

        ArrayList<LocalPhoto> result = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            LocalPhoto newItem = new LocalPhoto(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID)),
                    Uri.parse(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))));
            result.add(newItem);
        }

        cursor.close();
        return result;
    }

    public static LocalPhoto load(Context context, Uri uri) {
        final String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
        Uri targetUri = getImageUrlWithAuthority(context, uri);
        if (targetUri == null) {
            return null;
        }

        Cursor cursor = context.getContentResolver().query(targetUri, projection, null, null, null);
        if (cursor == null) {
            return null;
        }

        LocalPhoto photo = null;
        if (cursor.moveToNext()) {
            photo = new LocalPhoto(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID)),
                    Uri.parse(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))));
        }

        cursor.close();
        return photo;
    }

    public static String getRealPathFromURI(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        String result = null;

        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
            }

            cursor.close();
        }

        return result;
    }

    public static Uri getImageUrlWithAuthority(Context context, Uri uri) {
        InputStream is = null;
        if (uri.getAuthority() != null) {
            try {
                is = context.getContentResolver().openInputStream(uri);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                return writeToTempImageAndGetPathUri(context, bmp);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static Uri writeToTempImageAndGetPathUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public static Bitmap getThumbnail(Context context, long imageId) {
        return MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(),
                imageId, MediaStore.Images.Thumbnails.MINI_KIND, null);
    }

    public static List<LocalImageAlbum> getLocalImagesAlbums(Context context) {
        List<LocalImageAlbum> localImageAlbums = new ArrayList<>();
        final String album = MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME;
        final String albumId = MediaStore.Images.ImageColumns.BUCKET_ID;
        final String data = MediaStore.Images.ImageColumns.DATA;
        final String coverID = MediaStore.Images.ImageColumns._ID;
        String[] projection = new String[]{album, albumId, data, coverID, "COUNT(" + coverID + ")"};

        String selection = "1=1) GROUP BY (" + MediaStore.Images.ImageColumns.BUCKET_ID;

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, MediaStore.Images.ImageColumns.DATE_ADDED + " desc");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                LocalImageAlbum localImageAlbum = new LocalImageAlbum();
                localImageAlbums.add(localImageAlbum
                        .setId(cursor.getInt(1))
                        .setName(cursor.getString(0))
                        .setCoverPath(cursor.getString(2))
                        .setCoverImageId(cursor.getLong(3))
                        .setPhotoCount(cursor.getInt(4)));
            }

            cursor.close();
        }

        return localImageAlbums;
    }

    /**
     * Get the path to the full image for a given thumbnail.
     */
    private static Uri uriToFullImage(long imageID, Context context) {
        // Request image related to this thumbnail
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor imagesCursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, filePathColumn, MediaStore.Images.Media._ID + " = " + imageID, null, null);
        if (imagesCursor != null && imagesCursor.moveToFirst()) {
            int columnIndex = imagesCursor.getColumnIndex(filePathColumn[0]);
            String filePath = imagesCursor.getString(columnIndex);
            imagesCursor.close();
            return Uri.parse(filePath);
        } else {
            if (imagesCursor != null) {
                imagesCursor.close();
            }
            return Uri.parse("");
        }
    }

    /**
     * Matches code in MediaProvider.computeBucketValues. Should be a common
     * function.
     */
    public static String getBucketId(String path) {
        return String.valueOf(path.toLowerCase().hashCode());
    }
}
