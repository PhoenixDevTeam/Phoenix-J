package biz.dealnote.xmpp.model;

import android.net.Uri;

/**
 * Created by admin on 11.11.2016.
 * phoenix-for-xmpp
 */
public class MessageUpdate {

    private StatusUpdate statusUpdate;
    private FileUriUpdate fileUriUpdate;

    public MessageUpdate setFileUriUpdate(FileUriUpdate fileUriUpdate) {
        this.fileUriUpdate = fileUriUpdate;
        return this;
    }

    public MessageUpdate setStatusUpdate(StatusUpdate statusUpdate) {
        this.statusUpdate = statusUpdate;
        return this;
    }

    public FileUriUpdate getFileUriUpdate() {
        return fileUriUpdate;
    }

    public StatusUpdate getStatusUpdate() {
        return statusUpdate;
    }

    public static MessageUpdate simpleStatusChange(int status) {
        return new MessageUpdate().setStatusUpdate(new StatusUpdate(status));
    }

    public static class FileUriUpdate {

        private final Uri uri;

        public FileUriUpdate(Uri uri) {
            this.uri = uri;
        }

        public Uri getUri() {
            return uri;
        }
    }

    public static class StatusUpdate {

        private final int status;

        public StatusUpdate(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

}
