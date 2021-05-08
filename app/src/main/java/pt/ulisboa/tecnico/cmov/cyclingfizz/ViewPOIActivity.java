package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.mapbox.geojson.Point;


public class ViewPOIActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@ViewPOI";

    PointOfInterest poi;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poi);

        poi = (PointOfInterest) getIntent().getSerializableExtra(MapPreviewActivity.POI);

        setUI();
        uiSetClickListeners();
        setInputs();
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setUI() {
        // Set purple status bar
        getWindow().setStatusBarColor(getColor(R.color.purple_700));

        // Set toolbar title as POI name
        MaterialToolbar toolbar = findViewById(R.id.poi_toolbar).findViewById(R.id.topAppBar);
        toolbar.setTitle(poi.getName());

        // Hide btns
        MaterialButton takePhotosbtn = findViewById(R.id.poi_take_photo);
        takePhotosbtn.setVisibility(View.GONE);

        MaterialButton pickPhotosBtn = findViewById(R.id.poi_pick_photos);
        pickPhotosBtn.setVisibility(View.GONE);

        MaterialButton saveBtn = findViewById(R.id.save_poi);
        saveBtn.setVisibility(View.GONE);

        toolbar.getMenu().getItem(0).setVisible(false);

        // Set images
        LinearProgressIndicator progressIndicator = findViewById(R.id.progress_indicator);
        progressIndicator.setVisibility(View.VISIBLE);
        (new Thread(() -> {
            poi.downloadImages(ignored -> {
                runOnUiThread(() -> {
                    for (Bitmap bitmap : poi.getImages()) {
                        addImageToGallery(bitmap);
                    }
                    GridLayout gallery = findViewById(R.id.poi_gallery);
                    if (poi.getImages().size() > 0) gallery.setVisibility(View.VISIBLE);
                    progressIndicator.setVisibility(View.GONE);
                });
            });
        })).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("IntentReset")
    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.poi_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setInputs() {
        EditText nameInput = ((TextInputLayout) findViewById(R.id.poi_name)).getEditText();
        EditText descriptionInput = ((TextInputLayout) findViewById(R.id.poi_description)).getEditText();

        assert nameInput != null;
        nameInput.setInputType(InputType.TYPE_NULL);
        nameInput.setFocusable(false);

        assert descriptionInput != null;
        descriptionInput.setInputType(InputType.TYPE_NULL);
        descriptionInput.setFocusable(false);

        nameInput.setText(poi.getName());
        descriptionInput.setText(poi.getDescription());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void addImageToGallery(Bitmap bitmap) {
        GridLayout gallery = findViewById(R.id.poi_gallery);
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
        LinearLayout.LayoutParams newImgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        newImg.setLayoutParams(newImgParams);
        imgWrapper.addView(newImg);

        // Create overlay (when selected)
        LinearLayout overlay = new LinearLayout(this);
        LinearLayout.LayoutParams overlayParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        overlay.setBackgroundColor(getColor(R.color.purple_500));
        overlay.setAlpha(0.3f);
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
}