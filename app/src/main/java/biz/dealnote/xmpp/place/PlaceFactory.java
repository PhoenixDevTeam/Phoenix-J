package biz.dealnote.xmpp.place;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import biz.dealnote.xmpp.Extra;

public class PlaceFactory {

    public static AppPlace getChatPlace(int accountId, @NonNull String destination, @Nullable Integer chatId){
        AppPlace place = new AppPlace(AppPlace.Type.CHAT);
        place.prepareArguments().putInt(Extra.ACCOUNT_ID, accountId);
        place.prepareArguments().putString(Extra.DESTINATION, destination);
        if(chatId != null){
            place.prepareArguments().putInt(Extra.CHAT_ID, chatId);
        }
        return place;
    }

    private PlaceFactory(){

    }
}
