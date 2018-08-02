package biz.dealnote.xmpp.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Transformation;

import biz.dealnote.xmpp.Constants;
import biz.dealnote.xmpp.model.User;

import static biz.dealnote.xmpp.util.Utils.isEmpty;
import static biz.dealnote.xmpp.util.Utils.nonEmpty;

public class Avatars {

    public static void displayAvatar(Context context, AvatarWithLetter avatarWithLetter, @NonNull User user, Transformation transformation) {
        displayAvatar(context, avatarWithLetter, user.getJid(), user.getPhotoHash(), transformation);
    }

    public static void displayAvatar(Context context, AvatarWithLetter avatarWithLetter, String jid, @Nullable String photoHash, Transformation transformation) {
        boolean hasAvatar = nonEmpty(photoHash);

        avatarWithLetter.getAvatarView().setVisibility(hasAvatar ? View.VISIBLE : View.GONE);

        String firstInterlocutorLetter = isEmpty(jid) ? "?" : jid.substring(0, 1).toUpperCase();
        avatarWithLetter.getLetterView().setText(firstInterlocutorLetter);

        if (hasAvatar) {
            PicassoInstance.get()
                    .load(PicassoAvatarHandler.generateUri(photoHash))
                    .centerCrop()
                    .resize(200, 200)
                    .transform(transformation)
                    .tag(Constants.PICASSO_TAG)
                    .into(avatarWithLetter.getAvatarView());
        } else {
            PicassoInstance.get()
                    .cancelRequest(avatarWithLetter.getAvatarView());
        }
    }

    public interface AvatarWithLetter {
        ImageView getAvatarView();

        TextView getLetterView();
    }

}
