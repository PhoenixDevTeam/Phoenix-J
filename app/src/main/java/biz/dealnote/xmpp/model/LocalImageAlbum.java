package biz.dealnote.xmpp.model;

import android.content.ContentUris;
import android.net.Uri;

public class LocalImageAlbum {

    private int id;
    private String name;
    private long coverImageId;
    private String coverPath;
    private int photoCount;

    public int getId() {
        return id;
    }

    public LocalImageAlbum setId(int id) {
        this.id = id;
        return this;
    }

    public long getCoverId() {
        return coverImageId;
    }

    public LocalImageAlbum setCoverImageId(long coverImageId) {
        this.coverImageId = coverImageId;
        return this;
    }

    public String getName() {
        return name;
    }

    public LocalImageAlbum setName(String name) {
        this.name = name;
        return this;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public LocalImageAlbum setCoverPath(String coverPath) {
        this.coverPath = coverPath;
        return this;
    }

    public int getPhotoCount() {
        return photoCount;
    }

    public LocalImageAlbum setPhotoCount(int photoCount) {
        this.photoCount = photoCount;
        return this;
    }

    public Uri buildUriForPicasso() {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/images/media/"), coverImageId);
    }


    @Override
    public String toString() {
        return "LocalImageAlbum{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coverImageId=" + coverImageId +
                ", coverPath='" + coverPath + '\'' +
                ", photoCount=" + photoCount +
                '}';
    }
}
