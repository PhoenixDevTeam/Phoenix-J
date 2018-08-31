package biz.dealnote.xmpp.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import androidx.appcompat.app.ActionBar;
import biz.dealnote.mvp.core.IPresenterFactory;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.activity.ActivityUtils;
import biz.dealnote.xmpp.callback.AppStyleable;
import biz.dealnote.xmpp.fragment.base.BaseMvpFragment;
import biz.dealnote.xmpp.model.AccountId;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.model.User;
import biz.dealnote.xmpp.mvp.presenter.ContactCardPresenter;
import biz.dealnote.xmpp.mvp.view.IContactCardView;
import biz.dealnote.xmpp.place.PlaceFactory;
import biz.dealnote.xmpp.util.PicassoAvatarHandler;
import biz.dealnote.xmpp.util.PicassoInstance;

import static biz.dealnote.xmpp.util.Utils.nonEmpty;

public class ContactCardFragment extends BaseMvpFragment<ContactCardPresenter, IContactCardView>
        implements View.OnClickListener, IContactCardView {

    private static final String TAG = ContactCardFragment.class.getSimpleName();

    //private Contact entry;
    private View mainInfoRoot;
    private View unauthorizeRoot;
    private ImageView avatar;
    private TextView avaterLetter;
    private TextView tvName;
    private TextView tvEmail;

    public static ContactCardFragment newInstance(Contact entry) {
        Bundle args = new Bundle();
        args.putParcelable("contact", entry);
        ContactCardFragment fragment = new ContactCardFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //entry = getArguments().getParcelable("contact");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card, container, false);

        mainInfoRoot = view.findViewById(R.id.main_info_root);
        unauthorizeRoot = view.findViewById(R.id.unauthorize_root);

        tvName = view.findViewById(R.id.name);
        tvEmail = view.findViewById(R.id.email);

        avatar = view.findViewById(R.id.avatar);
        avaterLetter = view.findViewById(R.id.avatar_letter);

        view.findViewById(R.id.to_chat_button).setOnClickListener(v -> getPresenter().fireChatClick());
        view.findViewById(R.id.send_add_request).setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        resolveViews(true);

        requestVcard();
    }

    /*@Override
    public void onRequestFinished(Request request, Bundle resultData) {
        Log.d(TAG, "onRequestFinished, resultData: " + resultData);
        // TODO: 07.11.2016 Do it with Database
        if (request.getRequestType() == RequestFactory.REQUEST_GET_VCARD) {
            User user = resultData.getParcelable("user");
            if (user != null) {
                onVcardUpdated(user);
            }
        }

        if (request.getRequestType() == RequestFactory.REQUEST_DELETE_ROSTER_ENTRY) {
            boolean deleted = resultData.getBoolean(Extra.SUCCESS);
            if (isAdded() && root != null) {
                Snackbar.make(root, deleted ? R.string.deleted : R.string.unable_to_delete_contact, Snackbar.LENGTH_LONG).show();
            }
        }
    }*/

    private void onVcardUpdated(@NonNull User user) {
        Log.d(TAG, "onVcardUpdated, user: " + user);

        //boolean photoWasChanged = !Utils.safeEquals(user.getPhotoHash(), entry.user.getPhotoHash());

        //entry.user = user;

        //resolveViews(photoWasChanged);
    }

    private void requestVcard() {
        //Request request = RequestFactory.getVcardRequest(entry.account.id, new StringArray(entry.jid));

    }

    private void resolveViews(boolean needRefreshAvatar) {
        if (!isAdded()) return;

        /*boolean hasAvatar = entry.user != null && nonEmpty(entry.user.getPhotoHash());

        avatar.setVisibility(hasAvatar ? View.VISIBLE : View.GONE);
        avaterLetter.setText(entry.jid.substring(0, 1));

        if (hasAvatar && needRefreshAvatar) {
            PicassoInstance.get()
                    .load(PicassoAvatarHandler.generateUri(entry.user.getPhotoHash()))
                    .centerCrop()
                    .resize(500, 500)
                    .into(avatar);
        }

        boolean needAuthorize = entry.type == Contact.TYPE_NONE || entry.type == Contact.TYPE_FROM;
        unauthorizeRoot.setVisibility(needAuthorize ? View.VISIBLE : View.GONE);
        mainInfoRoot.setVisibility(needAuthorize ? View.GONE : View.VISIBLE);

        if (needAuthorize) {
            return;
        }

        User user = entry.user;
        String availableEmail = TextUtils.isEmpty(user.getEmailHome()) ? user.getEmailWork() : user.getEmailHome();

        tvName.setText(user.getDispayName());
        tvEmail.setText(availableEmail);
        tvEmail.setVisibility(TextUtils.isEmpty(availableEmail) ? View.GONE : View.VISIBLE);*/
    }

    @Override
    public void openChat(@NotNull AccountId accountId, @NotNull String jid) {
        PlaceFactory.getChatPlace(accountId.getId(), jid, null).tryOpenWith(requireActivity());
    }

    private void toChat() {

    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AppStyleable) {
            AppStyleable styleable = (AppStyleable) getActivity();
            styleable.enableToolbarElevation(false);
        }

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(R.string.contact_card);
            //actionBar.setSubtitle(entry.jid);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_add_request:
                //Request request = RequestFactory.getSendPresenceRequest(entry.account.id, entry.account.buildBareJid(), entry.jid, Contact.PRESENCE_TYPE_SUBSCRIBE);
                //getRequestManager().executeRequest(request);
                break;
            case R.id.to_chat_button:
                toChat();
                break;
        }
    }

    private void deleteFromContactsList() {
        //Request request = RequestFactory.getDeleteRosterEntryRequest(entry.account, entry.jid);
        //getRequestManager().executeRequest(request);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(R.string.delete_from_contact_list).setOnMenuItemClickListener(item -> {
            deleteFromContactsList();
            return true;
        });
    }

    @Override
    public IPresenterFactory<ContactCardPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new ContactCardPresenter(requireArguments().getParcelable("contact"), saveInstanceState);
    }

    @Override
    public void displayUser(@NotNull User user) {
        boolean hasAvatar = nonEmpty(user.getPhotoHash());

        avatar.setVisibility(hasAvatar ? View.VISIBLE : View.GONE);
        avaterLetter.setText(user.getJid().substring(0, 1));

        if (hasAvatar) {
            PicassoInstance.get()
                    .load(PicassoAvatarHandler.generateUri(user.getPhotoHash()))
                    .centerCrop()
                    .resize(500, 500)
                    .into(avatar);
        } else {
            PicassoInstance.get().cancelRequest(avatar);
        }

        String availableEmail = TextUtils.isEmpty(user.getEmailHome()) ? user.getEmailWork() : user.getEmailHome();

        tvName.setText(user.getDispayName());
        tvEmail.setText(availableEmail);
        tvEmail.setVisibility(TextUtils.isEmpty(availableEmail) ? View.GONE : View.VISIBLE);

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if(actionBar != null){
            actionBar.setSubtitle(user.getJid());
        }
    }
}