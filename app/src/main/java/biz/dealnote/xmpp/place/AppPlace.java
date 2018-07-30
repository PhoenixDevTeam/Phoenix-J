package biz.dealnote.xmpp.place;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

public class AppPlace implements Parcelable {

    private int type;
    private Bundle args;
    private Fragment target;
    private int requestCode;

    public static final class Type {
        public static final int CHAT = 1;
    }

    public AppPlace(int type) {
        this.type = type;
    }

    protected AppPlace(Parcel in) {
        type = in.readInt();
        args = in.readBundle(getClass().getClassLoader());
    }

    public int getType() {
        return type;
    }

    public Bundle getArgs() {
        return args;
    }

    public Fragment getTarget() {
        return target;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public static final Creator<AppPlace> CREATOR = new Creator<AppPlace>() {
        @Override
        public AppPlace createFromParcel(Parcel in) {
            return new AppPlace(in);
        }

        @Override
        public AppPlace[] newArray(int size) {
            return new AppPlace[size];
        }
    };

    public void tryOpenWith(@NonNull Context context){
        if(context instanceof AppPlaceProvider){
            ((AppPlaceProvider)context).openPlace(this);
        }
    }

    public AppPlace targetTo(Fragment fragment, int requestCode){
        this.target = fragment;
        this.requestCode = requestCode;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeBundle(args);
    }

    public AppPlace setArguments(Bundle arguments) {
        this.args = arguments;
        return this;
    }

    public AppPlace withAdditionalStringExtra(String name, String value){
        prepareArguments().putString(name, value);
        return this;
    }

    public AppPlace withAdditionalParcelableExtra(String name, Parcelable parcelableExtra){
        prepareArguments().putParcelable(name, parcelableExtra);
        return this;
    }

    public Bundle prepareArguments(){
        if(args == null){
            args = new Bundle();
        }

        return args;
    }

    public boolean hasTargeting(){
        return target != null;
    }

    public void obtainTargeting(@NonNull Fragment fragment){
        if(hasTargeting()){
            fragment.setTargetFragment(target, requestCode);
        }
    }
}
