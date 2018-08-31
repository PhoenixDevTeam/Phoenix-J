package biz.dealnote.xmpp.activity;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import biz.dealnote.xmpp.R;

public class ActivityUtils {

    public static ActionBar supportToolbarFor(Fragment fragment) {
        if (fragment.getActivity() == null) {
            return null;
        }

        return ((AppCompatActivity) fragment.getActivity()).getSupportActionBar();
    }

    public static void showSaveChangesWarning(Context context, DialogInterface.OnClickListener saveListener, DialogInterface.OnClickListener cancelListener) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.confirmation)
                .setMessage(R.string.save_changes_question)
                .setPositiveButton(R.string.button_yes, saveListener)
                .setNegativeButton(R.string.button_no, cancelListener)
                .setNeutralButton(R.string.button_cancel, null)
                .show();
    }
}