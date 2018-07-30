package biz.dealnote.xmpp.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.activity.ActivityUtils;
import biz.dealnote.xmpp.callback.AppStyleable;
import biz.dealnote.xmpp.model.AppRosterEntry;
import biz.dealnote.xmpp.model.Contact;
import biz.dealnote.xmpp.place.PlaceFactory;
import biz.dealnote.xmpp.service.StringArray;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.RequestFactory;
import biz.dealnote.xmpp.util.PicassoAvatarHandler;
import biz.dealnote.xmpp.util.PicassoInstance;
import biz.dealnote.xmpp.util.Utils;

import static biz.dealnote.xmpp.util.Utils.nonEmpty;

public class ContactCardFragment extends AbsRequestSupportFragment implements View.OnClickListener {

    private static final String TAG = ContactCardFragment.class.getSimpleName();
    private static final String EXTRA_ENTRY = "entry";

    private AppRosterEntry entry;
    private View root;
    private View mainInfoRoot;
    private View unauthorizeRoot;
    private ImageView avatar;
    private TextView avaterLetter;
    private TextView tvName;
    private TextView tvEmail;

    public static ContactCardFragment newInstance(AppRosterEntry entry) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_ENTRY, entry);
        ContactCardFragment fragment = new ContactCardFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        entry = getArguments().getParcelable(EXTRA_ENTRY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_card, container, false);
        mainInfoRoot = root.findViewById(R.id.main_info_root);
        unauthorizeRoot = root.findViewById(R.id.unauthorize_root);

        tvName = (TextView) root.findViewById(R.id.name);
        tvEmail = (TextView) root.findViewById(R.id.email);

        avatar = (ImageView) root.findViewById(R.id.avatar);
        avaterLetter = (TextView) root.findViewById(R.id.avatar_letter);

        root.findViewById(R.id.to_chat_button).setOnClickListener(this);
        root.findViewById(R.id.send_add_request).setOnClickListener(this);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        resolveViews(true);

        requestVcard();
    }

    @Override
    public void onRestoreConnectionToRequest(Request request) {

    }

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {
        Log.d(TAG, "onRequestFinished, resultData: " + resultData);
        // TODO: 07.11.2016 Do it with Database
        if (request.getRequestType() == RequestFactory.REQUEST_GET_VCARD) {
            Contact contact = resultData.getParcelable("contact");
            if (contact != null) {
                onVcardUpdated(contact);
            }
        }

        if (request.getRequestType() == RequestFactory.REQUEST_DELETE_ROSTER_ENTRY) {
            boolean deleted = resultData.getBoolean(Extra.SUCCESS);
            if (isAdded() && root != null) {
                Snackbar.make(root, deleted ? R.string.deleted : R.string.unable_to_delete_contact, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onCustromError(Request request, String errorText, int code) {
        if (isAdded() && root != null) {
            Snackbar.make(root, errorText, Snackbar.LENGTH_LONG).show();
        }
    }

    private void onVcardUpdated(@NonNull Contact contact) {
        Log.d(TAG, "onVcardUpdated, contact: " + contact);

        boolean photoWasChanged = !Utils.safeEquals(contact.getPhotoHash(), entry.contact.getPhotoHash());

        entry.contact = contact;
        getArguments().putParcelable(EXTRA_ENTRY, entry);

        resolveViews(photoWasChanged);
    }

    private void requestVcard() {
        Request request = RequestFactory.getVcardRequest(entry.account.id, new StringArray(entry.jid));
        getRequestManager().executeRequest(request);
    }

    private void resolveViews(boolean needRefreshAvatar) {
        if (!isAdded()) return;

        boolean hasAvatar = entry.contact != null && nonEmpty(entry.contact.getPhotoHash());

        avatar.setVisibility(hasAvatar ? View.VISIBLE : View.GONE);
        avaterLetter.setText(entry.jid.substring(0, 1));

        if (hasAvatar && needRefreshAvatar) {
            PicassoInstance.get()
                    .load(PicassoAvatarHandler.generateUri(entry.contact.getPhotoHash()))
                    .centerCrop()
                    .resize(500, 500)
                    .into(avatar);
        }

        boolean needAuthorize = entry.type == AppRosterEntry.TYPE_NONE || entry.type == AppRosterEntry.TYPE_FROM;
        unauthorizeRoot.setVisibility(needAuthorize ? View.VISIBLE : View.GONE);
        mainInfoRoot.setVisibility(needAuthorize ? View.GONE : View.VISIBLE);

        if (needAuthorize) {
            return;
        }

        Contact contact = entry.contact;
        String availableEmail = TextUtils.isEmpty(contact.getEmailHome()) ? contact.getEmailWork() : contact.getEmailHome();

        tvName.setText(contact.getDispayName());
        tvEmail.setText(availableEmail);
        tvEmail.setVisibility(TextUtils.isEmpty(availableEmail) ? View.GONE : View.VISIBLE);
    }

    private void toChat() {
        //if (getActivity() instanceof OnPlaceOpenCallback) {
        //    ((OnPlaceOpenCallback) getActivity()).onChatOpen(entry.account, entry.jid, null);
        //}

        PlaceFactory.getChatPlace(entry.getAccount().getId(), entry.getJid(), null)
                .tryOpenWith(getActivity());
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
            actionBar.setSubtitle(entry.jid);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_add_request:
                Request request = RequestFactory.getSendPresenceRequest(entry.account.id, entry.account.buildBareJid(), entry.jid, AppRosterEntry.PRESENCE_TYPE_SUBSCRIBE);
                getRequestManager().executeRequest(request);
                break;
            case R.id.to_chat_button:
                toChat();
                break;
        }
    }

    private void deleteFromContactsList() {
        Request request = RequestFactory.getDeleteRosterEntryRequest(entry.account, entry.jid);
        getRequestManager().executeRequest(request);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(R.string.delete_from_contact_list).setOnMenuItemClickListener(item -> {
            deleteFromContactsList();
            return true;
        });
    }
}
