package biz.dealnote.xmpp.transfer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import biz.dealnote.xmpp.Injection;
import biz.dealnote.xmpp.db.interfaces.IMessagesRepository;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.model.MessageUpdate;
import biz.dealnote.xmpp.service.TransferNotFoundException;
import biz.dealnote.xmpp.util.Logger;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by Ruslan Kolbasa on 25.04.2017.
 * phoenix-for-xmpp
 */
public class FileTransferer implements IFileTransferer {

    private static final String TAG = FileTransferer.class.getSimpleName();

    private static final long PROGRESS_LOOKUP_DELAY = 500;

    private final Context context;
    private SparseArray<FileTransferRequest> incomeRequests;
    private List<Entry> activeTransfers;
    private PublishSubject<List<IProgressValue>> progressPublisher;
    private IMessagesRepository repository;

    private Observable<Long> repeater;
    private Disposable repeatDisposable;

    public FileTransferer(Context context, IMessagesRepository repository) {
        this.context = context.getApplicationContext();
        this.repository = repository;
        this.incomeRequests = new SparseArray<>(1);
        this.activeTransfers = new LinkedList<>();
        this.progressPublisher = PublishSubject.create();
        this.repeater = Observable.interval(0L, PROGRESS_LOOKUP_DELAY, TimeUnit.MILLISECONDS, Injection.INSTANCE.provideMainThreadScheduler());
    }

    @Override
    public void registerIncomeRequest(int messageId, FileTransferRequest request) {
        incomeRequests.put(messageId, request);
        Logger.d(TAG, "Registered income request, mid: " + messageId);
    }

    @Override
    public void cancel(int messageId) throws TransferNotFoundException {
        FileTransferRequest request = incomeRequests.get(messageId);
        if(request != null){
            try {
                request.reject();
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                Logger.d(TAG, "Cancelled income, mid: " + messageId);
                updateMessage(messageId, MessageUpdate.simpleStatusChange(AppMessage.STATUS_CANCELLED));
            }

            return;
        }

        Entry entry = findByMessageId(messageId);
        if(entry != null){
            try {
                entry.cancel();
            } finally {
                Logger.d(TAG, "Cancelled in-progress, mid: " + messageId);
                updateMessage(messageId, MessageUpdate.simpleStatusChange(AppMessage.STATUS_CANCELLED));
            }
            return;
        }

        throw new TransferNotFoundException();
    }

    @Override
    public void acceptIncomeRequest(int messageId, @NonNull File file) throws TransferNotFoundException, IOException, SmackException {
        FileTransferRequest request = incomeRequests.get(messageId);
        if (request == null) {
            throw new TransferNotFoundException();
        }

        incomeRequests.remove(messageId);

        IncomingFileTransfer fileTransfer = request.accept();

        Entry entry = new Entry(messageId, fileTransfer, Uri.fromFile(file), true);
        activeTransfers.add(entry);

        try {
            fileTransfer.recieveFile(file);
        } catch (Exception e) {
            activeTransfers.remove(messageId);
            throw e;
        }

        Logger.d(TAG, "Income accepted, mid: " + messageId);
        checkLookupState();
    }

    @Override
    public FileTransferRequest findIncome(int messageId) {
        return incomeRequests.get(messageId);
    }

    @Override
    public void createOutgoingTransfer(int messageId, OutgoingFileTransfer transfer, @NonNull Uri uri, @NonNull String fileName, String mime) throws IOException {
        Entry entry = new Entry(messageId, transfer, uri, false);

        activeTransfers.add(entry);

        try {
            updateMessage(messageId, MessageUpdate.simpleStatusChange(AppMessage.STATUS_IN_PROGRESS));

            InputStream is = context.getContentResolver().openInputStream(uri);

            if (is != null) {
                int available = is.available();
                transfer.sendStream(is, fileName, available, mime);
            } else {
                throw new IOException("Unable to open stream");
            }
        } catch (IOException e) {
            updateMessage(messageId, MessageUpdate.simpleStatusChange(AppMessage.STATUS_CANCELLED));
            activeTransfers.remove(messageId);
            throw e;
        }

        Logger.d(TAG, "Created outgoing, mid: " + messageId);
        checkLookupState();
    }

