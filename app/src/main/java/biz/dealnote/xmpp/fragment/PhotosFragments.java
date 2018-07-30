package biz.dealnote.xmpp.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.xmpp.Constants;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.adapter.PhotoAlbumsRecyclerAdapter;
import biz.dealnote.xmpp.callback.OnBackButtonCallback;
import biz.dealnote.xmpp.loader.LocalImageAlbumsAsyncLoader;
import biz.dealnote.xmpp.model.LocalImageAlbum;
import biz.dealnote.xmpp.util.PicassoInstance;

public class PhotosFragments extends Fragment implements LoaderManager.LoaderCallbacks<List<LocalImageAlbum>>, PhotoAlbumsRecyclerAdapter.ClickListner, OnBackButtonCallback {

    private RecyclerView mRecyclerView;
    private PhotoAlbumsRecyclerAdapter mAdapter;

    private ArrayList<LocalImageAlbum> data;
    private TextView mEmptyTextView;
    private ProgressDialog mLoadingProgressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_albums_gallery, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);

        StaggeredGridLayoutManager manager = new StaggeredGridLayoutManager(getResources().getInteger(R.integer.photos_albums_column_count), StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    PicassoInstance.get().resumeTag(Constants.PICASSO_TAG);
                } else {
                    PicassoInstance.get().pauseTag(Constants.PICASSO_TAG);
                }
            }
        });

        mEmptyTextView = (TextView) view.findViewById(R.id.empty);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        data = new ArrayList<>();
        mAdapter = new PhotoAlbumsRecyclerAdapter(getActivity(), data);
        mAdapter.setmClickListner(this);

        mRecyclerView.setAdapter(mAdapter);

        resolveEmptyText();

        getLoaderManager().initLoader(0, null, this);
    }

    protected void resolveEmptyText() {
        if (data.isEmpty()) {
            mEmptyTextView.setVisibility(View.VISIBLE);
        } else {
            mEmptyTextView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        mLoadingProgressDialog = new ProgressDialog(getActivity());
        mLoadingProgressDialog.setMessage("Loading Albums...");
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

    @Override
    public Loader<List<LocalImageAlbum>> onCreateLoader(int id, Bundle args) {
        return new LocalImageAlbumsAsyncLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<LocalImageAlbum>> loader, List<LocalImageAlbum> result) {
        data.clear();
        data.addAll(result);

        mAdapter.notifyDataSetChanged();

        resolveEmptyText();
        cancelProgressDialog();
    }

    @Override
    public void onLoaderReset(Loader<List<LocalImageAlbum>> loader) {
        data.clear();

        mAdapter.notifyDataSetChanged();
        resolveEmptyText();
        cancelProgressDialog();
    }

    private void cancelProgressDialog() {
        if (mLoadingProgressDialog != null) {
            if (mLoadingProgressDialog.isShowing()) {
                mLoadingProgressDialog.cancel();
            }
        }
    }

    @Override
    public void onAlbumSelected(LocalImageAlbum album) {
        PhotoGalleryListFragment fragment = PhotoGalleryListFragment.newInstance(album.getId());
        getChildFragmentManager().beginTransaction().replace(R.id.child_fragment, fragment).addToBackStack("album").commit();
    }

    @Override
    public boolean onBackPressed() {
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStack();
            return false;
        }

        return true;
    }
}