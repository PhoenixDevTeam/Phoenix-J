package biz.dealnote.xmpp.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.SeekBar;

import biz.dealnote.xmpp.R;
import biz.dealnote.xmpp.util.Utils;

/**
 * Created by admin on 13.11.2016.
 * phoenix-for-xmpp
 */
public class LimitedWidthSeekbar extends SeekBar {

    public LimitedWidthSeekbar(Context context) {
        super(context);
        init(context, null);
    }

    public LimitedWidthSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private int mMaxWidth;

    private void init(Context context, AttributeSet attrs){
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.LimitedWidthSeekbar,
                0, 0);

        try {
            mMaxWidth = a.getDimensionPixelSize(R.styleable.LimitedWidthSeekbar_seekbar_max_width,
                    (int) Utils.dpToPx(160f, context));
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specWidth = MeasureSpec.getSize(widthMeasureSpec);

        if (specWidth > mMaxWidth) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth,
                    MeasureSpec.getMode(widthMeasureSpec));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
