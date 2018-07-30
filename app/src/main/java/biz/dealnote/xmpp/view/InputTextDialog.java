package biz.dealnote.xmpp.view;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import biz.dealnote.xmpp.R;

public class InputTextDialog {

    private Context context;
    private int inputType;
    private int titleRes;
    private String value;
    private boolean allowEmpty;
    private TextView target;
    private Callback callback;
    private Validator validator;

    private InputTextDialog() {
        // not instantiate class
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleRes);
        View view = View.inflate(context, R.layout.dialog_enter_text, null);

        final EditText input = (EditText) view.findViewById(R.id.editText);
        input.setText(value);
        input.setSelection(input.getText().length());
        input.setInputType(inputType);
        builder.setView(view);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        input.setError(null);
                        String newValue = input.getText().toString().trim();

                        if (TextUtils.isEmpty(newValue) && !allowEmpty) {
                            input.setError(context.getString(R.string.fill_in_this_field));
                            input.requestFocus();
                        } else {
                            try {
                                if (validator != null) {
                                    validator.validate(newValue);
                                }

                                if (callback != null) {
                                    callback.onChanged(newValue);
                                }

                                if (target != null) {
                                    target.setText(newValue);
                                }

                                alertDialog.dismiss();
                            } catch (IllegalArgumentException e) {
                                input.setError(e.getMessage());
                                input.requestFocus();
                            }
                        }
                    }
                });
            }
        });

        alertDialog.show();
    }

    public interface Callback {
        void onChanged(String newValue);
    }

    public interface Validator {
        void validate(String value) throws IllegalArgumentException;
    }

    public static class Builder {

        private Context context;
        private int inputType;
        private int titleRes;
        private String value;
        private boolean allowEmpty;
        private TextView target;
        private Callback callback;
        private Validator validator;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setInputType(int inputType) {
            this.inputType = inputType;
            return this;
        }

        public Builder setTitleRes(int titleRes) {
            this.titleRes = titleRes;
            return this;
        }

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        public Builder setAllowEmpty(boolean allowEmpty) {
            this.allowEmpty = allowEmpty;
            return this;
        }

        public Builder setTarget(TextView target) {
            this.target = target;
            return this;
        }

        public Builder setCallback(Callback callback) {
            this.callback = callback;
            return this;
        }

        public Builder setValidator(Validator validator) {
            this.validator = validator;
            return this;
        }

        public InputTextDialog create() {
            InputTextDialog inputTextDialog = new InputTextDialog();
            inputTextDialog.context = context;
            inputTextDialog.inputType = inputType;
            inputTextDialog.titleRes = titleRes;
            inputTextDialog.value = value;
            inputTextDialog.allowEmpty = allowEmpty;
            inputTextDialog.target = target;
            inputTextDialog.callback = callback;
            inputTextDialog.validator = validator;
            return inputTextDialog;
        }

        public void show() {
            create().show();
        }
    }
}
