package com.infowave.doctor_control;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
//noinspection ExifInterface
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AnimalVirtualReportActivity extends AppCompatActivity {

    private static final String TAG = "AnimalReport";

    // ====== Image controls (quality & sizing) ======
    private static final int MAX_IMAGE_SIDE_PX = 2560; // big enough for text readability
    private static final int JPEG_QUALITY = 94;

    // UI
    private RadioGroup radioGroupUploadType;
    private RadioButton radioVirtualReport, radioDirectUpload;
    private LinearLayout virtualForm, directUploadSection;
    private ImageView ivReportImage;
    private android.widget.Button btnCaptureImage, btnUploadImage, btnAddMedicine, btnSave;
    private LinearLayout medsContainer;

    // Virtual form fields
    private TextInputEditText etAnimalName, etSpeciesBreed, etSex, etAge, etWeight,
            etAddress, etDate, etTemperature, etPulse, etSpo2, etBloodPressure,
            etRespiratoryRateBpm, etPainScore, etHydrationStatus, etMucousMembranes,
            etCrtSec, etBehaviorGait, etSkinCoat, etSymptoms, etRespiratorySystem,
            etReasons, etInvestigationNotes, etSignature, etReportType;

    private CheckBox cbInvestigation;
    private TextInputLayout tilInvestigationNotes;

    // Image capture/pick
    private Uri cameraImageUri = null; // holds either captured or picked image

    // Networking
    private RequestQueue requestQueue;
    private String appointmentId;

    // ======== Activity Result API ========
    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openCamera();
                else toast("Camera permission denied");
            });

    private final ActivityResultLauncher<String[]> requestGalleryPermissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = true;
                for (Boolean ok : result.values()) granted &= (ok != null && ok);
                if (granted) openGallery();
                else toast("Storage permission denied");
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success) && cameraImageUri != null) {
                    // Show a preview; full-res is at the Uri
                    ivReportImage.setVisibility(View.VISIBLE);
                    ivReportImage.setImageURI(cameraImageUri);
                } else {
                    toast("Camera capture cancelled");
                }
            });

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    cameraImageUri = uri; // reuse field to point to selected image
                    ivReportImage.setVisibility(View.VISIBLE);
                    ivReportImage.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_animal_virtual_report);

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);

        final View root = findViewById(R.id.root_container_animal);
        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim    = findViewById(R.id.navigation_bar_scrim);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (statusScrim != null) {
                statusScrim.getLayoutParams().height = sys.top;
                statusScrim.setLayoutParams(statusScrim.getLayoutParams());
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }
            if (navScrim != null) {
                navScrim.getLayoutParams().height = sys.bottom;
                navScrim.setLayoutParams(navScrim.getLayoutParams());
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }
            v.setPadding(sys.left, 0, sys.right, 0);
            return insets;
        });

        bindViews();
        wireToggles();
        wireInvestigationNotes();
        wireAddMedicine();
        wireMediaButtons();
        wireSave();

        // Disable autofill/suggestions
        hardDisableSuggestionsAndAutofill(root);
        setupBackPressToHideKeyboard();

        // Net
        requestQueue  = Volley.newRequestQueue(this);
        appointmentId = getIntent().getStringExtra("appointment_id");
        Log.d(TAG, "onCreate → intent appointment_id = " + appointmentId);
        if (!TextUtils.isEmpty(appointmentId)) {
            fetchAppointmentDetails(appointmentId.trim());
        } else {
            Log.w(TAG, "onCreate → No appointment_id in Intent");
        }
    }

    @Override
    protected void onDestroy() {
        loaderutil.hideLoader();
        super.onDestroy();
    }

    private void bindViews() {
        radioGroupUploadType = findViewById(R.id.radioGroupUploadType);
        radioVirtualReport = findViewById(R.id.radioVirtualReport);
        radioDirectUpload  = findViewById(R.id.radioDirectUpload);
        virtualForm        = findViewById(R.id.virtual_report_form);
        directUploadSection= findViewById(R.id.direct_upload_section);
        ivReportImage      = findViewById(R.id.ivReportImage);
        btnCaptureImage    = findViewById(R.id.btnCaptureImage);
        btnUploadImage     = findViewById(R.id.btnUploadImage);
        btnAddMedicine     = findViewById(R.id.btnAddMedicine);
        btnSave            = findViewById(R.id.btnSaveReport);
        medsContainer      = findViewById(R.id.medications_container);

        etAnimalName = findViewById(R.id.etAnimalName);
        etSpeciesBreed = findViewById(R.id.etSpeciesBreed);
        etSex = findViewById(R.id.etSex);
        etAge = findViewById(R.id.etAge);
        etWeight = findViewById(R.id.etWeight);
        etAddress = findViewById(R.id.etAddress);
        etDate = findViewById(R.id.etDate);

        etTemperature = findViewById(R.id.etTemperature);
        etPulse = findViewById(R.id.etPulse);
        etSpo2 = findViewById(R.id.etSpo2);
        etBloodPressure = findViewById(R.id.etBloodPressure);

        etRespiratoryRateBpm = findViewById(R.id.etRespiratoryRateBpm);
        etPainScore = findViewById(R.id.etPainScore);
        etHydrationStatus = findViewById(R.id.etHydrationStatus);
        etMucousMembranes = findViewById(R.id.etMucousMembranes);
        etCrtSec = findViewById(R.id.etCrtSec);
        etBehaviorGait = findViewById(R.id.etBehaviorGait);
        etSkinCoat = findViewById(R.id.etSkinCoat);

        etSymptoms = findViewById(R.id.etSymptoms);
        etRespiratorySystem = findViewById(R.id.etRespiratorySystem);
        etReasons = findViewById(R.id.etReasons);

        cbInvestigation = findViewById(R.id.cbInvestigation);
        tilInvestigationNotes = findViewById(R.id.tilInvestigationNotes);
        etInvestigationNotes = findViewById(R.id.etInvestigationNotes);

        etSignature = findViewById(R.id.etSignature);
        etReportType = findViewById(R.id.etReportType);

        disableSuggestionsForAllInputs();
        setupDecimalField(etWeight, 2);
    }

    private void wireToggles() {
        radioGroupUploadType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isVirtual = (checkedId == R.id.radioVirtualReport);
            virtualForm.setVisibility(isVirtual ? View.VISIBLE : View.GONE);
            directUploadSection.setVisibility(isVirtual ? View.GONE : View.VISIBLE);
        });
        radioVirtualReport.setChecked(true);
    }

    private void wireInvestigationNotes() {
        cbInvestigation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilInvestigationNotes.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void wireAddMedicine() {
        btnAddMedicine.setOnClickListener(v -> addMedicineRow(null, null));
    }

    private void wireMediaButtons() {
        btnCaptureImage.setOnClickListener(v -> ensureCameraThenOpen());
        btnUploadImage.setOnClickListener(v -> ensureGalleryThenOpen());
    }

    private void wireSave() {
        btnSave.setOnClickListener(v -> {
            try {
                final boolean isPhotoMode = (radioGroupUploadType.getCheckedRadioButtonId() == R.id.radioDirectUpload);

                if (isPhotoMode) {
                    // Photo required; skip virtual JSON completely
                    if (cameraImageUri == null) {
                        toast("Please capture or select a report photo.");
                        return;
                    }
                    uploadReportPhoto(cameraImageUri);
                } else {
                    // Virtual JSON path
                    JSONObject payload = buildVirtualReportJson();
                    Log.d(TAG, "saveVirtualReport → JSON payload: " + payload);
                    String url = ApiConfig.endpoint("Doctors/insert_vet_report.php");
                    Log.d(TAG, "saveVirtualReport → URL=" + url);

                    loaderutil.showLoader(this);
                    JsonObjectRequest post = new JsonObjectRequest(
                            Request.Method.POST,
                            url,
                            payload,
                            res -> {
                                loaderutil.hideLoader();
                                Log.d(TAG, "saveVirtualReport → RAW RESPONSE: " + res);
                                boolean ok = res.optBoolean("success", false);
                                if (ok) {
                                    int reportId = res.optInt("report_id", -1);
                                    toast("Report saved (ID: " + reportId + ")");
                                } else {
                                    toast("Save failed: " + res.optString("message", "unknown"));
                                }
                            },
                            err -> {
                                loaderutil.hideLoader();
                                logVolleyErrorDetailed(err);
                                toast("Save failed");
                            }
                    );
                    requestQueue.add(post);
                }

            } catch (Exception e) {
                Log.e(TAG, "saveVirtualReport → error", e);
                toast("Invalid data");
            }
        });
    }

    // ======================== PHOTO MODE UPLOAD (MULTIPART) ========================
    private void uploadReportPhoto(@NonNull Uri imageUri) {
        try {
            loaderutil.showLoader(this);

            // 1) Decode, scale, fix orientation, compress
            @SuppressLint({"NewApi", "LocalSuppress"}) byte[] jpegData = prepareHighQualityJpeg(imageUri, JPEG_QUALITY);

            if (jpegData == null || jpegData.length == 0) {
                loaderutil.hideLoader();
                toast("Image processing failed");
                return;
            }

            String fileName = getDisplayName(imageUri);
            if (TextUtils.isEmpty(fileName)) {
                fileName = "report_" + System.currentTimeMillis() + ".jpg";
            }

            String url = ApiConfig.endpoint("Doctors/insert_vet_report.php");

            Log.d(TAG, "uploadReportPhoto → URL=" + url + ", size=" + jpegData.length + " bytes, name=" + fileName);

            String finalFileName = fileName;
            VolleyMultipartRequest req = new VolleyMultipartRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        loaderutil.hideLoader();
                        try {
                            String body = new String(response.data, StandardCharsets.UTF_8);
                            Log.d(TAG, "uploadReportPhoto → RAW RESPONSE: " + body);
                            JSONObject obj = new JSONObject(body);
                            boolean ok = obj.optBoolean("success", false);
                            if (ok) {
                                int reportId = obj.optInt("report_id", -1);
                                toast("Photo report uploaded (ID: " + reportId + ")");
                            } else {
                                toast("Upload failed: " + obj.optString("message", "unknown"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "uploadReportPhoto → parse error", e);
                            toast("Upload parse error");
                        }
                    },
                    err -> {
                        loaderutil.hideLoader();
                        logVolleyErrorDetailed(err);
                        toast("Upload failed");
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("appointment_id", safeStr(appointmentId));

                    // Basic fields required by PHP even in photo mode
                    params.put("animal_name", safe(etAnimalName));
                    params.put("species_breed", safe(etSpeciesBreed));
                    params.put("sex", safe(etSex));
                    params.put("age_years", safe(etAge));

                    String weightClean = normalizeDecimal(safe(etWeight), 2);
                    if (!TextUtils.isEmpty(weightClean)) params.put("weight_kg", weightClean);

                    params.put("owner_address", safe(etAddress));
                    params.put("date", safe(etDate));
                    params.put("signature", safe(etSignature));

                    // Force photo mode labeling on server
                    // If server requires symptoms, send it (or a placeholder for photo-only)
                    String sy = safe(etSymptoms);
                    if (TextUtils.isEmpty(sy)) sy = "Photo-only report";
                    params.put("symptoms", sy);


                    // OPTIONAL: include if you have it in memory/intent
                    // params.put("animal_category_id", someAnimalCategoryId);

                    return params;
                }


                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> data = new HashMap<>();
                    data.put("report_image", new DataPart(finalFileName, jpegData, "image/jpeg"));
                    return data;
                }
            };

            // Photo may be big → increase timeout
            req.setRetryPolicy(new DefaultRetryPolicy(
                    30_000, 1, 1.0f
            ));

            requestQueue.add(req);

        } catch (Exception e) {
            loaderutil.hideLoader();
            Log.e(TAG, "uploadReportPhoto → exception", e);
            toast("Upload error");
        }
    }

    // Decode + fix orientation + scale + compress
    @RequiresApi(api = Build.VERSION_CODES.N)
    private byte[] prepareHighQualityJpeg(@NonNull Uri uri, int quality) {
        try {
            Bitmap src = decodeBitmapRespectingBounds(uri, AnimalVirtualReportActivity.MAX_IMAGE_SIDE_PX);
            if (src == null) return null;

            int rotation = readExifRotation(uri);
            if (rotation != 0) {
                Matrix m = new Matrix();
                m.postRotate(rotation);
                src = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
            }

            // Ensure we still cap to maxSide after rotation (usually already ok)
            int w = src.getWidth(), h = src.getHeight();
            float scale = 1f;
            int maxWH = Math.max(w, h);
            if (maxWH > AnimalVirtualReportActivity.MAX_IMAGE_SIDE_PX) scale = (float) AnimalVirtualReportActivity.MAX_IMAGE_SIDE_PX / (float) maxWH;
            if (scale < 1f) {
                int nw = Math.round(w * scale);
                int nh = Math.round(h * scale);
                src = Bitmap.createScaledBitmap(src, nw, nh, true);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            src.compress(Bitmap.CompressFormat.JPEG, quality, out);
            return out.toByteArray();

        } catch (Exception e) {
            Log.e(TAG, "prepareHighQualityJpeg → error", e);
            return null;
        }
    }

    // Efficient decode with bounds to avoid OOM; then scale close to target
    private Bitmap decodeBitmapRespectingBounds(@NonNull Uri uri, int maxSide) {
        try {
            ContentResolver cr = getContentResolver();
            // First decode bounds
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream is = cr.openInputStream(uri)) {
                android.graphics.BitmapFactory.decodeStream(is, null, opts);
            }
            int srcW = opts.outWidth;
            int srcH = opts.outHeight;
            if (srcW <= 0 || srcH <= 0) return null;

            int maxWH = Math.max(srcW, srcH);
            int inSample = 1;
            while (maxWH / inSample > maxSide * 1.5f) { // decode a bit larger; we’ll rescale precisely later
                inSample *= 2;
            }

            android.graphics.BitmapFactory.Options opts2 = new android.graphics.BitmapFactory.Options();
            opts2.inSampleSize = Math.max(1, inSample);
            opts2.inPreferredConfig = Bitmap.Config.ARGB_8888;

            try (InputStream is2 = cr.openInputStream(uri)) {
                Bitmap rough = android.graphics.BitmapFactory.decodeStream(is2, null, opts2);
                if (rough == null) return null;
                // Precise scale (down) if needed
                int w = rough.getWidth(), h = rough.getHeight();
                int max2 = Math.max(w, h);
                if (max2 > maxSide) {
                    float scale = (float) maxSide / (float) max2;
                    int nw = Math.round(w * scale);
                    int nh = Math.round(h * scale);
                    return Bitmap.createScaledBitmap(rough, nw, nh, true);
                }
                return rough;
            }
        } catch (Exception e) {
            Log.e(TAG, "decodeBitmapRespectingBounds → error", e);
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private int readExifRotation(@NonNull Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r")) {
                    if (pfd != null) {
                        ExifInterface exif = new ExifInterface(pfd.getFileDescriptor());
                        int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        return exifToDegrees(o);
                    }
                }
            }  else {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    ExifInterface exif = new ExifInterface(is);
                    int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    return exifToDegrees(o);
                }
            }
        }

    } catch (Exception e) {
            Log.w(TAG, "readExifRotation → cannot read EXIF, default orientation", e);
        }
        return 0;
    }

    private int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) return 90;
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) return 180;
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        return 0;
    }

    private String getDisplayName(@NonNull Uri uri) {
        String name = "";
        try (android.database.Cursor c = getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                name = c.getString(0);
            }
        } catch (Exception ignore) {}
        return name;
    }

    // ======================== VIRTUAL JSON (unchanged except weight) ========================
    private JSONObject buildVirtualReportJson() throws Exception {
        JSONObject root = new JSONObject();

        root.put("appointment_id", safeStr(appointmentId));
        root.put("animal_name", safe(etAnimalName));
        root.put("species_breed", safe(etSpeciesBreed));
        root.put("sex", safe(etSex));
        root.put("age_years", safe(etAge));

        String weightClean = normalizeDecimal(safe(etWeight), 2);
        if (TextUtils.isEmpty(weightClean)) {
            root.put("weight_kg", JSONObject.NULL);
            Log.d(TAG, "weight_kg → null (empty/invalid)");
        } else {
            root.put("weight_kg", weightClean);
            Log.d(TAG, "weight_kg → " + weightClean);
        }

        root.put("owner_address", safe(etAddress));
        root.put("date", safe(etDate));
        root.put("report_type", emptyTo(safe(etReportType)));
        root.put("signature", safe(etSignature));

        root.put("behavior_gait", safe(etBehaviorGait));
        root.put("skin_coat", safe(etSkinCoat));
        root.put("symptoms", safe(etSymptoms));
        root.put("respiratory_system", safe(etRespiratorySystem));
        root.put("reasons", safe(etReasons));

        JSONObject vitals = new JSONObject();
        vitals.put("temperature_c", safe(etTemperature));
        vitals.put("pulse_bpm", safe(etPulse));
        vitals.put("spo2_pct", safe(etSpo2));
        vitals.put("bp_mmhg", safe(etBloodPressure));
        vitals.put("respiratory_rate_bpm", safe(etRespiratoryRateBpm));
        vitals.put("pain_score_0_10", safe(etPainScore));
        vitals.put("hydration_status", safe(etHydrationStatus));
        vitals.put("mucous_membranes", safe(etMucousMembranes));
        vitals.put("crt_sec", safe(etCrtSec));
        root.put("vitals", vitals);

        JSONObject inv = new JSONObject();
        inv.put("requires_investigation", cbInvestigation.isChecked());
        inv.put("investigation_notes", safe(etInvestigationNotes));
        root.put("investigation", inv);

        JSONArray meds = new JSONArray();
        for (int i = 0; i < medsContainer.getChildCount(); i++) {
            View row = medsContainer.getChildAt(i);
            TextInputEditText nameEt = row.findViewById(R.id.etMedicine);
            TextInputEditText doseEt = row.findViewById(R.id.etDosage);

            String mName = safe(nameEt);
            String mDose = safe(doseEt);
            if (TextUtils.isEmpty(mName) && TextUtils.isEmpty(mDose)) continue;

            JSONObject item = new JSONObject();
            item.put("medicine_name", mName);
            item.put("dosage", mDose);
            meds.put(item);
        }
        root.put("medications", meds);

        try {
            StringBuilder namesCsv = new StringBuilder();
            StringBuilder dosesCsv = new StringBuilder();
            for (int i = 0; i < meds.length(); i++) {
                JSONObject o = meds.getJSONObject(i);
                if (i > 0) { namesCsv.append(", "); dosesCsv.append(", "); }
                namesCsv.append(o.optString("medicine_name", ""));
                dosesCsv.append(o.optString("dosage", ""));
            }
            Log.d(TAG, "medications(names CSV) → " + namesCsv);
            Log.d(TAG, "medications(doses  CSV) → " + dosesCsv);
        } catch (Exception ignore) {}

        return root;
    }

    // ======================== APPOINTMENT FETCH (unchanged) ========================
    private void fetchAppointmentDetails(String id) {
        String url = ApiConfig.endpoint("Doctors/get_appointment_details.php", "appointment_id", id);
        Log.d(TAG, "fetchAppointmentDetails → id=" + id);
        Log.d(TAG, "fetchAppointmentDetails → URL=" + url);

        loaderutil.showLoader(this);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                res -> {
                    try {
                        Log.d(TAG, "fetchAppointmentDetails → RAW JSON: " + res);
                        boolean ok = res.optBoolean("success", false);
                        if (!ok) {
                            String msg = res.optString("message", "unknown");
                            Log.w(TAG, "fetchAppointmentDetails → failure: " + msg);
                            toast("Appointment not found");
                            return;
                        }

                        String fullName     = res.optString("full_name", "");
                        String age          = res.optString("age", "");
                        String sex          = res.optString("sex", "");
                        String address      = res.optString("address", "");
                        String date         = res.optString("date", "");
                        String doctorName   = res.optString("doctor_name", "");
                        String speciesBreed = res.optString("species_breed", "");
                        int isVet           = res.optInt("is_vet_case", -1);

                        etAnimalName.setText(fullName);
                        etAge.setText(age);
                        etSex.setText(sex);
                        etAddress.setText(address);
                        etDate.setText(date);
                        etSignature.setText(doctorName);
                        if (!TextUtils.isEmpty(speciesBreed)) etSpeciesBreed.setText(speciesBreed);

                        Log.d(TAG, "→ mapped: name=" + fullName + ", species=" + speciesBreed);
                    } catch (Exception ex) {
                        Log.e(TAG, "fetchAppointmentDetails → parse error", ex);
                        toast("Parse error");
                    } finally {
                        loaderutil.hideLoader();
                    }
                },
                err -> {
                    try {
                        logVolleyErrorDetailed(err);
                    } finally {
                        loaderutil.hideLoader();
                    }
                }
        );
        requestQueue.add(req);
    }

    // ======================== Error logging (unchanged) ========================
    private void logVolleyErrorDetailed(VolleyError err) {
        String netMsg = (err.getMessage() == null) ? err.toString() : err.getMessage();
        Log.e(TAG, "Volley error: " + netMsg, err);

        if (err.networkResponse != null) {
            int code = err.networkResponse.statusCode;
            String body = "";
            try {
                body = new String(err.networkResponse.data, StandardCharsets.UTF_8);
            } catch (Exception ignore) { }
            Log.e(TAG, "→ HTTP " + code + " body: " + body);
        } else {
            if (err instanceof NoConnectionError) Log.e(TAG, "→ NoConnectionError");
            else if (err instanceof TimeoutError) Log.e(TAG, "→ TimeoutError");
            else if (err instanceof AuthFailureError) Log.e(TAG, "→ AuthFailureError");
            else if (err instanceof ServerError) Log.e(TAG, "→ ServerError");
            else if (err instanceof NetworkError) Log.e(TAG, "→ NetworkError");
            else if (err instanceof ParseError) Log.e(TAG, "→ ParseError");
        }
    }

    // ======================== Helpers (unchanged + decimals) ========================
    private String safe(TextInputEditText et) {
        return et == null || et.getText() == null ? "" : String.valueOf(et.getText()).trim();
    }
    private String safeStr(String v) { return v == null ? "" : v.trim(); }
    private String emptyTo(String v) { return TextUtils.isEmpty(v) ? "Animal Virtual Report" : v; }

    private void hardDisableSuggestionsAndAutofill(View root) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            root.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }
    }
    private void disableSuggestionsForAllInputs() {
        disableSuggestions(etAnimalName);
        disableSuggestions(etSpeciesBreed);
        disableSuggestions(etSex);
        disableSuggestions(etAddress);
        disableSuggestions(etDate);
        disableSuggestions(etHydrationStatus);
        disableSuggestions(etMucousMembranes);
        disableSuggestions(etBehaviorGait);
        disableSuggestions(etSkinCoat);
        disableSuggestions(etSymptoms);
        disableSuggestions(etRespiratorySystem);
        disableSuggestions(etReasons);
        disableSuggestions(etInvestigationNotes);
        disableSuggestions(etSignature);
        disableSuggestions(etReportType);
        disableSuggestions(etBloodPressure);

        forceNumericNoSuggestions(etAge, true);
        forceNumericNoSuggestions(etTemperature, true);
        forceNumericNoSuggestions(etPulse, false);
        forceNumericNoSuggestions(etSpo2, false);
        forceNumericNoSuggestions(etRespiratoryRateBpm, false);
        forceNumericNoSuggestions(etPainScore, false);
        forceNumericNoSuggestions(etCrtSec, true);
        // etWeight: specialized handler already applied
    }
    private void disableSuggestions(TextInputEditText et) {
        if (et == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            et.setAutofillHints((String[]) null);
            et.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        int cls = InputType.TYPE_CLASS_TEXT;
        et.setInputType(cls | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }
    private void forceNumericNoSuggestions(TextInputEditText et, boolean allowDecimal) {
        if (et == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            et.setAutofillHints((String[]) null);
            et.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        int type = InputType.TYPE_CLASS_NUMBER;
        if (allowDecimal) type |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
        et.setInputType(type);
        et.setRawInputType(type | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }
    private void setupDecimalField(TextInputEditText et, int maxDecimals) {
        if (et == null) return;
        et.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
        et.setFilters(new InputFilter[]{ new DecimalDigitsInputFilter(maxDecimals) });
        et.addTextChangedListener(new android.text.TextWatcher() {
            boolean editing;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable e) {
                if (editing) return;
                editing = true;
                String cleaned = quickSanitizeDecimal(e.toString(), maxDecimals);
                if (!cleaned.equals(e.toString())) e.replace(0, e.length(), cleaned);
                editing = false;
            }
        });
    }
    private static class DecimalDigitsInputFilter implements InputFilter {
        private final int maxDecimals;
        DecimalDigitsInputFilter(int maxDecimals) { this.maxDecimals = Math.max(0, maxDecimals); }
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            StringBuilder out = new StringBuilder(dest);
            out.replace(dstart, dend, source.subSequence(start, end).toString());
            String s = out.toString();
            int dots = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (!(Character.isDigit(c) || c == '.')) return "";
                if (c == '.') dots++;
                if (dots > 1) return "";
            }
            if (maxDecimals >= 0) {
                int dot = s.indexOf('.');
                if (dot >= 0) {
                    int decimals = s.length() - dot - 1;
                    if (decimals > maxDecimals) return "";
                }
            }
            return null;
        }
    }
    private String quickSanitizeDecimal(String raw, int maxDecimals) {
        if (TextUtils.isEmpty(raw)) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT)
                .replace(",", ".").replaceAll("[^0-9.]", "");
        int firstDot = s.indexOf('.');
        if (firstDot >= 0) {
            String before = s.substring(0, firstDot + 1);
            String after  = s.substring(firstDot + 1).replace(".", "");
            if (maxDecimals >= 0 && after.length() > maxDecimals) after = after.substring(0, maxDecimals);
            s = before + after;
        }
        s = s.replaceFirst("^0+(?!$|\\.)", "");
        return s;
    }
    private String normalizeDecimal(String raw, int maxDecimals) {
        if (TextUtils.isEmpty(raw)) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT).replace(",", ".").replaceAll("[^0-9.]", "");
        if (!s.matches(".*\\d.*")) return "";
        int firstDot = s.indexOf('.');
        if (firstDot >= 0) {
            String before = s.substring(0, firstDot + 1);
            String after  = s.substring(firstDot + 1).replace(".", "");
            if (maxDecimals >= 0 && after.length() > maxDecimals) after = after.substring(0, maxDecimals);
            s = before + after;
        }
        s = s.replaceFirst("^0+(?!$|\\.)", "");
        if (s.equals(".") || s.isEmpty()) return "";
        if (s.startsWith(".")) s = "0" + s;
        try { Double.parseDouble(s); return s; } catch (NumberFormatException e) { return ""; }
    }

    private void setupBackPressToHideKeyboard() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                View current = getCurrentFocus();
                if (current != null) {
                    hideKeyboard(current);
                    current.clearFocus();
                    return;
                }
                setEnabled(false);
                onBackPressed();
            }
        });
    }
    private void hideKeyboard(@NonNull View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
    private void addMedicineRow(String prefillName, String prefillDose) {
        View row = getLayoutInflater().inflate(R.layout._row_medicine_animal, medsContainer, false);
        TextInputEditText etName = row.findViewById(R.id.etMedicine);
        TextInputEditText etDose = row.findViewById(R.id.etDosage);
        ImageButton btnRemove = row.findViewById(R.id.btnRemoveRow);
        disableSuggestions(etName);
        disableSuggestions(etDose);
        if (prefillName != null) etName.setText(prefillName);
        if (prefillDose != null) etDose.setText(prefillDose);
        btnRemove.setOnClickListener(v -> medsContainer.removeView(row));
        medsContainer.addView(row);
    }

    private void ensureCameraThenOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        } else {
            openCamera();
        }
    }
    private void ensureGalleryThenOpen() {
        if (Build.VERSION.SDK_INT >= 33) {
            String[] perms = new String[]{ Manifest.permission.READ_MEDIA_IMAGES };
            if (ContextCompat.checkSelfPermission(this, perms[0]) != PackageManager.PERMISSION_GRANTED) {
                requestGalleryPermissions.launch(perms);
                return;
            }
        } else {
            String[] perms = new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE };
            if (ContextCompat.checkSelfPermission(this, perms[0]) != PackageManager.PERMISSION_GRANTED) {
                requestGalleryPermissions.launch(perms);
                return;
            }
        }
        openGallery();
    }
    private void openCamera() {
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, "animal_report_" + System.currentTimeMillis() + ".jpg");
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
            if (cameraImageUri == null) { toast("Error creating media Uri"); return; }
            takePicture.launch(cameraImageUri);
        } catch (Exception e) {
            toast("Camera error: " + e.getMessage());
        }
    }

    private void openGallery() {
        pickImage.launch("image/*");
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    // ======================== Multipart request (Volley) ========================
    public abstract static class VolleyMultipartRequest extends com.android.volley.Request<com.android.volley.NetworkResponse> {

        private final Response.Listener<com.android.volley.NetworkResponse> mListener;
        private final Response.ErrorListener mErrorListener;
        private final String boundary = "apiclient-" + System.currentTimeMillis();
        private final String twoHyphens = "--";
        private final String lineEnd = "\r\n";

        public VolleyMultipartRequest(int method, String url,
                                      Response.Listener<com.android.volley.NetworkResponse> listener,
                                      Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            mListener = listener;
            mErrorListener = errorListener;
        }

        @Override
        public String getBodyContentType() {
            return "multipart/form-data;boundary=" + boundary;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                // text params
                Map<String, String> params = getParams();
                if (params != null && params.size() > 0) {
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        writeTextPart(bos, entry.getKey(), entry.getValue());
                    }
                }
                // binary params
                Map<String, DataPart> data = getByteData();
                if (data != null && data.size() > 0) {
                    for (Map.Entry<String, DataPart> entry : data.entrySet()) {
                        writeFilePart(bos, entry.getKey(), entry.getValue());
                    }
                }
                // finish
                bos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
            } catch (Exception e) {
                Log.e(TAG, "VolleyMultipartRequest getBody error", e);
            }
            return bos.toByteArray();
        }

        private void writeTextPart(ByteArrayOutputStream bos, String name, String value) throws Exception {
            bos.write((twoHyphens + boundary + lineEnd).getBytes());
            bos.write(("Content-Disposition: form-data; name=\"" + name + "\"" + lineEnd).getBytes());
            bos.write(("Content-Type: text/plain; charset=UTF-8" + lineEnd + lineEnd).getBytes());
            bos.write((value != null ? value : "").getBytes(StandardCharsets.UTF_8));
            bos.write(lineEnd.getBytes());
        }

        private void writeFilePart(ByteArrayOutputStream bos, String name, DataPart data) throws Exception {
            bos.write((twoHyphens + boundary + lineEnd).getBytes());
            bos.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + data.fileName + "\"" + lineEnd).getBytes());
            bos.write(("Content-Type: " + data.type + lineEnd + lineEnd).getBytes());
            bos.write(data.content);
            bos.write(lineEnd.getBytes());
        }

        @Override
        protected Response<com.android.volley.NetworkResponse> parseNetworkResponse(com.android.volley.NetworkResponse response) {
            try {
                return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new ParseError(e));
            }
        }

        @Override
        protected void deliverResponse(com.android.volley.NetworkResponse response) {
            mListener.onResponse(response);
        }

        @Override
        public void deliverError(VolleyError error) {
            mErrorListener.onErrorResponse(error);
        }

        protected Map<String, String> getParams() throws AuthFailureError { return new HashMap<>(); }
        protected Map<String, DataPart> getByteData() { return new HashMap<>(); }

        public static class DataPart {
            public final String fileName;
            public final byte[] content;
            public final String type;
            public DataPart(String name, byte[] data, String type) {
                this.fileName = name;
                this.content = data;
                this.type = type;
            }
        }
    }
}
