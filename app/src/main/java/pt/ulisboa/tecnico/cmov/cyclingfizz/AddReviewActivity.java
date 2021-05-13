package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class AddReviewActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@RouteActivity";
    static final int TAKE_PHOTO = 1;
    static final int PICK_IMAGES = 2;

    TextInputLayout msgInput;

    Route route;
    int rate = 0;

    boolean isDeletingImages = false;

    ArrayList<Bitmap> images = new ArrayList<>();
    ArrayList<Integer> imagesToDeleteIndexes = new ArrayList<>();

    String currentPhotoPath;

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
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leave_review);

        route = ((SharedState) getApplicationContext()).reviewingRoute;
        rate = getIntent().getIntExtra(RouteActivity.RATE, 0);

        uiSetClickListeners();
        uiSetRate();
        msgInput = findViewById(R.id.leave_review_message_input);
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
                    images.add(bitmap);

                    // Update view
                    addImageToGallery(bitmap);
                }
                GridLayout gallery = findViewById(R.id.leave_review_gallery);
                if (images.size() > 0) gallery.setVisibility(View.VISIBLE);

            } else if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK && currentPhotoPath != null) {
                // Get bitmap
                File f = new File(currentPhotoPath);
                Uri uri = Uri.fromFile(f);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                images.add(bitmap);

                // Update view
                addImageToGallery(bitmap);
                GridLayout gallery = findViewById(R.id.leave_review_gallery);
                if (images.size() > 0) gallery.setVisibility(View.VISIBLE);

            } else {
                Toast.makeText(this, "No photos selected", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint({"UseCompatLoadingForDrawables", "IntentReset"})
    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.leave_review_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set toolbar action btn click listener
        toolbar.setOnMenuItemClickListener(item -> {
            addReview();
            return false;
        });

        // Set stars click listeners
        for (int i = 1; i <= 5; i++) {
            ImageView star = findViewById(getResources().getIdentifier("rate_star" + i, "id", getPackageName()));
            int newRate = i;
            star.setOnClickListener(v -> {
                rate = newRate;
                uiSetRate();
            });
        }

        // Set take photo btn listener
        MaterialButton takePhotoBtn = findViewById(R.id.leave_review_take_photo);
        takePhotoBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {

                    File photoFile = createImageFile();

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(this,
                                "pt.ulisboa.tecnico.cmov.cyclingfizz.fileprovider",
                                photoFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    }
                    startActivityForResult(intent, TAKE_PHOTO);
                }

            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Set pick photos btn listener
        MaterialButton pickPhotosBtn = findViewById(R.id.leave_review_pick_photos);
        pickPhotosBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*"); //FIXME: add video support
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, PICK_IMAGES);
        });

        // Set done btn click listener
        MaterialButton doneBtn = findViewById(R.id.add_review);
        doneBtn.setOnClickListener(v -> addReview());

        // Set selecting items top bar btns listeners
        MaterialToolbar selectItemsToolbar = findViewById(R.id.leave_review_select_items_toolbar).findViewById(R.id.topAppBar);
        selectItemsToolbar.setNavigationOnClickListener(v -> quitDeletingImages());
        selectItemsToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.delete_items) deleteImages();
            return false;
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void uiSetRate() {
        for (int i = 1; i <= rate; i++) {
            ImageView star = findViewById(getResources().getIdentifier("rate_star" + i, "id", getPackageName()));
            star.setImageDrawable(getDrawable(R.drawable.ic_round_star_24));
            star.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.orange_500)));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void toggleToolbar() {
        View toolbarLayout = findViewById(R.id.leave_review_toolbar);
        View selectItemsToolbarLayout = findViewById(R.id.leave_review_select_items_toolbar);

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


    /*** -------------------------------------------- ***/
    /*** ------------------ GALLERY ----------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void addImageToGallery(Bitmap bitmap) {
        GridLayout gallery = findViewById(R.id.leave_review_gallery);
        final float scale = getResources().getDisplayMetrics().density;

        // Create wrapper
        ConstraintLayout imgWrapper = new ConstraintLayout(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = (int) (110 * scale);
        params.height = (int) (110 * scale);
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
        overlay.setBackgroundColor(getColor(R.color.orange_500));
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

        // Set click listeners
        imgWrapper.setOnLongClickListener(v -> {
            if (!isDeletingImages) toggleToolbar();
            isDeletingImages = true;
            View overlayChild = ((ViewGroup) v).getChildAt(1);
            if (overlayChild.getVisibility() == View.VISIBLE) deselectImg(v);
            else if (overlayChild.getVisibility() == View.GONE) selectImg(v);
            return true;
        });

        imgWrapper.setOnClickListener(v -> {
            if (isDeletingImages) {
                View overlayChild = ((ViewGroup) v).getChildAt(1);

                if (overlayChild.getVisibility() == View.VISIBLE) deselectImg(v);
                else if (overlayChild.getVisibility() == View.GONE) selectImg(v);
            }
        });
    }

    private void selectImg(View view) {
        // Add index to delete
        GridLayout gallery = findViewById(R.id.leave_review_gallery);
        imagesToDeleteIndexes.add(gallery.indexOfChild(view));

        // Update toolbar
        View toolbarLayout = findViewById(R.id.leave_review_select_items_toolbar);
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
        GridLayout gallery = findViewById(R.id.leave_review_gallery);
        imagesToDeleteIndexes.remove(gallery.indexOfChild(view));

        // Update toolbar
        View toolbar = findViewById(R.id.leave_review_select_items_toolbar);
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
        GridLayout gallery = findViewById(R.id.leave_review_gallery);
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
        GridLayout gallery = findViewById(R.id.leave_review_gallery);
        Collections.sort(imagesToDeleteIndexes, Collections.reverseOrder());

        for (int index : imagesToDeleteIndexes) {
            images.remove(index);
            gallery.removeViewAt(index);
        }

        isDeletingImages = false;
        imagesToDeleteIndexes.clear();
        toggleToolbar();
    }

    @SuppressLint("SimpleDateFormat")
    private File createImageFile() {
        try {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            currentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------------------- RATE ------------------- ***/
    /*** -------------------------------------------- ***/

    private void addReview() {
        // Get message
        String message = msgInput.getEditText() != null ? msgInput.getEditText().getText().toString() : null;

        if (rate != 0) {
            route.addReview(message, rate, images, reviewJson -> finish());
        }
    }
}