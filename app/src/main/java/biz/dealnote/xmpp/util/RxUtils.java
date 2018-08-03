package biz.dealnote.xmpp.util;

import biz.dealnote.xmpp.BuildConfig;
import biz.dealnote.xmpp.Injection;
import io.reactivex.CompletableTransformer;
import io.reactivex.MaybeTransformer;
import io.reactivex.ObservableTransformer;
import io.reactivex.SingleTransformer;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by admin on 19.03.2017.
 * phoenix
 */
public class RxUtils {

    public static Action dummy(){
        return () -> {/*ignore*/};
    }

    public static <T> Consumer<T> ignore(){
        return t -> {
            if(BuildConfig.DEBUG && t instanceof Throwable){
                ((Throwable) t).printStackTrace();
            }
        };
    }

    public static Consumer<Throwable> print(){
        return t -> {
            if(BuildConfig.DEBUG){
                t.printStackTrace();
            }
        };
    }

    public static <T> MaybeTransformer<T, T> applyMaybeIOToMainSchedulers() {
        return upstream -> upstream
                .subscribeOn(Schedulers.io())
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler());
    }

    public static <T> SingleTransformer<T, T> applySingleIOToMainSchedulers() {
        return upstream -> upstream
                .subscribeOn(Schedulers.io())
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler());
    }

    public static <T> ObservableTransformer<T, T> applyObservableIOToMainSchedulers() {
        return upstream -> upstream
                .subscribeOn(Schedulers.io())
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler());
    }

    public static CompletableTransformer applyCompletableIOToMainSchedulers() {
        return completable -> completable.subscribeOn(Schedulers.io())
                .observeOn(Injection.INSTANCE.provideMainThreadScheduler());
    }
}