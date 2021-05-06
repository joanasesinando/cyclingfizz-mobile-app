package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.Objects;

public class NewRouteActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@NewRoute";
    static final int PICK_IMAGES = 1;

    PathRecorder pathRecorder;

    TextInputLayout nameInputLayout;
    TextInputLayout descriptionInputLayout;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_route);

        pathRecorder = PathRecorder.getInstance();

        uiSetClickListeners();
        setInputs();
        setPOIs();
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

        // Set toolbar action btn click listener
        toolbar.setOnMenuItemClickListener(item -> {
            saveRoute();
            return false;
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
        saveBtn.setOnClickListener(v -> saveRoute());
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


    /*** -------------------------------------------- ***/
    /*** ------------------- POIs ------------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setPOIs() {
        LinearLayout linearLayout = findViewById(R.id.poi_list);

        for (PointOfInterest poi : pathRecorder.getAllPOIs()){
            LayoutInflater inflater = LayoutInflater.from(this);
            ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.poi_item, null, false);

            //Set title for POI
            TextView title = layout.findViewById(R.id.poi_item_title);
            title.setText(poi.getName());
            //Set description for POI
            TextView description = layout.findViewById(R.id.route_card_description);
            description.setText(poi.getDescription());
            //Add POI's images
            for (Bitmap bitmap: poi.getImages()) {
                addImageToGallery(bitmap, layout);
            }
            GridLayout gallery = layout.findViewById(R.id.poi_gallery);
            if (poi.getImages().size() > 0) gallery.setVisibility(View.VISIBLE);

            linearLayout.addView(layout);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void addImageToGallery(Bitmap bitmap, ConstraintLayout layout) {
        GridLayout gallery = layout.findViewById(R.id.poi_gallery);
        final float scale = getResources().getDisplayMetrics().density;

        // Create wrapper
        ConstraintLayout imgWrapper = new ConstraintLayout(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = (int) (100 * scale);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        imgWrapper.setLayoutParams(params);

        // Create image
        ImageView newImg = new ImageView(this);
        newImg.setImageBitmap(bitmap);
        newImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgWrapper.addView(newImg);

        // Create overlay (when selected)
        LinearLayout overlay = new LinearLayout(this);
        LinearLayout.LayoutParams overlayParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        overlay.setBackgroundColor(getColor(R.color.orange_500));
        overlay.setAlpha(0.4f);
        overlay.setVisibility(View.GONE);
        imgWrapper.addView(overlay, overlayParams);

        // Create checked icon (when selected)
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_round_check_circle_24);
        icon.setPadding((int) (10 * scale), (int) (10 * scale), (int) (10 * scale), (int) (10 * scale));
        icon.setColorFilter(getColor(R.color.white));
        icon.setVisibility(View.GONE);
        imgWrapper.addView(icon);

        gallery.addView(imgWrapper, params);
    }


    /*** -------------------------------------------- ***/
    /*** ------------------- ROUTE ------------------ ***/
    /*** -------------------------------------------- ***/

    public void saveRoute() {
        // Clean error messages
        nameInputLayout.setError(null);
        descriptionInputLayout.setError(null);

        // Get name & description
        String name = Objects.requireNonNull(nameInputLayout.getEditText()).getText().toString();
        String description = Objects.requireNonNull(descriptionInputLayout.getEditText()).getText().toString();

        // Check for errors
        boolean error = checkForErrors(name, description);

        if (!error) {
            LinearProgressIndicator progressIndicator = findViewById(R.id.progress_indicator);
            pathRecorder.saveRecording(name, description, result -> {
                progressIndicator.setVisibility(View.GONE);
                finish();
            });
            progressIndicator.setVisibility(View.VISIBLE);
        }
    }
}