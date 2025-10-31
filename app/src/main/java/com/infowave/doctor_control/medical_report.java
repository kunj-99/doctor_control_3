package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class medical_report extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;

    // --- Keyboard-aware scroll container ---
    private android.widget.ScrollView formScroll;

    private android.widget.LinearLayout virtualReportForm, directUploadSection, medicationsContainer;
    private android.widget.RadioGroup radioGroupUploadType;
    private android.widget.ImageView ivReportImage;
    private MaterialButton btnUploadImage, btnSaveReport, btnAddMedicine;
    private Uri selectedImageUri;
    private Bitmap selectedBitmap;
    private int medicineCount = 1;

    private TextInputEditText etPatientName, etAge, etSex, etWeight, etAddress, etDate,
            etTemperature, etPulse, etSpo2, etBloodPressure, etSignature, etReportType;
    private TextInputEditText etSymptoms, etRespiratorySystem;

    // Investigation checkbox + notes
    private CheckBox cbInvestigation;
    private TextInputLayout tilInvestigationNotes;
    private TextInputEditText etInvestigationNotes;

    private RequestQueue requestQueue;
    private String appointmentId;

    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_report);

        // ===== Edge-to-edge with black scrims (status/nav) =====
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);

        final View root = findViewById(R.id.root_container_medical);
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
        // ===== End scrim setup =====

        initializeViews();
        setupKeyboardAwareScrolling(); // <— NEW
        setupRadioGroup();

        appointmentId = getIntent().getStringExtra("appointment_id");
        requestQueue  = Volley.newRequestQueue(this);

        if (appointmentId != null && !appointmentId.isEmpty()) {
            fetchAppointmentDetails(appointmentId);
        }
    }

    @SuppressLint("WrongViewCast")
    private void initializeViews() {
        // IMPORTANT: Ensure your XML ScrollView has id @+id/form_scroll
        formScroll          = findViewById(R.id.form_scroll);

        virtualReportForm    = findViewById(R.id.virtual_report_form);
        directUploadSection  = findViewById(R.id.direct_upload_section);
        medicationsContainer = findViewById(R.id.medications_container);
        radioGroupUploadType = findViewById(R.id.radioGroupUploadType);
        ivReportImage        = findViewById(R.id.ivReportImage);
        btnUploadImage       = findViewById(R.id.btnUploadImage);
        btnSaveReport        = findViewById(R.id.btnSaveReport);
        btnAddMedicine       = findViewById(R.id.btnAddMedicine);

        etSymptoms           = findViewById(R.id.etSymptoms);
        etRespiratorySystem  = findViewById(R.id.etRespiratorySystem);

        etPatientName   = findViewById(R.id.etPatientName);
        etAge           = findViewById(R.id.etAge);
        etSex           = findViewById(R.id.etSex);
        etWeight        = findViewById(R.id.etWeight);
        etAddress       = findViewById(R.id.etAddress);
        etDate          = findViewById(R.id.etDate);
        etTemperature   = findViewById(R.id.etTemperature);
        etPulse         = findViewById(R.id.etPulse);
        etSpo2          = findViewById(R.id.etSpo2);
        etBloodPressure = findViewById(R.id.etBloodPressure);
        etSignature     = findViewById(R.id.etSignature);
        etReportType    = findViewById(R.id.etReportType);

        // Investigation UI
        cbInvestigation       = findViewById(R.id.cbInvestigation);
        tilInvestigationNotes = findViewById(R.id.tilInvestigationNotes);
        etInvestigationNotes  = findViewById(R.id.etInvestigationNotes);

        if (cbInvestigation != null && tilInvestigationNotes != null) {
            cbInvestigation.setOnCheckedChangeListener((button, checked) -> {
                tilInvestigationNotes.setVisibility(checked ? View.VISIBLE : View.GONE);
                if (!checked && etInvestigationNotes != null) etInvestigationNotes.setText(null);
            });
        }

        btnUploadImage.setOnClickListener(v -> showImagePickerDialog());
        btnSaveReport .setOnClickListener(v -> saveReport());
        btnAddMedicine.setOnClickListener(v -> addMedicineField());

        ivReportImage.setOnClickListener(v -> showImagePickerDialog());
    }

    private void setupRadioGroup() {
        radioGroupUploadType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioVirtualReport) {
                virtualReportForm.setVisibility(View.VISIBLE);
                directUploadSection.setVisibility(View.GONE);
            } else {
                virtualReportForm.setVisibility(View.GONE);
                directUploadSection.setVisibility(View.VISIBLE);
            }
        });
        // Optional default:
        // ((android.widget.RadioButton)findViewById(R.id.radioVirtualReport)).setChecked(true);
    }

    // ---------------- Keyboard-aware scrolling ----------------
    private void setupKeyboardAwareScrolling() {
        if (formScroll == null) return;

        // Apply IME bottom padding to the ScrollView
        ViewCompat.setOnApplyWindowInsetsListener(formScroll, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), ime.bottom);
            return insets;
        });

        View[] fields = new View[]{
                etPatientName, etAge, etSex, etWeight, etAddress, etDate,
                etTemperature, etPulse, etSpo2, etBloodPressure,
                etSymptoms, etRespiratorySystem, etSignature, etReportType,
                etInvestigationNotes
        };

        for (View f : fields) {
            if (f == null) continue;
            f.setOnFocusChangeListener((fv, hasFocus) -> {
                if (hasFocus) formScroll.post(() -> scrollIntoView(formScroll, fv));
            });
            f.setOnClickListener(fv -> formScroll.post(() -> scrollIntoView(formScroll, fv)));
        }

        formScroll.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        View focused = getCurrentFocus();
                        if (focused != null) {
                            formScroll.post(() -> scrollIntoView(formScroll, focused));
                        }
                    }
                }
        );
    }

    private void scrollIntoView(android.widget.ScrollView sv, View child) {
        if (sv == null || child == null) return;
        Rect r = new Rect();
        child.getDrawingRect(r);
        sv.offsetDescendantRectToMyCoords(child, r);
        sv.smoothScrollTo(0, r.top);
    }

    private void fetchAppointmentDetails(String id) {
        String url = ApiConfig.endpoint("Doctors/get_appointment_details.php", "appointment_id", id);
        loaderutil.showLoader(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    loaderutil.hideLoader();
                    try {
                        if (response.optBoolean("success")) {
                            etPatientName.setText(response.optString("full_name", ""));
                            etAge.setText     (response.optString("age", ""));
                            etSex.setText     (response.optString("sex", ""));
                            etAddress.setText (response.optString("address", ""));
                            etDate.setText    (response.optString("date", ""));
                            etReportType.setText("");
                            etSignature.setText(response.optString("doctor_name", ""));
                        } else {
                            showError("Appointment not found.");
                        }

                        etWeight.setText("");
                        etTemperature.setText("");
                        etPulse.setText("");
                        etSpo2.setText("");
                        etBloodPressure.setText("");

                        if (cbInvestigation != null) cbInvestigation.setChecked(false);
                        if (tilInvestigationNotes != null) tilInvestigationNotes.setVisibility(View.GONE);

                    } catch (Exception e) {
                        showError("Error parsing appointment data.");
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    showError("Failed to fetch appointment details.");
                });

        requestQueue.add(request);
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Upload Image")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    private void openCamera() {
        // Use MediaStore insert → no FileProvider complexity needed
        android.content.ContentValues cv = new android.content.ContentValues();
        cv.put(MediaStore.Images.Media.DISPLAY_NAME, "report_" + System.currentTimeMillis() + ".jpg");
        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @SuppressLint("IntentReset")
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loaderutil.showLoader(this);
            try {
                if (requestCode == REQUEST_CAMERA && cameraImageUri != null) {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), cameraImageUri);
                } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
                    selectedImageUri = data.getData();
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                }
                if (selectedBitmap != null) {
                    ivReportImage.setImageBitmap(selectedBitmap);
                    ivReportImage.setVisibility(View.VISIBLE);
                } else {
                    showError("Failed to load image.");
                }
            } catch (IOException e) {
                showError("Failed to load image.");
            } finally {
                loaderutil.hideLoader();
            }
        }
    }

    private void addMedicineField() {
        medicineCount++;
        View medicineView = LayoutInflater.from(this)
                .inflate(R.layout.item_medicine_field, medicationsContainer, false);
        TextInputLayout medicineNameLayout = medicineView.findViewById(R.id.medicineNameLayout);
        TextInputLayout dosageLayout       = medicineView.findViewById(R.id.dosageLayout);
        medicineNameLayout.setHint("Medicine Name " + medicineCount);
        dosageLayout.setHint("Dosage " + medicineCount);
        medicationsContainer.addView(medicineView);
    }

    private void saveReport() {
        int selectedRadioId = radioGroupUploadType.getCheckedRadioButtonId();
        if (selectedRadioId == -1) {
            Toast.makeText(this, "Please select an upload type.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = ApiConfig.endpoint("Doctors/insert_medical_report.php");
        JSONObject postData = new JSONObject();

        try {
            postData.put("appointment_id", appointmentId);
            postData.put("patient_name", s(etPatientName));
            postData.put("age",          s(etAge));
            postData.put("sex",          s(etSex));
            postData.put("weight",       s(etWeight));
            postData.put("patient_address", s(etAddress));
            postData.put("visit_date",   s(etDate));
            postData.put("temperature",  s(etTemperature));
            postData.put("pulse",        s(etPulse));
            postData.put("spo2",         s(etSpo2));
            postData.put("blood_pressure", s(etBloodPressure));
            postData.put("report_type",  s(etReportType));
            postData.put("symptoms",     s(etSymptoms));
            postData.put("respiratory_system", s(etRespiratorySystem));
            postData.put("doctor_name",  s(etSignature));
            postData.put("doctor_signature", s(etSignature));

            String investigations = "";
            if (cbInvestigation != null && cbInvestigation.isChecked() && etInvestigationNotes != null) {
                investigations = s(etInvestigationNotes);
            }
            postData.put("investigations", investigations);

            if (selectedRadioId == R.id.radioDirectUpload) {
                if (selectedBitmap == null) {
                    Toast.makeText(this, "Please select or capture an image.", Toast.LENGTH_SHORT).show();
                    return;
                }
                postData.put("report_photo", getStringImage(selectedBitmap));
            } else {
                postData.put("report_photo", "");
            }

            JSONArray medicineArray = new JSONArray();
            int childCount = medicationsContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = medicationsContainer.getChildAt(i);
                TextInputLayout medicineNameLayout = child.findViewById(R.id.medicineNameLayout);
                TextInputLayout dosageLayout       = child.findViewById(R.id.dosageLayout);
                String medicineName = (medicineNameLayout.getEditText() != null)
                        ? medicineNameLayout.getEditText().getText().toString().trim() : "";
                String dosage = (dosageLayout.getEditText() != null)
                        ? dosageLayout.getEditText().getText().toString().trim() : "";
                if (!medicineName.isEmpty() && !dosage.isEmpty()) {
                    JSONObject medObject = new JSONObject();
                    medObject.put("medicine_name", medicineName);
                    medObject.put("dosage", dosage);
                    medicineArray.put(medObject);
                }
            }
            postData.put("medicines", medicineArray);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to collect form data.", Toast.LENGTH_SHORT).show();
            return;
        }

        loaderutil.showLoader(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, postData,
                response -> {
                    loaderutil.hideLoader();
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Report saved successfully.", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("report_submitted", true);
                        resultIntent.putExtra("appointment_id", appointmentId);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save report.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Error sending report to server.", Toast.LENGTH_SHORT).show();
                });
        requestQueue.add(request);
    }

    private static String s(TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }

    private String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
