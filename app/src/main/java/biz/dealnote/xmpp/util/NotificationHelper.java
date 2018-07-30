package biz.dealnote.xmpp.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.squareup.picasso.Transformation;

import java.io.IOException;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.activity.MainActivity;
import biz.dealnote.xmpp.db.Repositories;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.place.AppPlace;
import biz.dealnote.xmpp.place.PlaceFactory;
import biz.dealnote.xmpp.settings.NotificationSettings;

import static biz.dealnote.xmpp.util.Utils.dpToPx;
import static biz.dealnote.xmpp.util.Utils.nonEmpty;

public class NotificationHelper {

    public static final String TAG = NotificationHelper.class.getSimpleName();
    public static final String KEY_INCOMING_MESSAGES = "incoming_messages";
    public static final String KEY_NEW_SUBSCRIPTIONS = "new_subscriptions";
    public static final String KEY_INCOMING_FILES = "incoming_files";

    public static final String SUBKEY_ENABLE = "_enable";
    public static final String SUBKEY_VIBRATION = "_vibro";
    public static final String SUBKEY_LIGHT = "_light";
    public static final String SUBKEY_SOUND = "_sound";
    private static final String SUBKEY_RINGTONE_URI = "_ringtone_uri";
    private static final int NOTIFICATION_INCOME_MESSAGE = 12;
    private static final int LARGE_ICON_SIZE_DP = 64;

    public static NotificationSettings.Value load(Context context, String nKey) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        return new NotificationSettings.Value()
                .setEnable(preferences.getBoolean(nKey + SUBKEY_ENABLE, true))
                .setLight(preferences.getBoolean(nKey + SUBKEY_LIGHT, true))
                .setSound(preferences.getBoolean(nKey + SUBKEY_SOUND, true))
                .setVibro(preferences.getBoolean(nKey + SUBKEY_VIBRATION, true))
                .setUri(Uri.parse(preferences.getString(nKey + SUBKEY_RINGTONE_URI, getDefIncomingRingtoneUri(context))));
    }

    private static String getDefIncomingRingtoneUri(Context context) {
        return "android.resource://" + context.getPackageName() + "/" + R.raw.incoming;
    }

    public static String getOutgoingRingtoneUri(Context context) {
        return "android.resource://" + context.getPackageName() + "/" + R.raw.outgoing;
    }

    public static void write(Context context, String key, String subkey, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(key + subkey, value)
                .apply();
    }

    public static void notifyAboutNewMessage(Context context, final AppMessage message) {
        final long start = System.currentTimeMillis();

        // получаем информацию об отправителе и его аватар в отдельном потоке,
        // после успешного получения отображаем уведомление

        new AvatarFetchTask(context) {
            @Override
            protected void onPostExecute(Entry entry) {
                Log.d(TAG, "notifyAboutNewMessage, time: " + (System.currentTimeMillis() - start) + " ms");
                notifyAboutNewMessage(context, message, entry);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message.getSenderJid());
    }

    private static void notifyAboutNewMessage(Context context, AppMessage message, Entry entry) {
        NotificationSettings.Value value;

        switch (message.getType()) {
            case AppMessage.TYPE_NORMAL:
            case AppMessage.TYPE_CHAT:
            case AppMessage.TYPE_GROUP_CHAT:
            case AppMessage.TYPE_HEADLINE:
            case AppMessage.TYPE_ERROR:
                value = NotificationHelper.load(context, NotificationHelper.KEY_INCOMING_MESSAGES);
                break;

            case AppMessage.TYPE_INCOME_FILE:
            case AppMessage.TYPE_OUTGOING_FILE:
                value = NotificationHelper.load(context, NotificationHelper.KEY_INCOMING_FILES);
                break;

            case AppMessage.TYPE_SUBSCRIBE:
            case AppMessage.TYPE_SUBSCRIBED:
            case AppMessage.TYPE_UNSUBSCRIBE:
            case AppMessage.TYPE_UNSUBSCRIBED:
                value = NotificationHelper.load(context, NotificationHelper.KEY_NEW_SUBSCRIPTIONS);
                break;

            default:
                throw new IllegalArgumentException("Unknown message type: " + message.getType());
        }

        String fullBody = message.getMessageBody(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notify_income_message)
                .setLargeIcon(entry.avatar)
                .setContentTitle(entry.contact == null ? message.getSenderJid() : entry.contact.getDispayName())
                .setContentText(fullBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(fullBody))
                .setAutoCancel(true);

        Intent intent = new Intent(context, MainActivity.class);
        AppPlace place = PlaceFactory.getChatPlace(message.getAccountId(), message.getDestination(), message.getChatId());
        intent.setAction(MainActivity.ACTION_OPEN_PLACE);
        intent.putExtra(Extra.PLACE, place);

        PendingIntent contentIntent = PendingIntent.getActivity(context, message.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(contentIntent);

        final NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = builder.build();

        if (value.light) {
            notification.ledARGB = 0xFF0000FF;
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            notification.ledOnMS = 100;
            notification.ledOffMS = 1000;
        }

        if (value.vibro) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }

        if (value.sound) {
            notification.sound = value.uri;
        }

        nManager.notify(message.getDestination(), NOTIFICATION_INCOME_MESSAGE, notification);
    }

    public static void cancelNotifyForDestination(Context context, String dest) {
        NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(dest, NOTIFICATION_INCOME_MESSAGE);
    }

    private static class Entry {
        Contact contact;
        Bitmap avatar;
    }

    /**
     * Асинхронная загрузка информации об пользователе
     * Загружаем аватар и полную информацию из БД
     */
    private static class AvatarFetchTask extends AsyncTask<String, Void, Entry> {

        Context context;
        Transformation transformation;

        AvatarFetchTask(Context context) {
            this.context = context.getApplicationContext();
            this.transformation = new RoundTransformation();
        }

        @Override
        protected Entry doInBackground(String... params) {
            String jid = params[0];
            Contact contact = Repositories.getInstance()
                    .getContactsRepository()
                    .findByJid(jid)
                    .blockingGet()
                    .get();

            if (contact == null) {
                return null;
            }

            Entry entry = new Entry();
            entry.contact = contact;

            if(nonEmpty(contact.getPhotoHash())){
                int size = (int) dpToPx(LARGE_ICON_SIZE_DP, context);
                try {
                    entry.avatar = PicassoInstance.get()
                            .load(PicassoAvatarHandler.generateUri(contact.getPhotoHash()))
                            .resize(size, size)
                            .transform(transformation)
                            .get();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            return entry;
        }
    }
}
