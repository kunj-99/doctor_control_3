package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettlementDetailsActivity extends AppCompatActivity {

    // API
    private static final String API_PAYMENT_CONTEXT = "Doctors/get_settlement_payment_context.php";
    private static final String API_SUBMIT_PROOF    = "Doctors/submit_settlement_payment_proof.php";

    // Intent extras
    public static final String EXTRA_SUMMARY_ID = "extra_summary_id";
    public static final String EXTRA_DOCTOR_ID  = "extra_doctor_id";

    private RequestQueue queue;

    // Views
    private MaterialToolbar toolbar;

    private TextView tvTitle, tvMode, tvAmount, tvQrHint, tvStatus;

    private MaterialCardView cardQr, cardUpload, cardDoctorProof, cardAdminProof;

    private ImageView ivQr, ivSelectedProof, ivDoctorProof, ivAdminProof;

    private TextView tvQrLoading, tvSelectedFile, tvDoctorTxnStatus, tvDoctorTxnNote, tvAdminProofTitle, tvAdminProofStatus;

    private MaterialButton btnChooseProof, btnSubmitProof;

    private ProgressBar progressBar;

    // State
    private int doctorId;
    private int summaryId;
    private Uri selectedProofUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                selectedProofUri = uri;

                String name = getFileName(uri);
                if (TextUtils.isEmpty(name)) name = uri.toString();
                safeSetText(tvSelectedFile, "Selected: " + name);

                if (ivSelectedProof != null) {
                    ivSelectedProof.setImageURI(uri);
                    ivSelectedProof.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settlement_details);

        View main = findViewById(R.id.main);
        if (main != null) {
            ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        queue = Volley.newRequestQueue(this);
        bindViews();

        doctorId = getIntExtraFallback(EXTRA_DOCTOR_ID, "doctor_id", -1);
        summaryId = getIntExtraFallback(EXTRA_SUMMARY_ID, "summary_id", -1);

        if (doctorId <= 0 || summaryId <= 0) {
            Toast.makeText(this, "Invalid settlement details.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        showNoneMode();

        if (btnChooseProof != null) {
            btnChooseProof.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }

        if (btnSubmitProof != null) {
            btnSubmitProof.setOnClickListener(v -> {
                if (selectedProofUri == null) {
                    Toast.makeText(this, "Please select screenshot first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                submitPaymentProof(selectedProofUri);
            });
        }

        fetchPaymentContext();
    }

    private void bindViews() {
        toolbar = findViewByIdOrNull(R.id.toolbar);

        tvTitle  = findViewByIdOrNull(R.id.tvTitle);
        tvMode   = findViewByIdOrNull(R.id.tvMode);
        tvAmount = findViewByIdOrNull(R.id.tvAmount);
        tvQrHint = findViewByIdOrNull(R.id.tvQrHint);
        tvStatus = findViewByIdOrNull(R.id.tvStatus);

        cardQr         = findViewByIdOrNull(R.id.cardQr);
        ivQr           = findViewByIdOrNull(R.id.ivQr);
        tvQrLoading    = findViewByIdOrNull(R.id.tvQrLoading);

        cardUpload      = findViewByIdOrNull(R.id.cardUpload);
        btnChooseProof  = findViewByIdOrNull(R.id.btnChooseProof);
        tvSelectedFile  = findViewByIdOrNull(R.id.tvSelectedFile);
        ivSelectedProof = findViewByIdOrNull(R.id.ivSelectedProof);
        btnSubmitProof  = findViewByIdOrNull(R.id.btnSubmitProof);

        cardDoctorProof    = findViewByIdOrNull(R.id.cardDoctorProof);
        tvDoctorTxnStatus  = findViewByIdOrNull(R.id.tvDoctorTxnStatus);
        tvDoctorTxnNote    = findViewByIdOrNull(R.id.tvDoctorTxnNote);
        ivDoctorProof      = findViewByIdOrNull(R.id.ivDoctorProof);

        cardAdminProof      = findViewByIdOrNull(R.id.cardAdminProof);
        tvAdminProofTitle   = findViewByIdOrNull(R.id.tvAdminProofTitle);
        ivAdminProof        = findViewByIdOrNull(R.id.ivAdminProof);
        tvAdminProofStatus  = findViewByIdOrNull(R.id.tvAdminProofStatus);

        progressBar = findViewByIdOrNull(R.id.progressBar);

        safeSetVisible(ivSelectedProof, false);
        safeSetVisible(ivDoctorProof, false);
        safeSetVisible(ivAdminProof, false);
        safeSetVisible(tvDoctorTxnNote, false);
        safeSetVisible(tvAdminProofTitle, false);

        // Tap-to-open viewer
        if (ivDoctorProof != null) {
            ivDoctorProof.setOnClickListener(v -> {
                String url = (ivDoctorProof.getTag() instanceof String) ? (String) ivDoctorProof.getTag() : null;
                if (!TextUtils.isEmpty(url)) showImageViewerFromUrl(url, "Doctor Payment Proof");
            });
        }
        if (ivAdminProof != null) {
            ivAdminProof.setOnClickListener(v -> {
                String url = (ivAdminProof.getTag() instanceof String) ? (String) ivAdminProof.getTag() : null;
                if (!TextUtils.isEmpty(url)) showImageViewerFromUrl(url, "Admin Payment Proof");
            });
        }
        if (ivSelectedProof != null) {
            ivSelectedProof.setOnClickListener(v -> {
                if (selectedProofUri != null) showImageViewerFromUri(selectedProofUri, "Selected Screenshot");
            });
        }
        if (ivQr != null) {
            ivQr.setOnClickListener(v -> {
                String url = (ivQr.getTag() instanceof String) ? (String) ivQr.getTag() : null;
                if (!TextUtils.isEmpty(url)) showImageViewerFromUrl(url, "Payment QR");
            });
        }
    }

    private void fetchPaymentContext() {
        safeSetLoading(true);

        try {
            String url = ApiConfig.endpoint(
                    API_PAYMENT_CONTEXT,
                    "doctor_id", URLEncoder.encode(String.valueOf(doctorId), StandardCharsets.UTF_8.name()),
                    "summary_id", URLEncoder.encode(String.valueOf(summaryId), StandardCharsets.UTF_8.name())
            );

            StringRequest req = new StringRequest(Request.Method.GET, url, response -> {
                safeSetLoading(false);

                try {
                    JSONObject root = new JSONObject(response);
                    if (!root.optBoolean("success", false)) {
                        Toast.makeText(this, root.optString("message", "Failed"), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONObject data = root.optJSONObject("data");
                    if (data == null) {
                        Toast.makeText(this, "Invalid server response.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    applyContextToUi(data);

                } catch (Exception e) {
                    Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, err -> {
                safeSetLoading(false);
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
            });

            req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
            queue.add(req);

        } catch (Exception e) {
            safeSetLoading(false);
            Toast.makeText(this, "Build URL failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void submitPaymentProof(Uri proofUri) {
        safeSetLoading(true);
        setUploadEnabled(false);

        String url = normalizeUrl(API_SUBMIT_PROOF);

        String method = "UPI";
        String utrNumber = "";
        String description = "Settlement payment proof";

        MultipartStringRequest req = new MultipartStringRequest(
                Request.Method.POST,
                url,
                response -> {
                    safeSetLoading(false);
                    setUploadEnabled(true);

                    try {
                        JSONObject root = new JSONObject(response);
                        boolean ok = root.optBoolean("success", false);
                        String msg = root.optString("message", ok ? "Submitted" : "Failed");

                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

                        if (ok) {
                            resetSelectedProofUi();
                            fetchPaymentContext();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    safeSetLoading(false);
                    setUploadEnabled(true);

                    String msg = "Upload failed";
                    String serverBody = extractServerError(error);
                    if (!TextUtils.isEmpty(serverBody)) msg = serverBody;

                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        );

        req.addFormField("doctor_id", String.valueOf(doctorId));
        req.addFormField("summary_id", String.valueOf(summaryId));
        req.addFormField("method", method);
        req.addFormField("utr_number", utrNumber);
        req.addFormField("description", description);

        String mime = getMimeType(proofUri);
        if (TextUtils.isEmpty(mime)) mime = "image/*";

        String fileName = getFileName(proofUri);
        if (TextUtils.isEmpty(fileName)) {
            fileName = "proof_" + System.currentTimeMillis() + ".jpg";
        }

        try {
            byte[] fileBytes = readBytesFromUri(proofUri);
            req.addFilePart("proof_file", fileName, mime, fileBytes);
        } catch (Exception e) {
            safeSetLoading(false);
            setUploadEnabled(true);
            Toast.makeText(this, "File read failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        req.setRetryPolicy(new DefaultRetryPolicy(30000, 0, 1.0f));
        queue.add(req);
    }

    private String extractServerError(VolleyError error) {
        try {
            if (error != null && error.networkResponse != null && error.networkResponse.data != null) {
                return new String(error.networkResponse.data, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void setUploadEnabled(boolean enabled) {
        if (btnChooseProof != null) btnChooseProof.setEnabled(enabled);
        if (btnSubmitProof != null) btnSubmitProof.setEnabled(enabled);
    }

    private void resetSelectedProofUi() {
        selectedProofUri = null;
        if (tvSelectedFile != null) tvSelectedFile.setText("No file selected");
        safeSetVisible(ivSelectedProof, false);
    }

    private byte[] readBytesFromUri(Uri uri) throws Exception {
        if (uri == null) throw new Exception("Uri is null");

        try (InputStream is = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            if (is == null) throw new Exception("Unable to open stream");

            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }

    private String getMimeType(Uri uri) {
        try {
            return getContentResolver().getType(uri);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getFileName(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                String name = (nameIndex >= 0) ? cursor.getString(nameIndex) : null;
                cursor.close();
                return name;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private boolean isTerminalDoctorStatus(String status) {
        if (TextUtils.isEmpty(status)) return false;
        return "Approved".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status);
    }

    private void applyContextToUi(JSONObject data) {
        JSONObject summary       = data.optJSONObject("summary");
        JSONObject adminQr       = data.optJSONObject("admin_qr");
        JSONObject doctorToAdmin = data.optJSONObject("doctor_to_admin");
        JSONObject adminToDoctor = data.optJSONObject("admin_to_doctor");
        JSONObject flags         = data.optJSONObject("flags");

        double givenToDoctor      = summary != null ? summary.optDouble("given_to_doctor", 0) : 0;
        double receivedFromDoctor = summary != null ? summary.optDouble("received_from_doctor", 0) : 0;

        String modeHint = cleanJsonString(data, "mode_hint");

        // New API fields
        boolean qrShowFlag = adminQr != null && adminQr.optBoolean("show", false);
        String qrImageUrl  = cleanJsonString(adminQr, "qr_image");      // absolute from API if shown

        String adminProofUrl  = cleanJsonString(adminToDoctor, "payment_proof_url"); // absolute if present
        String doctorProofUrl = cleanJsonString(doctorToAdmin, "payment_proof_url"); // absolute if present
        String doctorStatus   = cleanJsonString(doctorToAdmin, "status");
        String adminNote      = cleanJsonString(doctorToAdmin, "admin_note");

        boolean finalized = flags != null && flags.optBoolean("finalized", false);

        if (TextUtils.isEmpty(doctorStatus)) doctorStatus = "Pending";

        safeSetText(tvTitle, String.format(Locale.ROOT, "Settlement #%d", summaryId));

        // If finalized, show read-only proofs (both sides if available), always hide QR/upload
        if (finalized) {
            showFinalizedMode(givenToDoctor, receivedFromDoctor, doctorStatus, doctorProofUrl, adminProofUrl, adminNote);
            return;
        }

        // Prefer modeHint from PHP. Backstop with amounts if missing.
        boolean isPayMode = "PAY_TO_ADMIN".equalsIgnoreCase(modeHint)
                || (TextUtils.isEmpty(modeHint) && receivedFromDoctor > 0);
        boolean isReceiveMode = "RECEIVE_FROM_ADMIN".equalsIgnoreCase(modeHint)
                || (TextUtils.isEmpty(modeHint) && !isPayMode && givenToDoctor > 0);

        if (isPayMode) {
            // QR visibility controlled strictly by server flag
            showPayToAdminMode(receivedFromDoctor, qrShowFlag ? qrImageUrl : "", doctorStatus, doctorProofUrl, adminNote, qrShowFlag);
        } else if (isReceiveMode) {
            showReceiveFromAdminMode(givenToDoctor, adminProofUrl);
        } else {
            showNoneMode();
        }
    }

    private void showFinalizedMode(double amountGivenToDoctor,
                                   double amountReceivedFromDoctor,
                                   String doctorStatus,
                                   String doctorProofUrl,
                                   String adminProofUrl,
                                   String adminNote) {

        safeSetText(tvMode, "Finalized");
        safeSetText(tvAmount,
                String.format(Locale.ROOT,
                        "Doctor→Admin: ₹%.2f   |   Admin→Doctor: ₹%.2f",
                        amountReceivedFromDoctor, amountGivenToDoctor));
        // For clarity, reflect doctor-side final status
        safeSetText(tvStatus, "Doctor→Admin Status: " + doctorStatus);
        safeSetText(tvQrHint, "This settlement is finalized. Proofs are shown below.");

        // Hide QR & upload
        safeSetVisible(cardQr, false);
        safeSetVisible(cardUpload, false);

        // Doctor proof (if any)
        boolean hasDoctorProof = !TextUtils.isEmpty(doctorProofUrl);
        safeSetVisible(cardDoctorProof, hasDoctorProof);
        if (hasDoctorProof) {
            safeSetText(tvDoctorTxnStatus, "Status: " + doctorStatus);
            if (!TextUtils.isEmpty(adminNote)) {
                safeSetText(tvDoctorTxnNote, adminNote);
                safeSetVisible(tvDoctorTxnNote, true);
            } else {
                safeSetVisible(tvDoctorTxnNote, false);
            }
            safeSetVisible(ivDoctorProof, true);
            ivDoctorProof.setTag(doctorProofUrl);
            loadImageInto(ivDoctorProof, doctorProofUrl, null, () -> safeSetVisible(ivDoctorProof, false));
        }

        // Admin proof (if any)
        boolean hasAdminProof = !TextUtils.isEmpty(adminProofUrl);
        safeSetVisible(cardAdminProof, hasAdminProof);
        if (hasAdminProof) {
            safeSetVisible(tvAdminProofTitle, true);
            safeSetText(tvAdminProofStatus, "Proof status: Uploaded");
            safeSetVisible(ivAdminProof, true);
            ivAdminProof.setTag(adminProofUrl);
            loadImageInto(ivAdminProof, adminProofUrl, null, () -> {
                safeSetVisible(ivAdminProof, false);
                safeSetText(tvAdminProofStatus, "Proof status: Failed to load (check path/file)");
            });
        }

        // lock all upload controls
        setUploadEnabled(false);
        safeSetVisible(ivSelectedProof, false);
        selectedProofUri = null;
        if (tvSelectedFile != null) tvSelectedFile.setText("No file selected");
    }

    private void showPayToAdminMode(double amountToPay,
                                    String qrImageUrl,
                                    String doctorStatus,
                                    String doctorProofUrl,
                                    String adminNote,
                                    boolean shouldShowQr) {

        safeSetText(tvMode, "Mode: Pay to Admin");
        safeSetText(tvAmount, String.format(Locale.ROOT, "Amount: ₹%.2f", amountToPay));
        safeSetText(tvStatus, "Status: " + doctorStatus);

        // QR visibility is controlled by server flag now
        if (!shouldShowQr || isTerminalDoctorStatus(doctorStatus)) {
            // Hide QR & upload (read-only)
            safeSetText(tvQrHint, "This settlement is not awaiting your payment.");
            safeSetVisible(cardQr, false);
            safeSetVisible(cardUpload, false);
        } else {
            // Show QR & upload
            safeSetText(tvQrHint, "Scan QR to pay and upload proof.");
            safeSetVisible(cardQr, true);
            safeSetVisible(cardUpload, true);

            if (TextUtils.isEmpty(qrImageUrl)) {
                safeSetVisible(ivQr, false);
                safeSetVisible(tvQrLoading, true);
                safeSetText(tvQrLoading, "QR not available. Contact admin.");
            } else {
                safeSetVisible(tvQrLoading, true);
                safeSetText(tvQrLoading, "Loading QR...");
                safeSetVisible(ivQr, true);
                ivQr.setTag(qrImageUrl);
                loadImageInto(ivQr, qrImageUrl,
                        () -> safeSetVisible(tvQrLoading, false),
                        () -> {
                            safeSetVisible(tvQrLoading, true);
                            safeSetText(tvQrLoading, "QR failed to load (check file/path on server).");
                        }
                );
            }
        }

        // Show Doctor proof (if any) always read-only
        safeSetVisible(cardDoctorProof, true);
        safeSetText(tvDoctorTxnStatus, "Status: " + doctorStatus);

        if (!TextUtils.isEmpty(adminNote)) {
            safeSetText(tvDoctorTxnNote, adminNote);
            safeSetVisible(tvDoctorTxnNote, true);
        } else {
            safeSetVisible(tvDoctorTxnNote, false);
        }

        if (!TextUtils.isEmpty(doctorProofUrl)) {
            safeSetVisible(ivDoctorProof, true);
            ivDoctorProof.setTag(doctorProofUrl);
            loadImageInto(ivDoctorProof, doctorProofUrl, null, () -> safeSetVisible(ivDoctorProof, false));
        } else {
            safeSetVisible(ivDoctorProof, false);
        }

        // Admin proof card hidden in pay mode
        safeSetVisible(cardAdminProof, false);

        // Upload enablement: lock for Approved/Submitted, allow for Pending/Rejected (Rejected handled server-side but keep UX)
        boolean lockUpload = "Approved".equalsIgnoreCase(doctorStatus) || "Submitted".equalsIgnoreCase(doctorStatus) || !shouldShowQr;
        if (btnChooseProof != null) btnChooseProof.setEnabled(!lockUpload);
        if (btnSubmitProof != null) btnSubmitProof.setEnabled(!lockUpload);

        if (selectedProofUri == null) safeSetVisible(ivSelectedProof, false);
    }

    @SuppressLint("SetTextI18n")
    private void showReceiveFromAdminMode(double amountToReceive, String adminProofUrl) {
        safeSetText(tvMode, "Mode: Receive from Admin");
        safeSetText(tvAmount, String.format(Locale.ROOT, "Amount: ₹%.2f", amountToReceive));
        safeSetText(tvStatus, "Status: Pending (Admin → Doctor)");
        safeSetText(tvQrHint, "Admin will upload proof here.");

        safeSetVisible(cardQr, false);
        safeSetVisible(cardUpload, false);
        safeSetVisible(cardDoctorProof, false);
        safeSetVisible(cardAdminProof, true);

        safeSetVisible(tvAdminProofTitle, true);

        if (TextUtils.isEmpty(adminProofUrl)) {
            safeSetVisible(ivAdminProof, false);
            safeSetText(tvAdminProofStatus, "Proof status: Not uploaded yet");
        } else {
            safeSetText(tvAdminProofStatus, "Proof status: Uploaded");
            safeSetVisible(ivAdminProof, true);
            ivAdminProof.setTag(adminProofUrl);
            loadImageInto(ivAdminProof, adminProofUrl, null, () -> {
                safeSetVisible(ivAdminProof, false);
                safeSetText(tvAdminProofStatus, "Proof status: Failed to load (check path/file)");
            });
        }

        selectedProofUri = null;
        if (tvSelectedFile != null) tvSelectedFile.setText("No file selected");
        safeSetVisible(ivSelectedProof, false);
    }

    @SuppressLint("SetTextI18n")
    private void showNoneMode() {
        safeSetText(tvMode, "Mode: -");
        safeSetText(tvAmount, "Amount: ₹0.00");
        safeSetText(tvStatus, "Status: N/A");
        safeSetText(tvQrHint, "");

        safeSetVisible(cardQr, false);
        safeSetVisible(cardUpload, false);
        safeSetVisible(cardDoctorProof, false);
        safeSetVisible(cardAdminProof, false);

        selectedProofUri = null;
        if (tvSelectedFile != null) tvSelectedFile.setText("No file selected");
        safeSetVisible(ivSelectedProof, false);
        setUploadEnabled(true);
    }

    private void loadImageInto(ImageView target, String url, @Nullable Runnable onSuccess, @Nullable Runnable onError) {
        if (target == null || TextUtils.isEmpty(url)) {
            if (onError != null) onError.run();
            return;
        }

        ImageRequest imgReq = new ImageRequest(
                url,
                (Bitmap bitmap) -> {
                    target.setImageBitmap(bitmap);
                    if (onSuccess != null) onSuccess.run();
                },
                0, 0,
                ImageView.ScaleType.FIT_CENTER,
                Bitmap.Config.ARGB_8888,
                error -> {
                    Toast.makeText(this, "Image failed: " + url, Toast.LENGTH_SHORT).show();
                    if (onError != null) onError.run();
                }
        );

        imgReq.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        queue.add(imgReq);
    }

    private String cleanJsonString(JSONObject obj, String key) {
        if (obj == null) return "";
        if (obj.isNull(key)) return "";
        String v = obj.optString(key, "");
        if (v == null) return "";
        v = v.trim();
        if (v.isEmpty()) return "";
        if ("null".equalsIgnoreCase(v)) return "";
        return v;
    }

    private String normalizeUrl(String maybeRelative) {
        if (TextUtils.isEmpty(maybeRelative)) return "";
        String s = maybeRelative.trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return "";

        if (s.startsWith("http://") || s.startsWith("https://")) return s;

        String base = ApiConfig.BASE_URL;
        if (!base.endsWith("/")) base += "/";
        s = s.startsWith("/") ? s.substring(1) : s;
        return base + s;
    }

    private void safeSetLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void safeSetVisible(View v, boolean visible) {
        if (v == null) return;
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void safeSetText(TextView tv, String text) {
        if (tv == null) return;
        tv.setText(text == null ? "" : text);
    }

    private int getIntExtraFallback(String key1, String key2, int def) {
        int v1 = getIntent().getIntExtra(key1, def);
        if (v1 != def) return v1;
        return getIntent().getIntExtra(key2, def);
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T findViewByIdOrNull(int id) {
        try {
            return findViewById(id);
        } catch (Exception ignored) {
            return null;
        }
    }

    // -----------------------------
    // Full-screen Image Viewer (no extra files)
    // -----------------------------
    private void showImageViewerFromUrl(String url, String title) {
        if (TextUtils.isEmpty(url)) return;

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Root
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(0xCC000000);

        // Zoom image
        ZoomImageView zoomImage = new ZoomImageView(this);
        zoomImage.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(zoomImage);

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        FrameLayout.LayoutParams tbLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        tbLp.gravity = Gravity.TOP;
        topBar.setLayoutParams(tbLp);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), 0, dp(8), 0);
        topBar.setBackgroundColor(0x66000000);

        ImageView ivClose = new ImageView(this);
        LinearLayout.LayoutParams clLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        ivClose.setLayoutParams(clLp);
        ivClose.setPadding(dp(6), dp(6), dp(6), dp(6));
        ivClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        ivClose.setColorFilter(Color.WHITE);
        ivClose.setOnClickListener(v -> dialog.dismiss());
        topBar.addView(ivClose);

        TextView tvTitle = new TextView(this);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvTitle.setLayoutParams(tLp);
        tvTitle.setText(TextUtils.isEmpty(title) ? "Preview" : title);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(16);
        tvTitle.setPadding(dp(8), 0, 0, 0);
        topBar.addView(tvTitle);

        root.addView(topBar);

        // Loading label
        TextView tvLoading = new TextView(this);
        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.gravity = Gravity.CENTER;
        tvLoading.setLayoutParams(llp);
        tvLoading.setText("Loading...");
        tvLoading.setTextColor(Color.WHITE);
        tvLoading.setTextSize(14);
        root.addView(tvLoading);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageRequest req = new ImageRequest(
                url,
                bmp -> {
                    tvLoading.setVisibility(View.GONE);
                    zoomImage.setImageBitmap(bmp);
                },
                0, 0,
                ImageView.ScaleType.FIT_CENTER,
                Bitmap.Config.ARGB_8888,
                error -> {
                    tvLoading.setText("Failed to load image");
                    tvLoading.setVisibility(View.VISIBLE);
                }
        );
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        queue.add(req);

        dialog.show();
    }

    @SuppressLint("SetTextI18n")
    private void showImageViewerFromUri(Uri uri, String title) {
        if (uri == null) return;

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Root
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(0xCC000000);

        // Zoom image
        ZoomImageView zoomImage = new ZoomImageView(this);
        zoomImage.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(zoomImage);

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        FrameLayout.LayoutParams tbLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        tbLp.gravity = Gravity.TOP;
        topBar.setLayoutParams(tbLp);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), 0, dp(8), 0);
        topBar.setBackgroundColor(0x66000000);

        ImageView ivClose = new ImageView(this);
        LinearLayout.LayoutParams clLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        ivClose.setLayoutParams(clLp);
        ivClose.setPadding(dp(6), dp(6), dp(6), dp(6));
        ivClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        ivClose.setColorFilter(Color.WHITE);
        ivClose.setOnClickListener(v -> dialog.dismiss());
        topBar.addView(ivClose);

        TextView tvTitle = new TextView(this);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvTitle.setLayoutParams(tLp);
        tvTitle.setText(TextUtils.isEmpty(title) ? "Preview" : title);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(16);
        tvTitle.setPadding(dp(8), 0, 0, 0);
        topBar.addView(tvTitle);

        root.addView(topBar);

        // Loading label
        TextView tvLoading = new TextView(this);
        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.gravity = Gravity.CENTER;
        tvLoading.setLayoutParams(llp);
        tvLoading.setText("Loading...");
        tvLoading.setTextColor(Color.WHITE);
        tvLoading.setTextSize(14);
        root.addView(tvLoading);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        try {
            tvLoading.setVisibility(View.GONE);
            zoomImage.setImageURI(uri);
        } catch (Exception e) {
            tvLoading.setText("Failed to open image");
            tvLoading.setVisibility(View.VISIBLE);
        }

        dialog.show();
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    // -----------------------------
    // ZoomImageView (inner class, no extra file)
    // -----------------------------
    private static class ZoomImageView extends AppCompatImageView {

        private static final float MIN_SCALE = 1.0f;
        private static final float MAX_SCALE = 5.0f;

        private final Matrix matrix = new Matrix();
        private final float[] m = new float[9];

        private float saveScale = 1.0f;
        private float origWidth, origHeight;
        private float viewWidth, viewHeight;

        private final android.view.ScaleGestureDetector scaleDetector;
        private final GestureDetector gestureDetector;

        private final PointF last = new PointF();
        private final PointF start = new PointF();
        private int mode = NONE;

        private static final int NONE = 0;
        private static final int DRAG = 1;
        private static final int ZOOM = 2;

        public ZoomImageView(android.content.Context context) {
            this(context, null);
        }

        public ZoomImageView(android.content.Context context, android.util.AttributeSet attrs) {
            super(context, attrs);
            super.setClickable(true);
            setScaleType(ScaleType.MATRIX);
            setImageMatrix(matrix);

            scaleDetector = new android.view.ScaleGestureDetector(context, new ScaleListener());
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onDoubleTap(MotionEvent e) {
                    float target = (saveScale < 2.0f) ? 2.0f : 1.0f;
                    zoomTo(target, e.getX(), e.getY());
                    return true;
                }
            });
        }

        private void zoomTo(float targetScale, float focusX, float focusY) {
            targetScale = Math.max(MIN_SCALE, Math.min(targetScale, MAX_SCALE));
            float factor = targetScale / saveScale;
            matrix.postScale(factor, factor, focusX, focusY);
            saveScale = targetScale;
            fixTrans();
            setImageMatrix(matrix);
            invalidate();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            gestureDetector.onTouchEvent(event);
            scaleDetector.onTouchEvent(event);

            PointF curr = new PointF(event.getX(), event.getY());

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    last.set(curr);
                    start.set(last);
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        float dx = curr.x - last.x;
                        float dy = curr.y - last.y;
                        matrix.postTranslate(dx, dy);
                        fixTrans();
                        last.set(curr.x, curr.y);
                        setImageMatrix(matrix);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
            }
            return true;
        }

        private class ScaleListener extends android.view.ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override public boolean onScaleBegin(@NonNull android.view.ScaleGestureDetector detector) {
                mode = ZOOM;
                return true;
            }

            @Override public boolean onScale(android.view.ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float origScale = saveScale;
                saveScale *= scaleFactor;

                if (saveScale > MAX_SCALE) {
                    saveScale = MAX_SCALE;
                    scaleFactor = MAX_SCALE / origScale;
                } else if (saveScale < MIN_SCALE) {
                    saveScale = MIN_SCALE;
                    scaleFactor = MIN_SCALE / origScale;
                }

                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                fixTrans();
                setImageMatrix(matrix);
                return true;
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            viewWidth = w;
            viewHeight = h;
            fitImageToView();
        }

        @Override
        public void setImageDrawable(android.graphics.drawable.Drawable drawable) {
            super.setImageDrawable(drawable);
            fitImageToView();
        }

        private void fitImageToView() {
            if (getDrawable() == null) return;

            float bmWidth = getDrawable().getIntrinsicWidth();
            float bmHeight = getDrawable().getIntrinsicHeight();

            float scale = Math.min(viewWidth / bmWidth, viewHeight / bmHeight);
            if (scale <= 0 || Float.isInfinite(scale) || Float.isNaN(scale)) scale = 1f;
            matrix.setScale(scale, scale);

            float redundantYSpace = (viewHeight - (scale * bmHeight)) / 2.0f;
            float redundantXSpace = (viewWidth - (scale * bmWidth)) / 2.0f;
            matrix.postTranslate(redundantXSpace, redundantYSpace);

            origWidth = viewWidth - 2 * redundantXSpace;
            origHeight = viewHeight - 2 * redundantYSpace;
            saveScale = 1.0f;

            setImageMatrix(matrix);
        }

        private void fixTrans() {
            matrix.getValues(m);
            float transX = m[Matrix.MTRANS_X];
            float transY = m[Matrix.MTRANS_Y];

            float fixTransX = getFixTrans(transX, viewWidth, origWidth * saveScale);
            float fixTransY = getFixTrans(transY, viewHeight, origHeight * saveScale);

            if (fixTransX != 0 || fixTransY != 0) {
                matrix.postTranslate(fixTransX, fixTransY);
            }
        }

        private float getFixTrans(float trans, float viewSize, float contentSize) {
            float minTrans, maxTrans;

            if (contentSize <= viewSize) {
                minTrans = 0;
                maxTrans = viewSize - contentSize;
            } else {
                minTrans = viewSize - contentSize;
                maxTrans = 0;
            }

            if (trans < minTrans) return -trans + minTrans;
            if (trans > maxTrans) return -trans + maxTrans;
            return 0;
        }

        public RectF getDisplayRect() {
            if (getDrawable() == null) return null;
            Matrix drawMatrix = new Matrix(matrix);
            RectF rect = new RectF(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            drawMatrix.mapRect(rect);
            return rect;
        }
    }

    // -----------------------------
    // Multipart Volley Request
    // -----------------------------
    static class MultipartStringRequest extends Request<String> {

        private final Response.Listener<String> listener;
        private final Response.ErrorListener errorListener;

        private final String boundary = "----DoctorAtHomeBoundary" + System.currentTimeMillis();
        private final String mimeType = "multipart/form-data; boundary=" + boundary;

        private final Map<String, String> formFields = new HashMap<>();
        private final Map<String, FilePart> fileParts = new HashMap<>();

        MultipartStringRequest(int method, String url,
                               Response.Listener<String> listener,
                               Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            this.listener = listener;
            this.errorListener = errorListener;
        }

        void addFormField(String key, String value) {
            if (key == null) return;
            formFields.put(key, value == null ? "" : value);
        }

        void addFilePart(String fieldName, String fileName, String contentType, byte[] data) {
            if (fieldName == null || data == null) return;
            fileParts.put(fieldName, new FilePart(fileName, contentType, data));
        }

        @Override
        public String getBodyContentType() {
            return mimeType;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                Charset utf8 = Charset.forName("UTF-8");

                for (Map.Entry<String, String> e : formFields.entrySet()) {
                    bos.write(("--" + boundary + "\r\n").getBytes(utf8));
                    bos.write(("Content-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n").getBytes(utf8));
                    bos.write(("Content-Type: text/plain; charset=UTF-8\r\n\r\n").getBytes(utf8));
                    bos.write((e.getValue() + "\r\n").getBytes(utf8));
                }

                for (Map.Entry<String, FilePart> e : fileParts.entrySet()) {
                    FilePart p = e.getValue();
                    bos.write(("--" + boundary + "\r\n").getBytes(utf8));
                    bos.write(("Content-Disposition: form-data; name=\"" + e.getKey() + "\"; filename=\"" + safeFileName(p.fileName) + "\"\r\n")
                            .getBytes(utf8));
                    bos.write(("Content-Type: " + (p.contentType == null ? "application/octet-stream" : p.contentType) + "\r\n").getBytes(utf8));
                    bos.write(("Content-Transfer-Encoding: binary\r\n\r\n").getBytes(utf8));
                    bos.write(p.data);
                    bos.write("\r\n".getBytes(utf8));
                }

                bos.write(("--" + boundary + "--\r\n").getBytes(utf8));
                return bos.toByteArray();

            } catch (Exception ex) {
                throw new AuthFailureError("Multipart build failed: " + ex.getMessage(), ex);
            }
        }

        private String safeFileName(String n) {
            if (n == null) return "file.jpg";
            return n.replace("\"", "");
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            try {
                String charset = HttpHeaderParser.parseCharset(response.headers, "UTF-8");
                String res = new String(response.data, charset);
                return Response.success(res, HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new ParseError(e));
            }
        }

        @Override
        protected void deliverResponse(String response) {
            if (listener != null) listener.onResponse(response);
        }

        @Override
        public void deliverError(VolleyError error) {
            if (errorListener != null) errorListener.onErrorResponse(error);
            else super.deliverError(error);
        }

        static class FilePart {
            final String fileName;
            final String contentType;
            final byte[] data;

            FilePart(String fileName, String contentType, byte[] data) {
                this.fileName = fileName;
                this.contentType = contentType;
                this.data = data;
            }
        }
    }
}
