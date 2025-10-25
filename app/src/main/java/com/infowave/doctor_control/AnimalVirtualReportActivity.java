package com.infowave.doctor_control;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

public class AnimalVirtualReportActivity extends AppCompatActivity {

    private static final String TAG = "AnimalReport";

    // UI
    private RadioGroup radioGroupUploadType;
    private RadioButton radioVirtualReport, radioDirectUpload;
    private LinearLayout virtualForm, directUploadSection;
    private ImageView ivReportImage;
    private Button btnCaptureImage, btnUploadImage, btnAddMedicine, btnSave;
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
    private Uri cameraImageUri = null;

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
                    ivReportImage.setVisibility(View.VISIBLE);
                    ivReportImage.setImageURI(cameraImageUri);
                } else {
                    toast("Camera capture cancelled");
                }
            });

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    ivReportImage.setVisibility(View.VISIBLE);
                    ivReportImage.setImageURI(uri);
                    cameraImageUri = uri; // reuse field
                }
            });

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_animal_virtual_report);

        // ===== Edge-to-edge: transparent system bars + black scrims =====
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        // White icons on black scrims (i.e., NOT light appearance)
        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);

        final View root = findViewById(R.id.root_container_animal);
        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim    = findViewById(R.id.navigation_bar_scrim);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Set scrim heights from current insets
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

            // Apply safe paddings on left/right only; top/bottom handled by scrims
            v.setPadding(sys.left, 0, sys.right, 0);
            return insets;
        });
        // ===== End of scrim setup =====

        bindViews();
        wireToggles();
        wireInvestigationNotes();
        wireAddMedicine();
        wireMediaButtons();
        wireSave();
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
    }

    private void wireToggles() {
        radioGroupUploadType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioVirtualReport) {
                virtualForm.setVisibility(View.VISIBLE);
                directUploadSection.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioDirectUpload) {
                virtualForm.setVisibility(View.GONE);
                directUploadSection.setVisibility(View.VISIBLE);
            }
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
            if (radioVirtualReport.isChecked()) {
                if (!validateVirtualReport()) return;
                try {
                    JSONObject payload = buildVirtualReportJson();
                    Log.d(TAG, "Virtual Report JSON: " + payload);
                    toast("Virtual report validated ✓");
                    // TODO: POST 'payload' to your API
                } catch (Exception e) {
                    toast("Error building JSON: " + e.getMessage());
                }
            } else {
                if (cameraImageUri == null) {
                    toast("Please capture or select an image");
                    return;
                }
                toast("Direct image ready ✓");
                // TODO: upload image at 'cameraImageUri'
            }
        });
    }

    // ===== Validation (unchanged from your logic style) =====
    private boolean validateVirtualReport() {
        if (isEmpty(etAnimalName)) return err(etAnimalName, "Required");
        if (isEmpty(etSpeciesBreed)) return err(etSpeciesBreed, "Required");
        if (isEmpty(etDate)) return err(etDate, "Required");
        if (isEmpty(etReasons)) return err(etReasons, "Required");

        if (!isEmpty(etPainScore)) {
            int ps = parseInt(etPainScore.getText().toString(), -1);
            if (ps < 0 || ps > 10) return err(etPainScore, "Pain score must be 0–10");
        }
        if (!isEmpty(etSpo2)) {
            int sp = parseInt(etSpo2.getText().toString(), -1);
            if (sp < 0 || sp > 100) return err(etSpo2, "SPO2 must be 0–100");
        }
        if (!isEmpty(etRespiratoryRateBpm)) {
            int rr = parseInt(etRespiratoryRateBpm.getText().toString(), -1);
            if (rr <= 0) return err(etRespiratoryRateBpm, "Enter a valid rate");
        }

        if (cbInvestigation.isChecked() && isEmpty(etInvestigationNotes)) {
            etInvestigationNotes.requestFocus();
            toast("Please add investigation notes");
            return false;
        }

        for (int i = 0; i < medsContainer.getChildCount(); i++) {
            View row = medsContainer.getChildAt(i);
            TextInputEditText etName = row.findViewById(R.id.etMedicine);
            TextInputEditText etDose = row.findViewById(R.id.etDosage);

            boolean nameEmpty = etName == null || TextUtils.isEmpty(etName.getText());
            boolean doseEmpty = etDose == null || TextUtils.isEmpty(etDose.getText());

            if (!nameEmpty && doseEmpty) return err(etDose, "Dosage required");
            if (nameEmpty && !doseEmpty) return err(etName, "Medicine name required");
        }

        return true;
    }

    private boolean err(@NonNull TextInputEditText et, @NonNull String msg) {
        et.setError(msg);
        et.requestFocus();
        return false;
    }

    private boolean isEmpty(TextInputEditText et) {
        return et == null || TextUtils.isEmpty(et.getText());
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private JSONObject buildVirtualReportJson() throws Exception {
        JSONObject root = new JSONObject();

        root.put("animal_name", s(etAnimalName));
        root.put("species_breed", s(etSpeciesBreed));
        root.put("sex", s(etSex));
        root.put("age_years", s(etAge));
        root.put("weight_kg", s(etWeight));
        root.put("owner_address", s(etAddress));
        root.put("date", s(etDate));

        JSONObject vitals = new JSONObject();
        vitals.put("temperature_c", s(etTemperature));
        vitals.put("pulse_bpm", s(etPulse));
        vitals.put("spo2_pct", s(etSpo2));
        vitals.put("bp_mmhg", s(etBloodPressure));
        vitals.put("respiratory_rate_bpm", s(etRespiratoryRateBpm));
        vitals.put("pain_score_0_10", s(etPainScore));
        vitals.put("hydration_status", s(etHydrationStatus));
        vitals.put("mucous_membranes", s(etMucousMembranes));
        vitals.put("crt_sec", s(etCrtSec));
        root.put("vitals", vitals);

        root.put("behavior_gait", s(etBehaviorGait));
        root.put("skin_coat", s(etSkinCoat));
        root.put("symptoms", s(etSymptoms));
        root.put("respiratory_system", s(etRespiratorySystem));
        root.put("reasons", s(etReasons));

        JSONObject inv = new JSONObject();
        inv.put("requires_investigation", cbInvestigation.isChecked());
        inv.put("investigation_notes", s(etInvestigationNotes));
        root.put("investigation", inv);

        root.put("signature", s(etSignature));
        root.put("report_type", s(etReportType));

        JSONArray meds = new JSONArray();
        for (int i = 0; i < medsContainer.getChildCount(); i++) {
            View row = medsContainer.getChildAt(i);
            TextInputEditText etName = row.findViewById(R.id.etMedicine);
            TextInputEditText etDose = row.findViewById(R.id.etDosage);
            String name = etName != null ? val(etName) : "";
            String dose = etDose != null ? val(etDose) : "";
            if (!name.isEmpty() && !dose.isEmpty()) {
                JSONObject m = new JSONObject();
                m.put("medicine_name", name);
                m.put("dosage", dose);
                meds.put(m);
            }
        }
        root.put("medications", meds);

        return root;
    }

    private String s(TextInputEditText et) { return et == null ? "" : val(et); }
    private String val(TextInputEditText et) { return String.valueOf(et.getText()).trim(); }

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
            if (cameraImageUri == null) {
                toast("Error creating media Uri");
                return;
            }
            takePicture.launch(cameraImageUri);
        } catch (Exception e) {
            toast("Camera error: " + e.getMessage());
        }
    }

    private void openGallery() {
        pickImage.launch("image/*");
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
