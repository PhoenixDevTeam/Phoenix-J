package biz.dealnote.xmpp.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import biz.dealnote.xmpp.util.Logger;

/**
 * Created by ruslan.kolbasa on 10.11.2016.
 * mobilebankingandroid
 *
 * Class for safe caching RecyclerView holders
 */
public class SharedHolders<T extends IdentificableHolder> {

    private static final String TAG = SharedHolders.class.getSimpleName();

    private SparseArray<Set<WeakReference<T>>> mHoldersCache;

    private boolean mSupportManyHoldersForEntity;

    public SharedHolders(boolean supportManyHoldersForEntity) {
        mHoldersCache = new SparseArray<>(0);
        mSupportManyHoldersForEntity = supportManyHoldersForEntity;
    }

    @NonNull
    public SparseArray<Set<WeakReference<T>>> getCache() {
        return mHoldersCache;
    }

    @Nullable
    public T findOneByEntityId(int entityId) {
        Set<WeakReference<T>> weakReferences = mHoldersCache.get(entityId);
        if (weakReferences == null) {
            return null;
        }

        for (WeakReference<T> weakReference : weakReferences) {
            T holder = weakReference.get();
            if (holder != null) {
                return holder;
            }
        }

        return null;
    }

    @Nullable
    public T findHolderByHolderId(int holderId) {
        for (int i = 0; i < mHoldersCache.size(); i++) {
            int key = mHoldersCache.keyAt(i);
            Set<WeakReference<T>> holders = mHoldersCache.get(key);
            for (WeakReference<T> reference : holders) {
                T holder = reference.get();
                if (holder == null) {
                    continue;
                }

                if (holder.getHolderId() == holderId) {
                    return holder;
                }
            }
        }

        return null;
    }

    public void put(int entityId, @NonNull T holder) {
        Logger.d(TAG, "TRY to put holder, entityId: " + entityId);

        boolean success = false;

        for (int i = 0; i < mHoldersCache.size(); i++) {
            int key = mHoldersCache.keyAt(i);

            Set<WeakReference<T>> holders = mHoldersCache.get(key);
            boolean mustHaveInThisSet = entityId == key;

            Iterator<WeakReference<T>> iterator = holders.iterator();
            while (iterator.hasNext()) {
                WeakReference<T> reference = iterator.next();
                T h = reference.get();

                if (h == null) {
                    Logger.d(TAG, "WEAK reference expire, remove");
                    iterator.remove();
                    continue;
                }

                if (holder == h) {
                    if (!mustHaveInThisSet) {
                        Logger.d(TAG, "THIS holder should not be here, remove");
                        iterator.remove();
                    } else {
                        success = true;
                        Logger.d(TAG, "THIS holder alredy exist there");
                    }
                } else {
                    if (!mSupportManyHoldersForEntity && mustHaveInThisSet) {
                        Logger.d(TAG, "CACHE not support many holders for entity, remove other holder");
                        iterator.remove();
                    }
                }
            }

            if (mustHaveInThisSet && !success) {
                Logger.d(TAG, "SET for entity already exist, but holder not found, added");
                WeakReference<T> reference = new WeakReference<>(holder);
                holders.add(reference);
                success = true;
            }
        }

        if (!success) {
            Logger.d(TAG, "SET for entity does not exist yes, created and added");
            Set<WeakReference<T>> set = new HashSet<>(1);
            set.add(new WeakReference<>(holder));
            mHoldersCache.put(entityId, set);
        }
    }
}