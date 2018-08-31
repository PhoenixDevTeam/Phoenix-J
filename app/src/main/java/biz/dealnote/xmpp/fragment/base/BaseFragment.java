package biz.dealnote.xmpp.fragment.base;

import androidx.fragment.app.Fragment;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Created by Ruslan Kolbasa on 25.04.2017.
 * phoenix-for-xmpp
 */
public class BaseFragment extends Fragment {

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    public void onDestroy() {
        mCompositeDisposable.dispose();
        super.onDestroy();
    }

    protected void appendDisposable(Disposable disposable){
        this.mCompositeDisposable.add(disposable);
    }
}
