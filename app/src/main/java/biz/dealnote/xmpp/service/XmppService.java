package biz.dealnote.xmpp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jxmpp.jid.Jid;

import java.util.Collection;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.Injection;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.activity.MainActivity;
import biz.dealnote.xmpp.db.AppRoster;
import biz.dealnote.xmpp.db.Messages;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.db.exception.AlreadyExistException;
import biz.dealnote.xmpp.model.Account;
import biz.dealnote.xmpp.model.AppFile;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.model.MessageBuilder;
import biz.dealnote.xmpp.model.User;
import biz.dealnote.xmpp.security.IOtrManager;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestAdapter;
import biz.dealnote.xmpp.service.request.RequestFactory;
import biz.dealnote.xmpp.service.request.XmppOperationManager;
import biz.dealnote.xmpp.transfer.IFileTransferer;
import biz.dealnote.xmpp.util.ExtensionsKt;
import biz.dealnote.xmpp.util.Logger;
import biz.dealnote.xmpp.util.NotificationHelper;
import biz.dealnote.xmpp.util.RxUtils;
import biz.dealnote.xmpp.util.Unixtime;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class XmppService extends Service implements IXmppContext {

    public static final String ACTION_EXECUTE_REQUEST = "biz.dealnote.xmpp.service.XmppService.ACTION_EXECUTE_REQUEST";

    private static final String TAG = XmppService.class.getSimpleName();
    private static final int FOREGROUND_SERVICE = 15;

    static {
        try {
            Class.forName("org.jivesoftware.smack.ReconnectionManager");
        } catch (ClassNotFoundException ex) {
            // problem loading reconnection manager
        }
    }

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private XmppOperationManager mXmppOperationManager;
    private RequestAdapter mRequestAdapter;

    private IConnectionManager mConnectionManager;
    private IOtrManager mOtrManager;
    private IFileTransferer fileTransferer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_EXECUTE_REQUEST.equals(action)) {
                mXmppOperationManager.queue(intent);
            }
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        this.fileTransferer = Injection.INSTANCE.provideTransferer();
        this.mConnectionManager = Injection.INSTANCE.provideConnectionManager();
        this.mOtrManager = Injection.INSTANCE.provideOtrManager();

        mRequestAdapter = new RequestAdapter() {
            @Override
            public void onRequestFinished(Request request, Bundle resultData) {
                if (request.getRequestType() == RequestFactory.REQUEST_GET_VCARD) {
                    User user = resultData.getParcelable(Extra.CONTACT);
                    Log.d(TAG, "VCard was loaded: " + user);
                }
            }
        };

        this.mXmppOperationManager = new XmppOperationManager(this, this);

        appendDisposable(mConnectionManager.observeNewMessages()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(action -> onIncomeMessageReceived(action.getAccount(), action.getData())));

        appendDisposable(mConnectionManager.observeIncomeFileRequests()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(action -> onIncomeFile(action.getAccount().getId(), action.getData())));

        appendDisposable(mConnectionManager.observeRosterAdding()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(action -> onRosterEntryAdded(action.getAccount().getId(), action.getData())));

        appendDisposable(mConnectionManager.observeRosterUpdates()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(action -> onRosterEntryUpdated(action.getAccount().getId(), action.getData())));

        appendDisposable(mConnectionManager.observeRosterDetetions()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(action -> onRosterEntryDeleted(action.getAccount().getId(), action.getData())));

        appendDisposable(mConnectionManager.observePresenses()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(action -> onNewPresence(action.getAccount().getId(), action.getData())));

        appendDisposable(mConnectionManager.observeRosterPresenses()
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler())
                .subscribe(action -> onPresenceChanged(action.getAccount().getId(), action.getData())));

        startWithNotification();
    }

    private void onIncomeFile(int accountId, FileTransferRequest request) {
        AppFile file = new AppFile(null, request.getFileName(), request.getFileSize())
                .setMime(request.getMimeType())
                .setDescription(request.getDescription());

        MessageBuilder builder = new MessageBuilder(accountId)
                .setDestination(request.getRequestor().asBareJid().toString())
                .setSenderJid(request.getRequestor().asBareJid().toString())
                .setType(AppMessage.TYPE_INCOME_FILE)
                .setStatus(AppMessage.STATUS_WAITING_FOR_REASON)
                .setAppFile(file)
                .setUniqueServiceId(request.getStreamID())
                .setOut(false)
                .setReadState(false)
                .setDate(Unixtime.now());

        Repositories.getInstance()
                .getMessages()
                .saveMessage(builder)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .doOnSuccess(message -> fileTransferer.registerIncomeRequest(message.getId(), request))
                .subscribe(this::notifyAboutNewMessage);
    }

    private void onIncomeMessageReceived(Account account, Message message) {
        String body = message.getBody();
        String from = message.getFrom().asBareJid().toString();
        if (TextUtils.isEmpty(body)) {
            return;
        }

        String encryptedBody = mOtrManager.handleInputMessage(account, from, body);
        if (TextUtils.isEmpty(encryptedBody)) {
            return;
        }

        boolean encrypted = !encryptedBody.equals(body);
        int type = Messages.getAppTypeFrom(message.getType());

        // Сохраняем сообщение в базу в отдельном потоке
        // и потом создаем уведомление о входящем сообщении
        MessageBuilder builder = new MessageBuilder(account.getId())
                .setDestination(from)
                .setSenderJid(from)
                .setType(type)
                .setBody(encryptedBody)
                .setDate(Unixtime.now())
                .setOut(false)
                .setReadState(false)
                .setStatus(AppMessage.STATUS_SENT)
                .setUniqueServiceId(message.getStanzaId())
                .setWasEncrypted(encrypted);

        Repositories.getInstance()
                .getMessages()
                .saveMessage(builder)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::notifyAboutNewMessage, this::onNewInputNormalMessageSaveError);
    }

    private void onNewInputNormalMessageSaveError(Throwable throwable) {
        if (throwable instanceof AlreadyExistException) {
            // ignore
            return;
        }

        Toast.makeText(this, throwable.toString(), Toast.LENGTH_LONG).show();
    }

    private void onPresenceChanged(int accountId, Presence presence) {
        AppRoster.mergeNewPresence(this, accountId, presence);
    }

    private void onNewPresence(int accountId, Presence presence) {
        Integer appTargetType = Messages.appPresenseTypeFromApi(presence.getType());
        if (appTargetType == null) {
            Log.d(TAG, "onNewPresence, appTargetType is unknown, apitype: " + presence.getType());
            return;
        }

        String from = presence.getFrom().asBareJid().toString();
        MessageBuilder builder = new MessageBuilder(accountId)
                .setType(appTargetType)
                .setDate(Unixtime.now())
                .setDestination(from)
                .setSenderJid(from)
                .setOut(false);

        Repositories.getInstance()
                .getMessages()
                .saveMessage(builder)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::notifyAboutNewMessage);
    }

    private void notifyAboutNewMessage(@NonNull AppMessage message) {
        NotificationHelper.notifyAboutNewMessage(XmppService.this, message);
    }

    private void onRosterEntryAdded(int accountId, Collection<RosterEntry> entries) {
        Logger.d("onRosterEntryAdded", "count: " + entries);

        ExtensionsKt.subscribeIOAndIgnoreResults(Injection.INSTANCE.proviceContactsRepository().handleContactsAdded(accountId, entries));

        //ArrayList<String> bareJids = new ArrayList<>(entries.size());
        //for (RosterEntry entry : entries) {
        //    AppRoster.mergeRosterEntry(this, accountId, entry);
        //    bareJids.add(entry.getJid().asBareJid().toString());
        //}

        //StringArray array = new StringArray(bareJids.toArray(new String[bareJids.size()]));

        //Request request = RequestFactory.getVcardRequest(accountId, array);
        //XmppRequestManager.from(this).execute(request, mRequestAdapter);
    }

    private void onRosterEntryUpdated(int accountId, Collection<RosterEntry> entries) {
        for (RosterEntry entry : entries) {
            AppRoster.mergeRosterEntry(this, accountId, entry);
        }
    }

    private void onRosterEntryDeleted(int accountId, Collection<Jid> jids) {
        ExtensionsKt.subscribeIOAndIgnoreResults(Injection.INSTANCE.proviceContactsRepository().handleContactsDeleted(accountId, jids));

        //for (Jid jid : jids) {
        //    AppRoster.deleteRosterEntry(this, accountId, jid.asBareJid().toString());
        //}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startWithNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("foreground_service", "Foreground Service", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, "foreground_service")
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getClass().getSimpleName())
                .setSmallIcon(R.drawable.ic_foreground_icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(FOREGROUND_SERVICE, notification);
    }

    @Override
    public void onDestroy() {
        mCompositeDisposable.dispose();
        mXmppOperationManager.shutdown();

        stopForeground(true);
        super.onDestroy();
    }

    @NonNull
    @Override
    public IConnectionManager getConnectionManager() {
        return mConnectionManager;
    }

    @NonNull
    @Override
    public IOtrManager getOtrManager() {
        return mOtrManager;
    }

    private void appendDisposable(Disposable disposable) {
        mCompositeDisposable.add(disposable);
    }
}