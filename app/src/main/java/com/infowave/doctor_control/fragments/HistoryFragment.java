package com.infowave.doctor_control.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.doctor_control.HistoryItem;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.adapter.HistoryAdapter;
import com.infowave.doctor_control.ApiConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Doctor-side History with:
 *  - Section headers (Today / Yesterday / Last 7 days / Earlier)
 *  - Live silent refresh (every 6s) + pull-to-refresh
 *  - Search bar (patient name / date / appointment id / status / problem)
 *
 * Adapter files unchanged.
 */
public class HistoryFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 6000L;
    private static final String REQ_TAG = "doc_history_poll";

    private RecyclerView rvHistory;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private ImageButton btnClear;
    private ImageView icSearch;

    private HistoryAdapter historyAdapter;

    // adapter-visible lists
    private final ArrayList<HistoryItem> historyItems = new ArrayList<>();
    private final ArrayList<String> appointmentIds = new ArrayList<>();

    // masters (full fetched data)
    private final ArrayList<HistoryItem> masterItems = new ArrayList<>();
    private final ArrayList<String> masterIds = new ArrayList<>();
    private final ArrayList<String> masterSectionHeaders = new ArrayList<>();
    private final ArrayList<Long> masterEpochs = new ArrayList<>();

    // displayed section headers & epochs
    private final ArrayList<String> sectionHeaders = new ArrayList<>();
    private final ArrayList<Long> epochs = new ArrayList<>();

    private RequestQueue requestQueue;
    private String endpointUrl;
    private String doctorId;

    // live refresh
    private final Handler liveHandler = new Handler();
    private Runnable liveRunnable;
    private boolean isPolling = false;

    // search debounce
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private static final long SEARCH_DEBOUNCE_MS = 300L;

    // diff signature
    private String lastSig = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvHistory    = view.findViewById(R.id.rv_history);
        swipeRefresh = view.findViewById(R.id.swipeRefreshHistory);
        etSearch     = view.findViewById(R.id.et_search);
        btnClear     = view.findViewById(R.id.btn_clear);
        icSearch     = view.findViewById(R.id.ic_search);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new HistoryAdapter(historyItems, appointmentIds);
        rvHistory.setAdapter(historyAdapter);

        // section headers (no adapter change)
        rvHistory.addItemDecoration(new SectionHeaderDecoration(
                sectionHeaders,
                dp(12),  // top margin above header
                dp(36),  // header height
                dp(16),  // header text left padding
                dp(8),   // bottom space per item
                0xFFF3F6FB, // strip bg
                0xFF123B52  // text color
        ));

        requestQueue = Volley.newRequestQueue(requireContext());

        doctorId = getDoctorId();
        endpointUrl = ApiConfig.endpoint("Doctors/gethistory.php", "doctor_id", doctorId);

        // initial load
        fetchHistoryData(false);

        // pull-to-refresh
        swipeRefresh.setOnRefreshListener(() -> fetchHistoryData(false));

        // periodic silent refresh
        liveRunnable = new Runnable() {
            @Override public void run() {
                if (!isAdded()) return;
                fetchHistoryData(true);
                liveHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };

        // search behaviour with debounce
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                btnClear.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                final String q = s == null ? "" : s.toString();
                searchRunnable = () -> applyFilter(q);
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            applyFilter("");
        });
    }

    @Override public void onResume() {
        super.onResume();
        if (!isPolling && liveRunnable != null) {
            isPolling = true;
            liveHandler.postDelayed(liveRunnable, POLL_INTERVAL_MS);
        }
    }

    @Override public void onPause() {
        super.onPause();
        if (isPolling) {
            isPolling = false;
            liveHandler.removeCallbacks(liveRunnable);
        }
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }

    private String getDoctorId() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int id = prefs.getInt("doctor_id", 0);
        return String.valueOf(id);
    }

    private void stopSwipeSpinner() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }

    /** Fetch → sort → sectionize → update masters → apply current filter */
    private void fetchHistoryData(boolean silent) {
        @SuppressLint("NotifyDataSetChanged")
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                endpointUrl,
                null,
                response -> {
                    try {
                        ArrayList<HistoryItem> tItems = new ArrayList<>();
                        ArrayList<String> tIds = new ArrayList<>();
                        ArrayList<Long> tEpochs = new ArrayList<>();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            String apptId          = obj.optString("appointment_id", "");
                            String patientName     = obj.optString("patient_name", "");
                            String appointmentDate = obj.optString("appointment_date", "");
                            String timeSlot        = obj.optString("time_slot", "");
                            String problem         = obj.optString("reason_for_visit", "");
                            boolean flag           = obj.optBoolean("flag", false);
                            String patientId       = obj.optString("patient_id", "");
                            String status          = obj.optString("status", "");

                            tIds.add(apptId);
                            tItems.add(new HistoryItem(
                                    patientName,
                                    appointmentDate,
                                    problem,
                                    flag,
                                    patientId,
                                    apptId,
                                    status
                            ));

                            tEpochs.add(buildSortKey(appointmentDate, timeSlot));
                        }

                        ArrayList<Integer> order = orderDesc(tEpochs, tIds);

                        ArrayList<HistoryItem> fItems = new ArrayList<>();
                        ArrayList<String> fIds = new ArrayList<>();
                        ArrayList<Long> fEpochs = new ArrayList<>();
                        for (int idx : order) {
                            fItems.add(tItems.get(idx));
                            fIds.add(tIds.get(idx));
                            fEpochs.add(tEpochs.get(idx));
                        }

                        ArrayList<String> fHeaders = buildSectionHeaders(fEpochs);

                        String sig = buildSig(fIds, fItems);
                        if (!sig.equals(lastSig)) {
                            masterItems.clear(); masterItems.addAll(fItems);
                            masterIds.clear(); masterIds.addAll(fIds);
                            masterEpochs.clear(); masterEpochs.addAll(fEpochs);
                            masterSectionHeaders.clear(); masterSectionHeaders.addAll(fHeaders);
                            lastSig = sig;

                            String q = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
                            applyFilter(q);
                        }
                    } catch (JSONException ignored) {
                        // silent
                    } finally {
                        stopSwipeSpinner();
                    }
                },
                error -> {
                    // silent on errors during background poll
                    stopSwipeSpinner();
                }
        );

        req.setTag(REQ_TAG);
        requestQueue.add(req);
    }

    /* ========== Filtering / applying view ========== */

    /**
     * Searchable fields: patientName, appointmentDate, appointmentId, status, problem.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void applyFilter(String rawQuery) {
        String q = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.getDefault());

        if (q.isEmpty()) {
            historyItems.clear(); historyItems.addAll(masterItems);
            appointmentIds.clear(); appointmentIds.addAll(masterIds);
            sectionHeaders.clear(); sectionHeaders.addAll(masterSectionHeaders);
            epochs.clear(); epochs.addAll(masterEpochs);

            if (historyAdapter != null) historyAdapter.notifyDataSetChanged();
            rvHistory.invalidateItemDecorations();
            return;
        }

        ArrayList<HistoryItem> fItems = new ArrayList<>();
        ArrayList<String> fIds = new ArrayList<>();
        ArrayList<Long> fEpochs = new ArrayList<>();

        for (int i = 0; i < masterItems.size(); i++) {
            HistoryItem it = masterItems.get(i);
            String id = masterIds.get(i);
            String date = it.getAppointmentDate() == null ? "" : it.getAppointmentDate();
            String patient = it.getPatientName() == null ? "" : it.getPatientName();
            String status = it.getStatus() == null ? "" : it.getStatus();
            String problem = it.getProblem() == null ? "" : it.getProblem();

            boolean match =
                    id.toLowerCase(Locale.getDefault()).contains(q) ||
                            patient.toLowerCase(Locale.getDefault()).contains(q) ||
                            date.toLowerCase(Locale.getDefault()).contains(q) ||
                            status.toLowerCase(Locale.getDefault()).contains(q) ||
                            problem.toLowerCase(Locale.getDefault()).contains(q);

            if (match) {
                fItems.add(it);
                fIds.add(id);
                fEpochs.add(masterEpochs.get(i));
            }
        }

        // rebuild section headers for filtered list
        ArrayList<String> rebuiltHeaders = new ArrayList<>();
        String prev = null;
        for (int i = 0; i < fEpochs.size(); i++) {
            String label = sectionLabel(classifySection(fEpochs.get(i)));
            if (i == 0 || !label.equals(prev)) {
                rebuiltHeaders.add(label);
                prev = label;
            } else {
                rebuiltHeaders.add("");
            }
        }

        historyItems.clear(); historyItems.addAll(fItems);
        appointmentIds.clear(); appointmentIds.addAll(fIds);
        sectionHeaders.clear(); sectionHeaders.addAll(rebuiltHeaders);
        epochs.clear(); epochs.addAll(fEpochs);

        if (historyAdapter != null) historyAdapter.notifyDataSetChanged();
        rvHistory.invalidateItemDecorations();
    }

    /* ---------- signature helper ---------- */
    private String buildSig(List<String> ids, List<HistoryItem> items) {
        StringBuilder sb = new StringBuilder(ids.size() * 12);
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i)).append('|')
                    .append(items.get(i).getStatus() == null ? "" : items.get(i).getStatus()).append(';');
        }
        return sb.toString();
    }

    /* ---------- Date/Section helpers ---------- */

    private enum Section { TODAY, YESTERDAY, LAST7, EARLIER }

    private ArrayList<String> buildSectionHeaders(List<Long> orderedEpochs) {
        ArrayList<String> headers = new ArrayList<>(orderedEpochs.size());
        Section prev = null;
        for (int i = 0; i < orderedEpochs.size(); i++) {
            Section s = classifySection(orderedEpochs.get(i));
            if (i == 0 || s != prev) {
                headers.add(sectionLabel(s));
                prev = s;
            } else {
                headers.add("");
            }
        }
        return headers;
    }

    private Section classifySection(long epoch) {
        if (epoch <= 0) return Section.EARLIER;
        long dayMs = 24L * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        long startToday = startOfDay(now);
        long startYesterday = startToday - dayMs;
        long start7 = startToday - (7L * dayMs);

        if (epoch >= startToday) return Section.TODAY;
        if (epoch >= startYesterday && epoch < startToday) return Section.YESTERDAY;
        if (epoch >= start7 && epoch < startYesterday) return Section.LAST7;
        return Section.EARLIER;
    }

    private String sectionLabel(Section s) {
        switch (s) {
            case TODAY: return "Today";
            case YESTERDAY: return "Yesterday";
            case LAST7: return "Last 7 days";
            default: return "Earlier";
        }
    }

    private long startOfDay(long ts) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Build epoch from date+time with multiple formats. */
    private long buildSortKey(String dateText, String timeText) {
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd hh:mm a",
                "yyyy-MM-dd",
                "dd-MM-yyyy HH:mm:ss",
                "dd-MM-yyyy hh:mm a",
                "dd-MM-yyyy"
        };
        String candidate = (safe(dateText) + " " + safe(timeText)).trim();
        for (String p : patterns) {
            try {
                SimpleDateFormat df = new SimpleDateFormat(p, Locale.getDefault());
                Date d = df.parse(candidate);
                if (d != null) return d.getTime();
            } catch (ParseException ignored) {}
        }
        // fallback on date only
        for (String p : patterns) {
            if (!p.contains(" ")) {
                try {
                    SimpleDateFormat df = new SimpleDateFormat(p, Locale.getDefault());
                    Date d = df.parse(safe(dateText));
                    if (d != null) return d.getTime();
                } catch (ParseException ignored) {}
            }
        }
        return 0L;
    }

    /** order indices by (epoch desc, id desc) */
    @SuppressLint("NewApi")
    private ArrayList<Integer> orderDesc(List<Long> keys, List<String> ids) {
        int n = keys.size();
        ArrayList<Integer> idx = new ArrayList<>(n);
        for (int i = 0; i < n; i++) idx.add(i);
        idx.sort(new Comparator<Integer>() {
            @Override public int compare(Integer a, Integer b) {
                int c = Long.compare(keys.get(b), keys.get(a));
                if (c != 0) return c;
                return ids.get(b).compareTo(ids.get(a));
            }
        });
        return idx;
    }

    private static String safe(String s) { return (s == null) ? "" : s.trim(); }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestQueue != null) {
            requestQueue.cancelAll(REQ_TAG);
        }
        if (liveRunnable != null) liveHandler.removeCallbacks(liveRunnable);
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }

    /* ================== Section Header ItemDecoration ================== */

    private static class SectionHeaderDecoration extends RecyclerView.ItemDecoration {
        private final List<String> headers; // per-position header text or ""
        private final int topMargin;
        private final int headerHeight;
        private final int leftPadding;
        private final int bottomSpace;
        private final int bgColor;
        private final int textColor;

        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        SectionHeaderDecoration(List<String> headers,
                                int topMargin, int headerHeight, int leftPadding, int bottomSpace,
                                int bgColor, int textColor) {
            this.headers = headers;
            this.topMargin = topMargin;
            this.headerHeight = headerHeight;
            this.leftPadding = leftPadding;
            this.bottomSpace = bottomSpace;
            this.bgColor = bgColor;
            this.textColor = textColor;

            bgPaint.setColor(bgColor);
            textPaint.setColor(textColor);
            textPaint.setTextSize(40f);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int pos = parent.getChildAdapterPosition(view);
            if (pos == RecyclerView.NO_POSITION) return;

            String h = (pos < headers.size()) ? headers.get(pos) : "";
            if (h != null && !h.isEmpty()) {
                outRect.top = topMargin + headerHeight;
                outRect.bottom = bottomSpace;
            } else {
                outRect.top = 0;
                outRect.bottom = bottomSpace;
            }
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent,
                           @NonNull RecyclerView.State state) {
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                int pos = parent.getChildAdapterPosition(child);
                if (pos == RecyclerView.NO_POSITION) continue;

                String header = (pos < headers.size()) ? headers.get(pos) : "";
                if (header == null || header.isEmpty()) continue;

                int left = parent.getPaddingLeft();
                int right = parent.getWidth() - parent.getPaddingRight();
                int top = child.getTop() - headerHeight - topMargin;
                int bottom = top + headerHeight;

                c.drawRect(left, top, right, bottom, bgPaint);

                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float textY = top + (headerHeight - fm.bottom + fm.top) / 2f - fm.top;

                c.drawText(header, left + leftPadding, textY, textPaint);
            }
        }
    }
}
