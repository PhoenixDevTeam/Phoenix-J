package biz.dealnote.xmpp.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import biz.dealnote.xmpp.Constants;
import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.activity.ActivityUtils;
import biz.dealnote.xmpp.adapter.IncomeFilesAdapter;
import biz.dealnote.xmpp.db.ChatContentProvider;
import biz.dealnote.xmpp.db.Storages;
import biz.dealnote.xmpp.db.columns.MessagesColumns;
import biz.dealnote.xmpp.model.IncomeFileItem;
import biz.dealnote.xmpp.model.Msg;
import biz.dealnote.xmpp.util.Utils;

public class IncomeFilesFragment extends Fragment implements IncomeFilesAdapter.ClickListener {

    private static final String TAG = IncomeFilesFragment.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private IncomeFilesAdapter mAdapter;
    private File path;
    private FilesCriteria criteria;
    private List<IncomeFileItem> fileItems;

    public static IncomeFilesFragment newInstance(@Nullable FilesCriteria criteria) {
        Bundle args = new Bundle();
        args.putParcelable(Extra.CRITERIA, criteria);
        IncomeFilesFragment filesFragment = new IncomeFilesFragment();
        filesFragment.setArguments(args);
        return filesFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = new File(Constants.INCOME_FILES_DIRECTORY);
        criteria = getArguments().getParcelable(Extra.CRITERIA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_income_files, container, false);

        mRecyclerView = (RecyclerView) root.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        fileItems = createList(criteria);
        mAdapter = new IncomeFilesAdapter(getActivity(), fileItems);
        mAdapter.setClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    private List<IncomeFileItem> createList(@Nullable FilesCriteria criteria) {
        boolean mkdirs = path.mkdirs();
        Log.d(TAG, "mkdirs: " + mkdirs);

        ArrayList<IncomeFileItem> items = new ArrayList<>();

        String[] fList = path.list(createFilter(criteria));

        for (String aFList : fList) {
            File file = new File(path, aFList);
            items.add(new IncomeFileItem(file, R.drawable.ic_file));
        }

        return items;
    }

    private FilenameFilter createFilter(@Nullable final FilesCriteria criteria) {
        if (criteria == null) return null;

        final ArrayList<String> paths = getFilesByDestination(criteria.destnation);
        return new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                File file = new File(dir, filename);

                boolean acceptName = false;
                if (paths != null) {
                    for (String path : paths) {
                        File f = new File(path);
                        if (file.equals(f)) {
                            acceptName = true;
                            break;
                        }
                    }
                } else {
                    acceptName = true;
                }

                boolean acceptExts = false;
                if (criteria.exts != null) {
                    for (String ext : criteria.exts) {
                        if (filename.endsWith(ext)) {
                            acceptExts = true;
                            break;
                        }
                    }
                } else {
                    acceptExts = true;
                }

                return acceptName && acceptExts;
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(R.string.files);
            actionBar.setSubtitle(criteria == null || TextUtils.isEmpty(criteria.destnation) ? null : criteria.destnation);
        }
    }

    private ArrayList<String> getFilesByDestination(String destnation) {
        if (TextUtils.isEmpty(destnation)) return null;

        String[] columns = {MessagesColumns.ATTACHED_FILE_PATH};
        String where = MessagesColumns.TYPE + " = ? AND " + MessagesColumns.STATUS + " = ? AND " + MessagesColumns.DESTINATION + " LIKE ?";
        String[] args = {String.valueOf(Msg.TYPE_INCOME_FILE), String.valueOf(Msg.STATUS_DONE), destnation};

        Cursor cursor = getActivity().getContentResolver().query(ChatContentProvider.MESSAGES_CONTENT_URI, columns, where, args, null);
        if (cursor == null) {
            return null;
        }

        ArrayList<String> paths = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            paths.add(cursor.getString(cursor.getColumnIndex(MessagesColumns.ATTACHED_FILE_PATH)));
        }

        cursor.close();
        return paths;
    }

    private boolean deleteFileImpl(IncomeFileItem fileItem) {
        String where = MessagesColumns.TYPE + " = ? AND " + MessagesColumns.ATTACHED_FILE_PATH + " LIKE ?";
        String[] args = {String.valueOf(Msg.TYPE_INCOME_FILE), fileItem.file.getPath()};
        String[] columns = {MessagesColumns._ID, MessagesColumns.CHAT_ID};

        Cursor cursor = getActivity().getContentResolver().query(ChatContentProvider.MESSAGES_CONTENT_URI, columns, where, args, null);

        if (cursor != null) {
            Map<Integer, Set<Integer>> map = new HashMap<>();

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(MessagesColumns._ID));
                int chatId = cursor.getInt(cursor.getColumnIndex(MessagesColumns.CHAT_ID));

                Set<Integer> ids = map.get(chatId);
                if (ids == null) {
                    ids = new HashSet<>();
                    map.put(chatId, ids);
                }

                ids.add(id);
            }

            cursor.close();

            for (Map.Entry<Integer, Set<Integer>> entry : map.entrySet()) {
                Storages.getINSTANCE()
                        .getMessages()
                        .deleteMessages(entry.getKey(), entry.getValue())
                        .blockingGet();
            }
        }

        boolean deleted = fileItem.file.delete();
        getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem.file)));
        return deleted;
    }

    private void deleteFile(int index, IncomeFileItem fileItem) {
        if (deleteFileImpl(fileItem)) {
            fileItems.remove(index);
            mAdapter.notifyItemRemoved(index);
            Toast.makeText(getActivity(), R.string.deleted, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), R.string.unable_to_delete_file, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(final int index, final IncomeFileItem item) {
        String[] options = {getString(R.string.open_file), getString(R.string.delete)};
        new AlertDialog.Builder(getActivity()).setTitle(item.file.getName()).setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Utils.openFile(getActivity(), item.file);
                        break;
                    case 1:
                        deleteFile(index, item);
                        break;
                }
            }
        }).setNegativeButton(R.string.button_cancel, null).show();
    }
}
