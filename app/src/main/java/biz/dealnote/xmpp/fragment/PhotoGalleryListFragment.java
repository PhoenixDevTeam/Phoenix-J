package biz.dealnote.xmpp.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.xmpp.Constants;
import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.adapter.PhotoAdapter;
import biz.dealnote.xmpp.callback.PicassoPauseOnScrollListener;
import biz.dealnote.xmpp.loader.PhotoGalleryAsyncLoader;
import biz.dealnote.xmpp.model.LocalPhoto;
import biz.dealnote.xmpp.util.Utils;

public class PhotoGalleryListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<List<LocalPhoto>>, PhotoAdapter.PhotoSelectionListener {

    private PhotoAdapter mAdapter;
    private ArrayList<LocalPhoto> mItems;
    private TextView mEmptyTextView;
    private ProgressDialog mLoadingProgressDialog;

    private long albumId;

    public static PhotoGalleryListFragment newInstance(long albumId) {
        Bundle args = new Bundle();
        args.putLong(Extra.ALBUM_ID, albumId);
        PhotoGalleryListFragment fragment = new PhotoGalleryListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        albumId = getArguments().getLong(Extra.ALBUM_ID);

        mItems = new ArrayList<>();
        mAdapter = new PhotoAdapter(getActivity(), mItems);
        mAdapter.setListener(this);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        RecyclerView mRecyclerView = (RecyclerView) view.findViewById(R.id.list);

        GridLayoutManager manager = new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.local_gallery_column_count));
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(getActivity(), Constants.PICASSO_TAG));

        mRecyclerView.setAdapter(mAdapter);
        mEmptyTextView = (TextView) view.findViewById(R.id.empty);

        resolveEmptyText();
        return view;
    }

    /**
     * Used to show a generic empty text warning. Override in inheriting classes.
     */
    protected void resolveEmptyText() {
        mEmptyTextView.setVisibility(Utils.isEmpty(mItems) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        mLoadingProgressDialog = new ProgressDialog(getActivity());
        mLoadingProgressDialog.setMessage("Loading Photos...");
        mLoadingProgressDialog.setCancelable(true);
        mLoadingProgressDialog.show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        cancelProgressDialog();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelProgressDialog();
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelProgressDialog();
    }

    /**
     * Loader Handlers for loading the photos in the background.
     */
    @Override
    public Loader<List<LocalPhoto>> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.
        return new PhotoGalleryAsyncLoader(getActivity(), albumId);
    }

    @Override
    public void onLoadFinished(Loader<List<LocalPhoto>> loader, List<LocalPhoto> data) {
        mItems.clear();
        mItems.addAll(data);

        mAdapter.notifyDataSetChanged();
        resolveEmptyText();
        cancelProgressDialog();
    }

    @Override
    public void onLoaderReset(Loader<List<LocalPhoto>> loader) {
        // Clear the data in the mAdapter.
        mItems.clear();
        mAdapter.notifyDataSetChanged();
        resolveEmptyText();
        cancelProgressDialog();
    }

    /**
     * Save cancel for the progress loader
     */
    private void cancelProgressDialog() {
        if (mLoadingProgressDialog != null && mLoadingProgressDialog.isShowing()) {
            mLoadingProgressDialog.cancel();
        }
    }

    @Override
    public void onPhotoSelected(LocalPhoto localPhoto) {
        Intent retIntent = new Intent();
        retIntent.putExtra(FileManagerFragment.returnFileParameter, String.valueOf(localPhoto.uri));
        getActivity().setResult(Activity.RESULT_OK, retIntent);
        getActivity().finish();
    }
}