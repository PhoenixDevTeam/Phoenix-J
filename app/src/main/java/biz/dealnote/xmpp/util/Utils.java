package biz.dealnote.xmpp.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.model.Identificable;

public class Utils {

    private static final String UTILS_TAG = Utils.class.getCanonicalName();

    private static final String DIVIDER = "/";
    private static final SimpleDateFormat DF_TODAY = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DF_OLD = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    private static final Date DATE = new Date();
    private static final Calendar CALENDAR = Calendar.getInstance();

    private Utils() {
    }

    public static <T> int findIndexByPredicate(List<T> data, Predicate<T> predicate){
        for(int i = 0; i < data.size(); i++){
            if(predicate.test(data.get(i))){
                return i;
            }
        }

        return -1;
    }

    public static boolean nonEmpty(Collection<?> data){
        return data != null && !data.isEmpty();
    }

    public static <T> ArrayList<T> cloneListAsArrayList(List<T> original){
        if(original == null){
            return null;
        }

        ArrayList<T> clone = new ArrayList<>(original.size());
        clone.addAll(original);
        return clone;
    }

    public static int countOfPositive(Collection<Integer> values){
        int count = 0;
        for(Integer value : values){
            if(value > 0){
                count++;
            }
        }

        return count;
    }

    public static int countOfNegative(Collection<Integer> values){
        int count = 0;
        for(Integer value : values){
            if(value < 0){
                count++;
            }
        }

        return count;
    }

    public static <T> int removeIf(Iterable<T> data, Predicate<T> predicate){
        int count = 0;
        Iterator<T> iterator = data.iterator();
        while (iterator.hasNext()){
            if(predicate.test(iterator.next())){
                iterator.remove();
                count++;
            }
        }

        return count;
    }

    public static void trimListToSize(List<?> data, int maxsize){
        if(data.size() > maxsize){
            data.remove(data.size() - 1);
            trimListToSize(data, maxsize);
        }
    }

    public static <T> List<T> copyListWithPredicate(final List<T> orig, Predicate<T> predicate){
        final List<T> data = new ArrayList<>(orig.size());
        for(T t : orig){
            if(predicate.test(t)){
                data.add(t);
            }
        }

        return data;
    }

    public static boolean isEmpty(CharSequence body) {
        return body == null || body.length() == 0;
    }

    public static boolean nonEmpty(CharSequence text){
        return text != null && text.length() > 0;
    }

    public interface SimpleFunction<F, S> {
        S apply(F orig);
    }

