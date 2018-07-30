package biz.dealnote.xmpp.transfer;

import android.net.Uri;
import android.support.annotation.NonNull;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import biz.dealnote.xmpp.service.TransferNotFoundException;
import io.reactivex.Observable;

/**
 * Created by Ruslan Kolbasa on 25.04.2017.
 * phoenix-for-xmpp
 */
public interface IFileTransferer {

    void registerIncomeRequest(int messageId, FileTransferRequest request);

    void cancel(int messageId) throws TransferNotFoundException;

    void acceptIncomeRequest(int messageId, @NonNull File file) throws TransferNotFoundException, IOException, SmackException;

    FileTransferRequest findIncome(int messageId);

    void createOutgoingTransfer(int messageId, OutgoingFileTransfer transfer, @NonNull Uri uri, @NonNull String fileName, String mime) throws IOException;

    int getProgress(int messageId);

    Observable<List<IProgressValue>> observeProgress();

    interface IProgressValue {
        int getMessageId();
        int getProgress();
    }
}