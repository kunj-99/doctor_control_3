package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class AnimalVirtualReportActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1001;
    private static final int REQUEST_GALLERY = 1002;

    private RadioGroup radioGroupUploadType;
    private RadioButton radioVirtualReport, radioDirectUpload;
    private LinearLayout virtualForm, directUploadSection;
    private ImageView ivReportImage;
    private android.widget.Button btnUploadImage, btnCaptureImage, btnSave, btnAddMedicine, btnUploadDirect;
    private LinearLayout medsContainer;

    private TextInputEditText etAnimalName, etSpeciesBreed, etSex, etAge, etWeight,
            etAddress, etDate, etTemperature, etPulse, etSpo2, etBloodPressure,
            etRespiratoryRateBpm, etPainScore, etHydrationStatus, etMucousMembranes,
            etCrtSec, etBehaviorGait, etSkinCoat, etSymptoms, etRespiratorySystem,
            etReasons, etInvestigationNotes, etSignature, etReportType;
    private CheckBox cbInvestigation;
    private TextInputLayout tilInvestigationNotes;

    private Uri selectedImageUri;
    private Bitmap selectedBitmap;
    private RequestQueue requestQueue;
    private String appointmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_virtual_report);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);

        final View root = findViewById(R.id.root_container_animal);
        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim = findViewById(R.id.navigation_bar_scrim);
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

        initializeViews();
        setupRadioGroup();

        appointmentId = getIntent() != null ? getIntent().getStringExtra("appointment_id") : null;
        requestQueue = Volley.newRequestQueue(this);

        if (!TextUtils.isEmpty(appointmentId)) {
            fetchAppointmentDetails(appointmentId.trim());
        }
    }

    private void initializeViews() {
        radioGroupUploadType = findViewById(R.id.radioGroupUploadType);
        radioVirtualReport = findViewById(R.id.radioVirtualReport);
        radioDirectUpload = findViewById(R.id.radioDirectUpload);
        virtualForm = findViewById(R.id.virtual_report_form);
        directUploadSection = findViewById(R.id.direct_upload_section);
        ivReportImage = findViewById(R.id.ivReportImage);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnCaptureImage = findViewById(R.id.btnCaptureImage);
        btnSave = findViewById(R.id.btnSaveReport);
        btnAddMedicine = findViewById(R.id.btnAddMedicine);
        btnUploadDirect = findViewById(R.id.btnUploadDirect);
        medsContainer = findViewById(R.id.medications_container);

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

        // --- Corrected: Each button now does exactly one thing ---
        btnUploadImage.setOnClickListener(v -> openGallery());
        btnCaptureImage.setOnClickListener(v -> openCamera());
        ivReportImage.setOnClickListener(v -> openGallery()); // optional: remove if not needed
        btnUploadDirect.setOnClickListener(v -> uploadDirectImage());
        btnSave.setOnClickListener(v -> saveReport());
        btnAddMedicine.setOnClickListener(v -> addMedicineRow(null, null));
        if (cbInvestigation != null && tilInvestigationNotes != null) {
            cbInvestigation.setOnCheckedChangeListener((button, checked) -> {
                tilInvestigationNotes.setVisibility(checked ? View.VISIBLE : View.GONE);
                if (!checked && etInvestigationNotes != null) {
                    etInvestigationNotes.setText(null);
                }
            });
        }
    }

    private void uploadDirectImage() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select or capture an image.", Toast.LENGTH_SHORT).show();
            return;
        }
        saveReport();
    }

    private void setupRadioGroup() {
        radioGroupUploadType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioVirtualReport) {
                virtualForm.setVisibility(View.VISIBLE);
                directUploadSection.setVisibility(View.GONE);
            } else {
                virtualForm.setVisibility(View.GONE);
                directUploadSection.setVisibility(View.VISIBLE);
            }
        });
        radioVirtualReport.setChecked(true);
    }

    // --- POPUP REMOVED, each button does only its own job ---
    // private void showImagePickerDialog() {...}  <-- REMOVE/NOT USED

    private void openCamera() {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.DISPLAY_NAME, "animal_report_" + System.currentTimeMillis() + ".jpg");
        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        selectedImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && selectedImageUri != null) {
                previewImage(selectedImageUri);
            } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                previewImage(selectedImageUri);
            }
        }
    }

    private void previewImage(Uri uri) {
        try {
            selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            ivReportImage.setImageBitmap(selectedBitmap);
            ivReportImage.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveReport() {
        int selectedRadioId = radioGroupUploadType.getCheckedRadioButtonId();
        if (selectedRadioId == -1) {
            Toast.makeText(this, "Please select an upload type.", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = ApiConfig.endpoint("Doctors/insert_vet_report.php");
        JSONObject postData = new JSONObject();

        try {
            postData.put("appointment_id", appointmentId);
            postData.put("animal_name", etAnimalName.getText().toString());
            postData.put("species_breed", etSpeciesBreed.getText().toString());
            postData.put("sex", etSex.getText().toString());
            postData.put("age_years", etAge.getText().toString());
            postData.put("weight_kg", etWeight.getText().toString());
            postData.put("owner_address", etAddress.getText().toString());
            postData.put("date", etDate.getText().toString());
            postData.put("report_type", etReportType.getText().toString());
            postData.put("signature", etSignature.getText().toString());

            JSONObject vitals = new JSONObject();
            vitals.put("temperature_c", etTemperature.getText().toString());
            vitals.put("pulse_bpm", etPulse.getText().toString());
            vitals.put("spo2_pct", etSpo2.getText().toString());
            vitals.put("bp_mmhg", etBloodPressure.getText().toString());
            vitals.put("respiratory_rate_bpm", etRespiratoryRateBpm.getText().toString());
            vitals.put("pain_score_0_10", etPainScore.getText().toString());
            vitals.put("hydration_status", etHydrationStatus.getText().toString());
            vitals.put("mucous_membranes", etMucousMembranes.getText().toString());
            vitals.put("crt_sec", etCrtSec.getText().toString());
            postData.put("vitals", vitals);

            postData.put("behavior_gait", etBehaviorGait.getText().toString());
            postData.put("skin_coat", etSkinCoat.getText().toString());
            postData.put("symptoms", etSymptoms.getText().toString());
            postData.put("respiratory_system", etRespiratorySystem.getText().toString());
            postData.put("reasons", etReasons.getText().toString());

            JSONObject inv = new JSONObject();
            inv.put("requires_investigation", cbInvestigation != null && cbInvestigation.isChecked());
            inv.put("investigation_notes", etInvestigationNotes.getText().toString());
            postData.put("investigation", inv);

            JSONArray meds = new JSONArray();
            for (int i = 0; i < medsContainer.getChildCount(); i++) {
                View row = medsContainer.getChildAt(i);
                TextInputEditText nameEt = row.findViewById(R.id.etMedicine);
                TextInputEditText doseEt = row.findViewById(R.id.etDosage);
                String mName = nameEt != null && nameEt.getText() != null ? nameEt.getText().toString().trim() : "";
                String mDose = doseEt != null && doseEt.getText() != null ? doseEt.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(mName) || !TextUtils.isEmpty(mDose)) {
                    JSONObject item = new JSONObject();
                    item.put("medicine_name", mName);
                    item.put("dosage", mDose);
                    meds.put(item);
                }
            }
            postData.put("medications", meds);

            if (selectedRadioId == R.id.radioDirectUpload) {
                if (selectedBitmap == null) {
                    Toast.makeText(this, "Please select or capture an image.", Toast.LENGTH_SHORT).show();
                    return;
                }
                postData.put("attachment_url", getStringImage(selectedBitmap));
            } else {
                postData.put("attachment_url", "");
            }

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
                        setResult(RESULT_OK);
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

    private String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        return android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);
    }

    private void addMedicineRow(String prefillName, String prefillDose) {
        View row = getLayoutInflater().inflate(R.layout._row_medicine_animal, medsContainer, false);
        TextInputEditText etName = row.findViewById(R.id.etMedicine);
        TextInputEditText etDose = row.findViewById(R.id.etDosage);
        ImageButton btnRemove = row.findViewById(R.id.btnRemoveRow);
        if (prefillName != null) etName.setText(prefillName);
        if (prefillDose != null) etDose.setText(prefillDose);
        btnRemove.setOnClickListener(v -> medsContainer.removeView(row));
        medsContainer.addView(row);
    }

    private void fetchAppointmentDetails(String id) {
        String url = ApiConfig.endpoint("Doctors/get_appointment_details.php", "appointment_id", id);
        loaderutil.showLoader(this);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                res -> {
                    loaderutil.hideLoader();
                    try {
                        if (!res.optBoolean("success", false)) {
                            Toast.makeText(this, "Appointment not found", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        etAnimalName.setText(res.optString("full_name", ""));
                        etAge.setText(res.optString("age", ""));
                        etSex.setText(res.optString("sex", ""));
                        etAddress.setText(res.optString("address", ""));
                        etDate.setText(res.optString("date", ""));
                        etSignature.setText(res.optString("doctor_name", ""));
                        String speciesBreed = res.optString("species_breed", "");
                        if (!TextUtils.isEmpty(speciesBreed)) etSpeciesBreed.setText(speciesBreed);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Failed to fetch appointment details", Toast.LENGTH_SHORT).show();
                }
        );
        requestQueue.add(req);
    }
}
