package com.example.doctor_control.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.doctor_control.MainActivity;
import com.example.doctor_control.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AppointmentFragment extends Fragment {

    private static final String TAG = "AppointmentFragment";
    private static final int SWIPE_THRESHOLD = 100; // in pixels
    private static final int VELOCITY_THRESHOLD = 100; // in pixels per second
    private static final long NAVIGATION_DELAY_MS = 100; // delay for smooth transition

    private ViewPager2 innerViewPager;

    public AppointmentFragment() {
        // Required empty public constructor.
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout with TabLayout and nested ViewPager2.
        View view = inflater.inflate(R.layout.fragment_appointment, container, false);
        TabLayout tabLayout = view.findViewById(R.id.tabs);
        innerViewPager = view.findViewById(R.id.view_pager);

        // Set up adapter for inner ViewPager2.
        innerViewPager.setAdapter(new AppointmentPagerAdapter(this));

        // Set default page to "Ongoing" (index 0).
        innerViewPager.setCurrentItem(0, false);

        // Attach TabLayoutMediator for tab titles.
        new TabLayoutMediator(tabLayout, innerViewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Ongoing");
            } else if (position == 1) {
                tab.setText("Pending");
            } else if (position == 2) {
                tab.setText("Request");
            }
        }).attach();

        // Set up a GestureDetector on the inner ViewPager's RecyclerView.
        View recyclerView = innerViewPager.getChildAt(0);
        if (recyclerView instanceof RecyclerView) {
            final GestureDetector gestureDetector = new GestureDetector(getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                            if (e1 == null || e2 == null) return false;
                            float deltaX = e2.getX() - e1.getX();
                            int currentItem = innerViewPager.getCurrentItem();
                            int totalItems = (innerViewPager.getAdapter() != null)
                                    ? innerViewPager.getAdapter().getItemCount() : 0;

                            // Define edge region as 50dp converted to pixels.
                            int edgeSize = (int) (50 * getResources().getDisplayMetrics().density);

                            // For the first tab ("Ongoing"), allow outer navigation only if the fling starts from the left edge.
                            if (currentItem == 0 && deltaX > SWIPE_THRESHOLD && velocityX > VELOCITY_THRESHOLD &&
                                    e1.getX() < edgeSize) {
                                Log.d(TAG, "Detected edge fling: right swipe on first tab");
                                smoothNavigate(true);
                                return true;
                            }
                            // For the last tab ("Request"), check if the fling starts near the right edge.
                            if (currentItem == totalItems - 1 && deltaX < -SWIPE_THRESHOLD &&
                                    Math.abs(velocityX) > VELOCITY_THRESHOLD &&
                                    e1.getX() > (innerViewPager.getWidth() - edgeSize)) {
                                Log.d(TAG, "Detected edge fling: left swipe on last tab");
                                smoothNavigate(false);
                                return true;
                            }
                            return false;
                        }
                    });
            // In the touch listener, let inner ViewPager2 handle non‑edge gestures.
            recyclerView.setOnTouchListener((v, event) -> {
                // Depending on whether an edge fling is detected, we may allow or disallow parent interception.
                boolean edgeFlingDetected = gestureDetector.onTouchEvent(event);
                if (!edgeFlingDetected) {
                    // Tell parent to not intercept events—normal swiping should remain in the inner ViewPager2.
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            });
        } else {
            Log.w(TAG, "RecyclerView not found inside inner ViewPager2");
        }
        return view;
    }

    /**
     * Smoothly navigates to the appropriate page in the outer ViewPager2.
     *
     * @param toHome if true, navigates to Home (position 0); otherwise navigates to History (position 2).
     */
    private void smoothNavigate(boolean toHome) {
        // Temporarily disable inner ViewPager2 swipe.
        innerViewPager.setUserInputEnabled(false);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                // Outer navigation is triggered—enable the main ViewPager2 swiping to allow smooth transition.
                activity.setMainViewPagerSwipeEnabled(true);
                if (toHome) {
                    activity.navigateToHome();
                } else {
                    activity.navigateToHistory();
                }
            }
        }, NAVIGATION_DELAY_MS);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Disable outer (main) swiping while AppointmentFragment is active.
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setMainViewPagerSwipeEnabled(false);
        }
        innerViewPager.setUserInputEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Re-enable outer swiping when leaving AppointmentFragment.
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setMainViewPagerSwipeEnabled(true);
        }
    }

    /**
     * Adapter for the nested ViewPager2 in AppointmentFragment.
     */
    private static class AppointmentPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {

        public AppointmentPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new aOngoingFragment();
            } else if (position == 1) {
                return new aPendingFragment();
            } else if (position == 2) {
                return new aRequestFragment();
            }
            return new aOngoingFragment();
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
