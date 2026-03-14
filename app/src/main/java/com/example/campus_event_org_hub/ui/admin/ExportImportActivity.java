package com.example.campus_event_org_hub.ui.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.SyncManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ExportImportActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextView tvStatus;

    // SAF launcher for picking a CSV file to import
    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) doImport(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_import);

        db = new DatabaseHelper(this);

        findViewById(R.id.btn_back_export).setOnClickListener(v -> finish());

        tvStatus = findViewById(R.id.tv_export_status);

        Button btnExportFull    = findViewById(R.id.btn_export_full_db);
        Button btnExportStudents = findViewById(R.id.btn_export_students);
        Button btnImport         = findViewById(R.id.btn_import_db);

        btnExportFull.setOnClickListener(v -> exportFullDatabase());
        btnExportStudents.setOnClickListener(v -> exportRegisteredStudents());
        btnImport.setOnClickListener(v -> showImportWarning());
    }

    // ── Export full database ──────────────────────────────────────────────────

    private void exportFullDatabase() {
        setStatus("Exporting full database...");
        Executors.newSingleThreadExecutor().execute(() -> {
            String csv = db.exportDatabaseCsv();
            if (csv == null) {
                runOnUiThread(() -> setStatus("Export failed. Check logs."));
                return;
            }
            String filename = "ceoh_full_export_" + timestamp() + ".csv";
            File file = saveToDownloads(filename, csv);
            runOnUiThread(() -> {
                if (file != null) {
                    setStatus("Saved to Downloads:\n" + file.getAbsolutePath());
                    Toast.makeText(this, "Export saved to Downloads", Toast.LENGTH_LONG).show();
                    shareFile(file);
                } else {
                    setStatus("Failed to save file.");
                }
            });
        });
    }

    // ── Export registered students ────────────────────────────────────────────

    private void exportRegisteredStudents() {
        setStatus("Exporting registered students...");
        Executors.newSingleThreadExecutor().execute(() -> {
            String csv = db.exportRegisteredStudentsCsv();
            if (csv == null) {
                runOnUiThread(() -> setStatus("Export failed. Check logs."));
                return;
            }
            String filename = "ceoh_registered_students_" + timestamp() + ".csv";
            File file = saveToDownloads(filename, csv);
            runOnUiThread(() -> {
                if (file != null) {
                    setStatus("Saved to Downloads:\n" + file.getAbsolutePath());
                    Toast.makeText(this, "Students export saved to Downloads", Toast.LENGTH_LONG).show();
                    shareFile(file);
                } else {
                    setStatus("Failed to save file.");
                }
            });
        });
    }

    // ── Import database ───────────────────────────────────────────────────────

    private void showImportWarning() {
        new AlertDialog.Builder(this)
                .setTitle("Import Database")
                .setMessage("This will merge data from a previously exported CSV into the local database. "
                        + "Existing records with the same IDs will be overwritten.\n\n"
                        + "Pick a CSV file exported by this app.")
                .setPositiveButton("Pick File", (d, w) ->
                        importLauncher.launch(new String[]{"text/comma-separated-values",
                                "text/csv", "application/octet-stream", "*/*"}))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doImport(Uri uri) {
        setStatus("Reading file...");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) {
                    runOnUiThread(() -> setStatus("Could not open file."));
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                String csv = sb.toString();
                runOnUiThread(() -> setStatus("Importing data..."));
                int count = db.importDatabaseCsv(csv);

                // After import, also run a Firestore sync to pull remote data
                SyncManager.sync(this, () -> {
                    if (count >= 0) {
                        setStatus("Import complete. " + count + " rows processed.\nFirestore sync done.");
                        Toast.makeText(this, count + " rows imported", Toast.LENGTH_LONG).show();
                    } else {
                        setStatus("Import failed — invalid file format.");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> setStatus("Import error: " + e.getMessage()));
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private File saveToDownloads(String filename, String content) {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, filename);
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.close();
            return file;
        } catch (Exception e) {
            // Fallback: app-private files dir (always writable)
            try {
                File file = new File(getFilesDir(), filename);
                FileWriter fw = new FileWriter(file);
                fw.write(content);
                fw.close();
                return file;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private void shareFile(File file) {
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share CSV via"));
        } catch (Exception e) {
            // FileProvider not configured — file is still saved, just can't share directly
        }
    }

    private void setStatus(String msg) {
        if (tvStatus != null) tvStatus.setText(msg);
    }

    private String timestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }
}
