package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

public class AddPOIActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@AddPOI";
    static final int TAKE_PHOTO = 1;
    static final int PICK_IMAGES = 2;

    TextInputLayout nameInputLayout;
    TextInputLayout descriptionInputLayout;

    PathRecorder pathRecorder;
    Point coordPOI;

    boolean isDeletingImages = false;

    ArrayList<Bitmap> images = new ArrayList<>();
    ArrayList<Integer> imagesToDeleteIndexes = new ArrayList<>();

    /// -------------- PERMISSIONS -------------- ///

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, TAKE_PHOTO);

                }
            });

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_poi);

        pathRecorder = PathRecorder.getInstance();
        Location poiLocation = getIntent().getParcelableExtra(MapActivity.POI_LOCATION);
        coordPOI = Point.fromLngLat(poiLocation.getLongitude(), poiLocation.getLatitude());

        uiSetClickListeners();
        setInputs();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == PICK_IMAGES && resultCode == RESULT_OK && data != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    // Get URI
                    ClipData.Item item = data.getClipData().getItemAt(i);
                    Uri uri = item.getUri();

                    // Get bitmap
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    bitmap = fixImageRotation(uri, bitmap);
                    images.add(bitmap);

                    // Update view
                    addImageToGallery(bitmap);
                }
                GridLayout gallery = findViewById(R.id.new_poi_gallery);
                if (images.size() > 0) gallery.setVisibility(View.VISIBLE);

            } else if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK && data != null) {
                // Get bitmap
                Bundle extras = data.getExtras();
                Bitmap bitmap = (Bitmap) extras.get("data");
                images.add(bitmap);

                // Update view
                addImageToGallery(bitmap);
                GridLayout gallery = findViewById(R.id.new_poi_gallery);
                if (images.size() > 0) gallery.setVisibility(View.VISIBLE);

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("IntentReset")
    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.new_poi_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set take photo btn listener
        MaterialButton takePhotoBtn = findViewById(R.id.new_poi_take_photo);
        takePhotoBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, TAKE_PHOTO);

            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

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

        // Set selecting items top bar btns listeners
        MaterialToolbar selectItemsToolbar = findViewById(R.id.new_poi_select_items_toolbar).findViewById(R.id.topAppBar);
        selectItemsToolbar.setNavigationOnClickListener(v -> quitDeletingImages());
        selectItemsToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.delete_items) deleteImages();
            return false;
        });
    }

    private Bitmap fixImageRotation(Uri uri, Bitmap bitmap) {
        Bitmap rotatedBitmap = bitmap;
        String res = null;
        Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();

        try {
            ExifInterface ei = new ExifInterface(new File(res));

            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch(orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmap;
            }
            return rotatedBitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotatedBitmap;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void addImageToGallery(Bitmap bitmap) {
        GridLayout gallery = findViewById(R.id.new_poi_gallery);
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

        // Set click listeners
        imgWrapper.setOnLongClickListener(v -> {
            Log.d(TAG, "long click");
            if (!isDeletingImages) toggleToolbar();
            isDeletingImages = true;
            View overlayChild = ((ViewGroup) v).getChildAt(1);
            if (overlayChild.getVisibility() == View.VISIBLE) deselectImg(v);
            else if (overlayChild.getVisibility() == View.GONE) selectImg(v);
            return true;
        });

        imgWrapper.setOnClickListener(v -> {
            Log.d(TAG, "normal click");
            if (isDeletingImages) {
                View overlayChild = ((ViewGroup) v).getChildAt(1);

                if (overlayChild.getVisibility() == View.VISIBLE) deselectImg(v);
                else if (overlayChild.getVisibility() == View.GONE) selectImg(v);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void toggleToolbar() {
        View toolbarLayout = findViewById(R.id.new_poi_toolbar);
        View selectItemsToolbarLayout = findViewById(R.id.new_poi_select_items_toolbar);

        if (toolbarLayout.getVisibility() == View.VISIBLE) {
            toolbarLayout.setVisibility(View.GONE);
            selectItemsToolbarLayout.setVisibility(View.VISIBLE);
            getWindow().setStatusBarColor(getColor(R.color.darker));

        } else {
            toolbarLayout.setVisibility(View.VISIBLE);
            selectItemsToolbarLayout.setVisibility(View.GONE);
            getWindow().setStatusBarColor(getColor(R.color.orange_700));
        }
    }

    private void selectImg(View view) {
        // Add index to delete
        GridLayout gallery = findViewById(R.id.new_poi_gallery);
        imagesToDeleteIndexes.add(gallery.indexOfChild(view));
        Log.d(TAG, String.valueOf(gallery.indexOfChild(view)));

        // Update toolbar
        View toolbarLayout = findViewById(R.id.new_poi_select_items_toolbar);
        MaterialToolbar toolbar = toolbarLayout.findViewById(R.id.topAppBar);
        toolbar.setTitle(imagesToDeleteIndexes.size() + " selected");

        // Show overlay and check
        for (int i = 1; i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);
            child.setVisibility(View.VISIBLE);
        }
    }

    private void deselectImg(View view) {
        // Remove index to delete
        GridLayout gallery = findViewById(R.id.new_poi_gallery);
        imagesToDeleteIndexes.remove(gallery.indexOfChild(view));
        Log.d(TAG, String.valueOf(gallery.indexOfChild(view)));

        // Update toolbar
        View toolbar = findViewById(R.id.new_poi_select_items_toolbar);
        MaterialToolbar topBar = toolbar.findViewById(R.id.topAppBar);
        topBar.setTitle(imagesToDeleteIndexes.size() + " selected");

        // Hide overlay and check
        for (int i = 1; i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);
            child.setVisibility(View.GONE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void quitDeletingImages() {
        isDeletingImages = false;
        imagesToDeleteIndexes.clear();
        toggleToolbar();

        // Hide overlay and checks
        GridLayout gallery = findViewById(R.id.new_poi_gallery);
        for (int i = 0; i < gallery.getChildCount(); i++) {
            View imgWrapper = gallery.getChildAt(i);
            for (int j = 1; j < ((ViewGroup) imgWrapper).getChildCount(); j++) {
                View child = ((ViewGroup) imgWrapper).getChildAt(j);
                child.setVisibility(View.GONE);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void deleteImages() {
        GridLayout gallery = findViewById(R.id.new_poi_gallery);
        Collections.sort(imagesToDeleteIndexes, Collections.reverseOrder());

        for (int index : imagesToDeleteIndexes) {
            images.remove(index);
            gallery.removeViewAt(index);
        }

        isDeletingImages = false;
        imagesToDeleteIndexes.clear();
        toggleToolbar();
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
            pathRecorder.addPOI(images, name, description, coordPOI);
            finish();
        }
    }
}