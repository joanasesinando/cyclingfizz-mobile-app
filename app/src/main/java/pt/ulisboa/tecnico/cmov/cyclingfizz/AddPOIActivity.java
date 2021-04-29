package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.mapbox.geojson.Point;

import java.util.Arrays;
import java.util.Objects;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class AddPOIActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@AddPOI";
    final String DEFAULT_MEDIA_LINK = "https://storage.googleapis.com/cycling-fizz-pt.appspot.com/crane.jpg"; //FIXME: change
    static final int PICK_IMAGES = 1;

    TextInputLayout nameInputLayout;
    TextInputLayout descriptionInputLayout;

    PathRecorder pathRecorder;
    Point coordPOI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_poi);

        pathRecorder = PathRecorder.getInstance();
        Location poiLocation = (Location) getIntent().getParcelableExtra(MapActivity.POI_LOCATION);
        coordPOI = Point.fromLngLat(poiLocation.getLongitude(), poiLocation.getLatitude());

        uiSetClickListeners();
        setInputs();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == PICK_IMAGES && resultCode == RESULT_OK && data != null) {
                GridLayout gallery = findViewById(R.id.new_poi_gallery);
                final float scale = getResources().getDisplayMetrics().density;


                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    // Get URI
                    ClipData.Item item = data.getClipData().getItemAt(i);
                    Uri uri = item.getUri();

                    // Update view
                    ImageView newImg = new ImageView(this);
                    newImg.setImageURI(uri);
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = 0                                                                                                    ;
                    params.height = (int) (100 * scale);

                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                    params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                    newImg.setLayoutParams(params);

                    newImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    gallery.addView(newImg, params);
                }

            } else {
                Toast.makeText(this, "No photos selected", Toast.LENGTH_LONG).show();
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
        MaterialToolbar toolbar = findViewById(R.id.new_poi_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set pick photos btn listener
        MaterialButton pickPhotosBtn = findViewById(R.id.new_poi_pick_photos);
        pickPhotosBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*"); //FIXME: add video support
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, PICK_IMAGES);
        });

        // Set save btn click listener
        MaterialButton saveBtn = findViewById(R.id.save_poi);
        saveBtn.setOnClickListener(v -> savePOI());
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
    /*** ------------ POINT OF INTEREST ------------- ***/
    /*** -------------------------------------------- ***/

    private void savePOI() {
        // Clean error messages
        nameInputLayout.setError(null);
        descriptionInputLayout.setError(null);

        // Get name & description
        String name = Objects.requireNonNull(nameInputLayout.getEditText()).getText().toString();
        String description = Objects.requireNonNull(descriptionInputLayout.getEditText()).getText().toString();

        // Check for errors
        boolean error = checkForErrors(name, description);

        if (!error) {
            pathRecorder.addPOI(DEFAULT_MEDIA_LINK, name, description, coordPOI);
            finish();
        }
    }
}