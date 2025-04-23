package com.example.doctor_control.adapter;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.example.doctor_control.MainActivity;
import com.example.doctor_control.R;

public class TouchInterceptionFrameLayout extends FrameLayout {
    private ViewPager2 childViewPager;
    private float initialX, initialY;
    private boolean isHorizontalScrolling;
    private boolean atEdgeSwipe = false;
    private boolean swipingToStart = false;
    private final int touchSlop;
    private static final String TAG = "TouchInterception";

    public TouchInterceptionFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public TouchInterceptionFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        childViewPager = findViewById(R.id.child_view_pager);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (childViewPager == null) return super.onInterceptTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = ev.getX();
                initialY = ev.getY();
                isHorizontalScrolling = false;
                atEdgeSwipe = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - initialX;
                float dy = ev.getY() - initialY;

                if (!isHorizontalScrolling) {
                    isHorizontalScrolling = Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > touchSlop;
                }

                if (isHorizontalScrolling) {
                    int itemCount = childViewPager.getAdapter() != null ? childViewPager.getAdapter().getItemCount() : 0;
                    int currentItem = childViewPager.getCurrentItem();
                    boolean atStart = currentItem == 0 && dx > 0;
                    boolean atEnd = currentItem == itemCount - 1 && dx < 0;

                    if (atStart || atEnd) {
                        Log.d(TAG, atEnd ? "At END edge detected (wait for lift)" : "At START edge detected (wait for lift)");
                        atEdgeSwipe = true;
                        swipingToStart = atStart;
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                isHorizontalScrolling = false;
                atEdgeSwipe = false;
                break;

            case MotionEvent.ACTION_UP:
                Log.d(TAG, "ACTION_UP received");

                if (atEdgeSwipe && getContext() instanceof MainActivity) {
                    MainActivity main = (MainActivity) getContext();
                    if (swipingToStart) {
                        Log.d(TAG, "Swipe completed → Navigate to HOME");
                        main.navigateToHome();
                    } else {
                        Log.d(TAG, "Swipe completed → Navigate to HISTORY");
                        main.navigateToHistory();
                    }
                    atEdgeSwipe = false;
                    return true; // Consume
                }
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (childViewPager != null) {
            childViewPager.dispatchTouchEvent(event);
            return true;
        }
        return super.onTouchEvent(event);
    }
}
