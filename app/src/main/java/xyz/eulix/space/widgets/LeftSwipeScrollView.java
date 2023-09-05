package xyz.eulix.space.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

import java.util.UUID;

public class LeftSwipeScrollView extends HorizontalScrollView {
    private String mViewUuid;
    private int[] mMenuWidth;
    private int mMenuMargin;
    private boolean isMenuEndMargin;
    private LeftSwipeScrollCallback mCallback;

    public interface LeftSwipeScrollCallback {
        void onActionDown(String viewUuid);
    }

    public LeftSwipeScrollView(Context context) {
        super(context);
        init();
    }

    public LeftSwipeScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LeftSwipeScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public LeftSwipeScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mViewUuid = UUID.randomUUID().toString();
    }

    public void registerCallback(LeftSwipeScrollCallback callback) {
        mCallback = callback;
    }

    public void setMenuInfo(int[] menuWidth, int menuMargin, boolean isMenuEndMargin) {
        mMenuWidth = menuWidth;
        mMenuMargin = menuMargin;
        this.isMenuEndMargin = isMenuEndMargin;
    }

    public void resetScroll() {
        resetScroll(null);
    }

    public void resetScroll(String viewUuid) {
        if ((viewUuid == null || !viewUuid.equals(mViewUuid)) && (getScrollX() != 0)) {
            scrollTo(0, getScrollY());
        }
    }

    private boolean changeScrollX() {
        boolean isHandle = false;
        if (mMenuWidth != null) {
            int menuNumber = mMenuWidth.length;
            if (menuNumber > 0) {
                int scrollX = getScrollX();
                int changeScrollX = scrollX;
                int menuTotalWidth = 0;
                for (int i = 0; i < menuNumber; i++) {
                    int menuWidth = Math.abs(mMenuWidth[i]);
                    int marginMenuWidth = menuWidth + ((isMenuEndMargin || ((i + 1) < menuNumber))
                            ? mMenuMargin : 0);
                    if (scrollX < (menuTotalWidth + marginMenuWidth)) {
                        changeScrollX = menuTotalWidth + (((scrollX - menuTotalWidth) > (menuWidth / 2))
                                ? marginMenuWidth : 0);
                        break;
                    } else {
                        menuTotalWidth += marginMenuWidth;
                    }
                }
                isHandle = (changeScrollX != scrollX);
                if (isHandle) {
                    smoothScrollTo(changeScrollX, getScrollY());
                }
            }
        }
        return isHandle;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mCallback != null) {
                    mCallback.onActionDown(mViewUuid);
                }
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (changeScrollX()) {
                    return true;
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }
}
