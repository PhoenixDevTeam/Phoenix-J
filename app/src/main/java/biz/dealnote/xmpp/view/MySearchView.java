package biz.dealnote.xmpp.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import biz.dealnote.xmpp.R;


/**
 * Created by Ruslan Kolbasa on 12.04.2017.
 * mosst-code-android
 */
public class MySearchView extends LinearLayout {

    private String mQuery;

    private AutoCompleteTextView mInput;
    private ImageView mButtonLeft;
    private ImageView mButtonClear;
    private ImageView mButtonAdditional;
    private OnClearButtonClickListener mOnClearButtonClickListener;

    public MySearchView(Context context) {
        this(context, null);
    }

    public MySearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setAutocompleteData(@NonNull String[] items){
        mInput.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, items));
        //mInput.setDropDownAnWidth(mViewContainer.getWidth());
        mInput.showDropDown();
    }

    public  <T extends ListAdapter & Filterable> void setAdapter(T adapter){
        mInput.setAdapter(adapter);
    }

    private AdapterView.OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public void setText(CharSequence text){
        mInput.setText(text);
    }

    private void init() {
        inflate(getContext(), R.layout.custom_searchview, this);

        mInput = (AutoCompleteTextView) findViewById(R.id.input);
        mInput.setOnEditorActionListener(mOnEditorActionListener);
        mInput.setThreshold(3);
        mInput.setOnItemClickListener((parent, view, position, id) -> {
            if(mOnItemClickListener != null){
                mOnItemClickListener.onItemClick(parent, view, position, id);
            }
        });

        mButtonLeft = (ImageView) findViewById(R.id.button_left);
        mButtonClear = (ImageView) findViewById(R.id.clear);
        mButtonAdditional = (ImageView) findViewById(R.id.button_additional);

        mInput.addTextChangedListener(new SimpleTextWatcher() {
            String previos = null;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previos = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
                    mQuery = s.toString();

                    if (mOnQueryChangeListener != null) {
                        mOnQueryChangeListener.onQueryTextChange(mQuery);
                    }

                    resolveCloseButton();

            }
        });

        mButtonClear.setOnClickListener(v -> {
            clear();
            if (mOnClearButtonClickListener!=null){
                mOnClearButtonClickListener.onClearButtonClick();
            }
        });

        mButtonLeft.setOnClickListener(v -> {
            if (mOnBackButtonClickListener != null) {
                mOnBackButtonClickListener.onBackButtonClick();
            }
        });

        mButtonAdditional.setOnClickListener(v -> {
            if (mOnAdditionalButtonClickListener != null) {
                mOnAdditionalButtonClickListener.onAdditionalButtonClick();
            }
        });

        resolveCloseButton();
    }

    public void addTextChangedListener(TextWatcher textWatcher){
        mInput.addTextChangedListener(textWatcher);
    }

    public void setInputType(int type){
        mInput.setInputType(type);
    }

    public Editable getText(){
        return mInput.getText();
    }

    public void clear(){
        mInput.getText().clear();

    }

    private final TextView.OnEditorActionListener mOnEditorActionListener = (v, actionId, event) -> {
        onSubmitQuery();
        return true;
    };

    private void onSubmitQuery() {
        CharSequence query = mInput.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener != null && mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getWindowToken(), 0);
                }
            }
        }
    }

    public void setAdditionalButtonDrawableRes(@DrawableRes int res){
        mButtonAdditional.setImageResource(res);
    }

    public void setAdditionalButtonVisibility(boolean visible){
        mButtonAdditional.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setOnClearButtonClickListener(OnClearButtonClickListener listener){
        mOnClearButtonClickListener = listener;
    }

    private void resolveCloseButton() {
        boolean empty = TextUtils.isEmpty(mQuery);
        mButtonClear.setVisibility(TextUtils.isEmpty(mQuery) ? GONE : VISIBLE);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        Bundle state = new Bundle();
        state.putParcelable("PARENT", superState);
        state.putString("query", mQuery);

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle savedState = (Bundle) state;

        Parcelable superState = savedState.getParcelable("PARENT");
        super.onRestoreInstanceState(superState);

        mQuery = savedState.getString("query");
        mInput.setText(mQuery);
    }

    private OnQueryTextListener mOnQueryChangeListener;

    public void setOnQueryTextListener(OnQueryTextListener onQueryChangeListener) {
        this.mOnQueryChangeListener = onQueryChangeListener;
    }

    public void setOnLeftButtonClickListener(OnBackButtonClickListener onBackButtonClickListener) {
        this.mOnBackButtonClickListener = onBackButtonClickListener;
    }

    /**
     * Callbacks for changes to the query text.
     */
    public interface OnQueryTextListener {

        /**
         * Called when the user submits the query. This could be due to a key press on the
         * keyboard or due to pressing a submit button.
         * The listener can override the standard behavior by returning true
         * to indicate that it has handled the submit request. Otherwise return false to
         * let the SearchView handle the submission by launching any associated intent.
         *
         * @param query the query text that is to be submitted
         * @return true if the query has been handled by the listener, false to let the
         * SearchView perform the default action.
         */
        boolean onQueryTextSubmit(String query);

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        boolean onQueryTextChange(String newText);

        void onSuggessionSelected(int position);
    }

    public interface OnBackButtonClickListener {
        void onBackButtonClick();
    }

    public interface OnClearButtonClickListener {
        void onClearButtonClick();
    }

    private OnBackButtonClickListener mOnBackButtonClickListener;

    public interface OnAdditionalButtonClickListener {
        void onAdditionalButtonClick();
    }

    private OnAdditionalButtonClickListener mOnAdditionalButtonClickListener;

    public void setOnAdditionalButtonClickListener(OnAdditionalButtonClickListener onAdditionalButtonClickListener) {
        this.mOnAdditionalButtonClickListener = onAdditionalButtonClickListener;
    }

    public void setQuery(String query, boolean quetly){
        OnQueryTextListener tmp = mOnQueryChangeListener;
        if(quetly) {
            mOnQueryChangeListener = null;
        }

        setQuery(query);

        if(quetly){
            mOnQueryChangeListener = tmp;
        }
    }

    public void setQuery(String query) {
        mInput.setText(query);
    }

    public void setHint(String hint){
        mInput.setHint(hint);
    }

    public void setSelection(int start, int end) {
        mInput.setSelection(start, end);
    }

    public void setSelection(int position) {
        mInput.setSelection(position);
    }

    public void setLeftIcon(@DrawableRes int drawable) {
        mButtonLeft.setImageResource(drawable);
    }
}