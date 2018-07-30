package biz.dealnote.xmpp.dialog;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.TextView;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.settings.NotificationSettings;
import biz.dealnote.xmpp.util.NotificationHelper;

public class NotificationSettingsDialog extends DialogFragment implements CompoundButton.OnCheckedChangeListener {

    private static final String EXTRA_KEY = "key";
    private String key;
    private SwitchCompat sEnable;
    private SwitchCompat sVibro;
    private SwitchCompat sLight;
    private SwitchCompat sSound;
    private TextView tvRingtoneTitle;

    public static NotificationSettingsDialog newInstance(String key) {
        Bundle args = new Bundle();
        args.putString(EXTRA_KEY, key);
        NotificationSettingsDialog fragment = new NotificationSettingsDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        key = getArguments().getString(EXTRA_KEY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().setCanceledOnTouchOutside(false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        View root = inflater.inflate(R.layout.fragment_notification, container, false);
        sEnable = (SwitchCompat) root.findViewById(R.id.enable);
        sVibro = (SwitchCompat) root.findViewById(R.id.vibration);
        sLight = (SwitchCompat) root.findViewById(R.id.light);
        sSound = (SwitchCompat) root.findViewById(R.id.sound);

        NotificationSettings.Value value = NotificationHelper.load(getActivity(), key);
        setupViewsWith(value);

        sEnable.setOnCheckedChangeListener(this);
        sVibro.setOnCheckedChangeListener(this);
        sLight.setOnCheckedChangeListener(this);
        sSound.setOnCheckedChangeListener(this);

        tvRingtoneTitle = (TextView) root.findViewById(R.id.ringnote_title);

        root.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return root;
    }

    private void setupViewsWith(NotificationSettings.Value value) {
        sEnable.setChecked(value.enable);
        sVibro.setChecked(value.vibro);
        sLight.setChecked(value.light);
        sSound.setChecked(value.sound);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.enable:
                NotificationHelper.write(getActivity(), key, NotificationHelper.SUBKEY_ENABLE, isChecked);
                break;
            case R.id.vibration:
                NotificationHelper.write(getActivity(), key, NotificationHelper.SUBKEY_VIBRATION, isChecked);
                break;
            case R.id.light:
                NotificationHelper.write(getActivity(), key, NotificationHelper.SUBKEY_LIGHT, isChecked);
                break;
            case R.id.sound:
                NotificationHelper.write(getActivity(), key, NotificationHelper.SUBKEY_SOUND, isChecked);
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
    }
}