    public static <T> String join(Iterable<T> tokens, String delimiter, SimpleFunction<T, String> function){
        if(tokens == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (T token: tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }

            sb.append(function.apply(token));
        }

        return sb.toString();
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     * @param tokens an array objects to be joined. Strings will be formed from
     *     the objects by calling object.toString().
     */
    public static String join(CharSequence delimiter, Iterable tokens) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> it = tokens.iterator();
        if (it.hasNext()) {
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(delimiter);
                sb.append(it.next());
            }
        }
        return sb.toString();
    }

    public static Throwable getCauseIfRuntime(Throwable throwable) {
        Throwable target = throwable;
        while (target instanceof RuntimeException) {
            if (target.getCause() == null) {
                break;
            }

            target = target.getCause();
        }

        return target;
    }

    public static long startOfTodayMillis(){
        return startOfToday().getTimeInMillis();
    }

    public static Calendar startOfToday(){
        Calendar current = Calendar.getInstance();
        current.set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DATE), 0, 0, 0);
        return current;
    }

    /**
     * Null-safe equivalent of {@code a.equals(b)}.
     */
    public static boolean safeEquals(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    public static String getImageExt(String mime) {
        if (TextUtils.isEmpty(mime)) return null;

        switch (mime) {
            case "image/gif":
                return "gif";
            case "image/jpeg":
                return "jpeg";
            case "image/pjpeg":
                return "pjpeg";
            case "image/png":
                return "png";
            case "image/svg+xml":
                return "svg";
            case "image/tiff":
                return "tiff";
            case "image/vnd.microsoft.icon":
                return "ico";
            case "image/vnd.wap.wbmp":
                return "wbmp";
        }

        return null;
    }

    public static String getMimeType(Context context, String path) {
        // убираем все пробелы и переводим в нижний регистр для правильного распознавания mime
        String preparedName = path.toLowerCase().replaceAll("\\s", "");

        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(preparedName);

        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        if (type == null) {
            ContentResolver cR = context.getContentResolver();
            type = cR.getType(Uri.parse(path));
        }

        Log.d(UTILS_TAG, "getMimeType, file: " + path + ", extension: " + extension + ", type: " + type);
        return type;
    }

    @NonNull
    public static <T> List<T> listEmptyIfNull(@Nullable List<T> orig, boolean mutableIfNull){
        return orig == null ? (mutableIfNull ? new ArrayList<>(0) : Collections.emptyList()) : orig;
    }

    public static void openFile(Activity activity, File file) {
        if (!file.isFile()) {
            Toast.makeText(activity, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }

        openFile(activity, Uri.fromFile(file));
    }

    public static void openFile(Activity context, Uri uri) {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(context, uri.getPath()));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.app_for_file_not_found_message, Toast.LENGTH_LONG).show();
        }
    }

    public static String getBareJid(@NonNull String from) {
        int dividerPosition = from.indexOf(DIVIDER);
        if (dividerPosition == -1) {
            return from;
        }

        return from.substring(0, dividerPosition).toLowerCase();
    }

    public static String firstNonEmptyString(String... array) {
        for (String s : array) {
            if (!TextUtils.isEmpty(s)) {
                return s;
            }
        }

        return null;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static int safeCountOf(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    public static int safeCountOf(Cursor cursor) {
        return cursor == null ? 0 : cursor.getCount();
    }

    public static int safeCountOfMultiple(Collection<?>... collections) {
        if (collections == null) {
            return 0;
        }

        int count = 0;
        for (Collection<?> collection : collections) {
            count = count + safeCountOf(collection);
        }

        return count;
    }

    /**
     * Получение строки с датой и временем сообщений
     *
     * @param unixTime дата в формате unix-time
     * @return строка с датой и временем
     */
    public static String getDateFromUnixTime(long unixTime) {
        DATE.setTime(unixTime * 1000);
        CALENDAR.set(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH), CALENDAR.get(Calendar.DATE), 0, 0, 0);
        return unixTime * 1000 > CALENDAR.getTimeInMillis() ? DF_TODAY.format(DATE) : DF_OLD.format(DATE);
    }

    public static float dpToPx(float dp, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int getColorPrimary(Context context) {
        return getColorFromAttrs(R.attr.colorPrimary, context, "#000000");
    }

    public static int getPrimaryTextColor(Context context) {
        return getColorFromAttrs(R.attr.textColorPrimary, context, "#000000");
    }

    public static int getSecondaryTextColor(Context context) {
        return getColorFromAttrs(R.attr.textColorSecondary, context, "#000000");
    }
    public static int getSecondaryInverseTextColor(Context context) {
        return getColorFromAttrs(R.attr.textColorSecondaryInverse, context, "#000000");
    }


    private static int getColorFromAttrs(int resId, Context context, String defaultColor) {
        TypedValue a = new TypedValue();
        context.getTheme().resolveAttribute(resId, a, true);
        if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return a.data;
        } else {
            return Color.parseColor(defaultColor);
        }
    }

    /**
     * Получения строки с размером в мегабайтах
     *
     * @param size размер в байтах
     * @return строка типа "13.6 Mb"
     */
    public static String getSizeString(long size) {
        Double sizeDouble = ((double) size) / 1024 / 1024;
        double newDouble = new BigDecimal(sizeDouble).setScale(2, RoundingMode.UP).doubleValue();
        return newDouble + " Mb";
    }

    public static boolean hasMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /***
     * Android L (lollipop, API 21) introduced a new problem when trying to invoke implicit intent,
     * "java.lang.IllegalArgumentException: Service Intent must be explicit"
     * <p/>
     * If you are using an implicit intent, and know only 1 target would answer this intent,
     * This method will help you turn the implicit intent into the explicit form.
     * <p/>
     * Inspired from SO answer: http://stackoverflow.com/a/26318757/1446466
     *
     * @param context        context
     * @param implicitIntent - The original implicit intent
     * @return Explicit Intent created from the implicit original intent
     */
    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    public static int indexOf(List<? extends Identificable> data, int id) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId() == id) {
                return i;
            }
        }

        return -1;
    }

    public static String formatBytes(long bytes) {
        // TODO: add flag to which part is needed (e.g. GB, MB, KB or bytes)

        String retStr = "";

        // One binary gigabyte equals 1,073,741,824 bytes.
        if (bytes > 1073741824) {// Add GB
            long gbs = bytes / 1073741824;
            retStr += (Long.valueOf(gbs)).toString() + "GB ";
            bytes = bytes - (gbs * 1073741824);
        }

        // One MB - 1048576 bytes
        if (bytes > 1048576) {// Add GB
            long mbs = bytes / 1048576;
            retStr += (Long.valueOf(mbs)).toString() + "MB ";
            bytes = bytes - (mbs * 1048576);
        }

        if (bytes > 1024) {
            long kbs = bytes / 1024;
            retStr += (Long.valueOf(kbs)).toString() + "KB";
            bytes = bytes - (kbs * 1024);
        } else {
            retStr += (Long.valueOf(bytes)).toString() + " bytes";
        }

        return retStr;
    }

    public static String getDurationString(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        if (hours == 0) {
            return twoDigitString(minutes) + ":" + twoDigitString(seconds);
        } else {
            return twoDigitString(hours) + ":" + twoDigitString(minutes) + ":" + twoDigitString(seconds);
        }
    }

    private static String twoDigitString(int number) {
        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }

    public String getFileExtension(String filename) {
        try {
            String filenameArray[] = filename.split("\\.");
            return filenameArray[filenameArray.length - 1];
        } catch (Exception ignired) {
            return null;
        }
    }

    /**
     * <p>Adds an object to the list. The object will be inserted in the correct
     * place so that the objects in the list are sorted. When the list already
     * contains objects that are equal according to the comparator, the new
     * object will be inserted immediately after these other objects.</p>
     *
     * @param o the object to be added
     */
    public static <T> int addElementToList(final T o, List<T> data, Comparator<T> comparator) {
        int i = 0;
        boolean found = false;
        while (!found && (i < data.size())) {
            found = comparator.compare(o, data.get(i)) < 0;
            if (!found) {
                i++;
            }
        }

        data.add(i, o);
        return i;
    }

    public static <T> int getPositionForAdding(final T o, List<T> data, Comparator<T> comparator){
        int i = 0;
        boolean found = false;
        while (!found && (i < data.size())) {
            found = comparator.compare(o, data.get(i)) < 0;
            if (!found) {
                i++;
            }
        }

        return i;
    }

    public static void safelyCloseCursor(@Nullable Cursor cursor){
        if(cursor != null){
            cursor.close();
        }
    }

    public static boolean trimmedIsEmpty(CharSequence text){
        return text == null || TextUtils.getTrimmedLength(text) == 0;
    }
}