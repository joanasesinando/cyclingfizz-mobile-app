package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.Objects;

public class NewRouteActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@NewRoute";
    static final int PICK_IMAGES = 1;

    PathRecorder pathRecorder;

    TextInputLayout nameInputLayout;
    TextInputLayout descriptionInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_route);

        pathRecorder = PathRecorder.getInstance();

        uiSetClickListeners();
        setInputs();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == PICK_IMAGES && resultCode == RESULT_OK && data != null) {
                Log.d(TAG, String.valueOf(data.getData()));
                // Get URI
                Uri uri = data.getData();

                // Update view
                ImageView thumbnail = findViewById(R.id.route_thumbnail);
                thumbnail.setImageURI(uri);

            } else {
                Toast.makeText(this, "No photo selected", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, Arrays.toString(e.getStackTrace()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @SuppressLint("IntentReset")
    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.new_route_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dangerous_action)
                    .setMessage(R.string.quit_new_route_warning)
                    .setNeutralButton(R.string.cancel, (dialog, which) -> {
                        // Respond to neutral button press
                    })
                    .setPositiveButton(R.string.quit, (dialog, which) -> {
                        // Respond to positive button press
                        finish();
                    })
                    .show();
        });

        // Set thumbnail btn click listener
        CardView thumbnail = findViewById(R.id.new_route_thumbnail);
        thumbnail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGES);
        });

        // Set save btn click listener
        MaterialButton saveBtn = findViewById(R.id.save_route);
        saveBtn.setOnClickListener(v -> {
            // Clean error messages
            nameInputLayout.setError(null);
            descriptionInputLayout.setError(null);

            // Get name & description
            String name = Objects.requireNonNull(nameInputLayout.getEditText()).getText().toString();
            String description = Objects.requireNonNull(descriptionInputLayout.getEditText()).getText().toString();

            // Check for errors
            boolean error = checkForErrors(name, description);

            if (!error) {
                pathRecorder.saveRecording(name, description);
                finish();
            }
        });
    }


    /*** -------------------------------------------- ***/
    /*** ------------------ INPUTS ------------------ ***/
    /*** -------------------------------------------- ***/

    private void setInputs() {
        nameInputLayout = findViewById(R.id.new_poi_name);
        descriptionInputLayout = findViewById(R.id.new_poi_description);

        Objects.requireNonNull(nameInputLayout.getEditText()).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                nameInputLayout.setError(null);
            }
        });

        Objects.requireNonNull(descriptionInputLayout.getEditText()).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                descriptionInputLayout.setError(null);
            }
        });
    }

    private boolean checkForErrors(String name, String description) {
        boolean error = false;

        // Check name
        if (name.isEmpty()) {
            nameInputLayout.setError(getString(R.string.name_required));
            error = true;
        }

        // Check description
        if (description.isEmpty()) {
            descriptionInputLayout.setError(getString(R.string.description_required));
            error = true;
        }

        return error;
    }
}