    @Override
    public int getProgress(int messageId) {
        Entry entry = findByMessageId(messageId);
        return entry != null ? entry.intProgress() : 0;
    }

    private void updateMessage(int messageId, MessageUpdate update){
        repository.updateMessage(messageId, update)
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    @Nullable
    private Entry findByMessageId(int messageId) {
        for (Entry entry : activeTransfers) {
            if (entry.messageId == messageId) {
                return entry;
            }
        }

        return null;
    }

    private void onLookupIteration() {
        Logger.d(TAG, "Lookup iteration");

        Iterator<Entry> iterator = activeTransfers.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();

            if (entry.transfer.isDone()) {
                onFileTransferDone(entry);
                iterator.remove();
            }
        }

        publishProgress();
        checkLookupState();
    }

    private void onFileTransferDone(Entry entry) {
        Logger.d(TAG, "Done, mid: " + entry.messageId);

        final FileTransfer transfer = entry.transfer;
        final int messageId = entry.messageId;

        boolean success = transfer.getProgress() >= 1.0D;

        if (success) {
            MessageUpdate update = new MessageUpdate()
                    .setStatusUpdate(new MessageUpdate.StatusUpdate(AppMessage.STATUS_DONE));

            if (entry.income) {
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, entry.file));

                update.setFileUriUpdate(new MessageUpdate.FileUriUpdate(entry.file));
            }

            updateMessage(messageId, update);
        } else {
            if (entry.income) {
                File file = new File(entry.file.getPath());
                if (file.delete()) {
                    Logger.d(TAG, "file was deleted, mid: " + messageId);
                }
            }

            updateMessage(messageId, MessageUpdate.simpleStatusChange(AppMessage.STATUS_CANCELLED));

            Logger.d(TAG, "transfer was cancelled !!!, mid: " + messageId);
        }
    }

    private void startLookup() {
        if (repeatDisposable == null || repeatDisposable.isDisposed()) {
            repeatDisposable = repeater.subscribe(aLong -> onLookupIteration());
        }
    }

    private void stopLookup() {
        if (repeatDisposable != null) {
            if (!repeatDisposable.isDisposed()) {
                repeatDisposable.dispose();
            }

            repeatDisposable = null;
        }
    }

    private void checkLookupState() {
        if (activeTransfers.isEmpty()) {
            stopLookup();
        } else {
            startLookup();
        }
    }

    @Override
    public Observable<List<IProgressValue>> observeProgress() {
        return progressPublisher;
    }

    private void publishProgress() {
        List<IProgressValue> values;

        // optimize
        if (activeTransfers.size() == 0) {
            values = Collections.emptyList();
        } else if (activeTransfers.size() == 1) {
            values = Collections.singletonList(createProgressValue(activeTransfers.get(0)));
        } else {
            values = new ArrayList<>(activeTransfers.size());
            for (Entry entry : activeTransfers) {
                values.add(createProgressValue(entry));
            }
        }

        progressPublisher.onNext(values);
    }

    private static IProgressValue createProgressValue(Entry entry) {
        return new ProgressValue(entry.messageId, entry.intProgress());
    }

    private static class Entry {

        final FileTransfer transfer;

        final int messageId;

        final Uri file;

        final boolean income;

        boolean cancelling;

        private Entry(int messageId, FileTransfer transfer, Uri file, boolean income) {
            this.transfer = transfer;
            this.messageId = messageId;
            this.file = file;
            this.income = income;
        }

        void cancel(){
            if(cancelling){
                return;
            }

            cancelling = true;
            transfer.cancel();
        }

        int intProgress() {
            return (int) (transfer.getProgress() * 100);
        }
    }

    private static class ProgressValue implements IProgressValue {

        final int messageId;

        final int progress;

        ProgressValue(int messageId, int progress) {
            this.messageId = messageId;
            this.progress = progress;
        }

        @Override
        public int getMessageId() {
            return messageId;
        }

        @Override
        public int getProgress() {
            return progress;
        }
    }
}