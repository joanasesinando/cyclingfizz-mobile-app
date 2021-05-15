package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class AddCommentActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@LeaveComment";
    static final int TAKE_PHOTO = 1;
    static final int PICK_IMAGES = 2;

    TextInputLayout msgInput;

    String routeID;
    PointOfInterest poi;

    boolean isDeletingImages = false;

    ArrayList<Bitmap> images = new ArrayList<>();
    ArrayList<Integer> imagesToDeleteIndexes = new ArrayList<>();

    String currentPhotoPath;

    /// -------------- PERMISSIONS -------------- ///

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "passou por aqui");
                    sendTakePhotoIntent();
                }
            });

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leave_comment);
        Utils.setStatusBarColor(this, getColor(R.color.purple_700));

        poi = ((SharedState) getApplicationContext()).viewingPOI;
        routeID = getIntent().getStringExtra(ViewPOIActivity.ROUTE_ID);

        uiSetClickListeners();
        setInput();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            GridLayout gallery = findViewById(R.id.leave_comment_gallery);
            if (requestCode == PICK_IMAGES && resultCode == RESULT_OK && data != null) {

                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    // Get URI
                    ClipData.Item item = data.getClipData().getItemAt(i);
                    Uri uri = item.getUri();

                    // Get bitmap
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    images.add(bitmap);

                    // Update view
                    updateImageView(bitmap, gallery, images.size() - 1 + i, findViewById(R.id.leave_comment_select_items_toolbar).findViewById(R.id.topAppBar));
                }
                if (images.size() > 0) gallery.setVisibility(View.VISIBLE);

            } else if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK && currentPhotoPath != null) {
                // Get bitmap
                File f = new File(currentPhotoPath);
                Uri uri = Uri.fromFile(f);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                images.add(bitmap);

                // Update view
                updateImageView(bitmap, gallery, images.size() - 1, findViewById(R.id.leave_comment_select_items_toolbar).findViewById(R.id.topAppBar));
                if (images.size() > 0) gallery.setVisibility(View.VISIBLE);

            } else {
                Log.d(TAG, "current photo path = " + currentPhotoPath);
                Toast.makeText(this, "No photos selected", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendTakePhotoIntent() {
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
                } else {
                    Log.e(TAG, "Entra no else, photoFile = null");
                }
                startActivityForResult(intent, TAKE_PHOTO);
            }

        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("IntentReset")
    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.leave_comment_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set toolbar action btn click listener
        toolbar.setOnMenuItemClickListener(item -> {
            addComment();
            return false;
        });

        // Set take photo btn listener
        MaterialButton takePhotoBtn = findViewById(R.id.leave_comment_take_photo);
        takePhotoBtn.setOnClickListener(v -> {
            sendTakePhotoIntent();
        });

        // Set pick photos btn listener
        MaterialButton pickPhotosBtn = findViewById(R.id.leave_comment_pick_photos);
        pickPhotosBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*"); //FIXME: add video support
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, PICK_IMAGES);
        });

        // Set done btn click listener
        MaterialButton doneBtn = findViewById(R.id.add_comment);
        doneBtn.setOnClickListener(v -> addComment());

        // Set selecting items top bar btns listeners
        MaterialToolbar selectItemsToolbar = findViewById(R.id.leave_comment_select_items_toolbar).findViewById(R.id.topAppBar);
        selectItemsToolbar.setNavigationOnClickListener(v -> quitDeletingImages(findViewById(R.id.leave_comment_gallery)));
        selectItemsToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.delete_items) deleteImages(findViewById(R.id.leave_comment_gallery));
            return false;
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void toggleToolbar() {
        View toolbarLayout = findViewById(R.id.leave_comment_toolbar);
        View selectItemsToolbarLayout = findViewById(R.id.leave_comment_select_items_toolbar);

        if (toolbarLayout.getVisibility() == View.VISIBLE) {
            toolbarLayout.setVisibility(View.GONE);
            selectItemsToolbarLayout.setVisibility(View.VISIBLE);
            getWindow().setStatusBarColor(getColor(R.color.darker));

        } else {
            toolbarLayout.setVisibility(View.VISIBLE);
            selectItemsToolbarLayout.setVisibility(View.GONE);
            getWindow().setStatusBarColor(getColor(R.color.purple_700));
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------------------ GALLERY ----------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updateImageView(Bitmap bitmap, GridLayout gallery, int index, MaterialToolbar toolbar) {
        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, Utils.THUMBNAIL_SIZE_MEDIUM, Utils.THUMBNAIL_SIZE_MEDIUM);
        ViewGroup imgWrapper = Utils.addImageToGallery(this, thumbImage, gallery, Utils.GALLERY_IMAGE_SIZE_MEDIUM, true, getColor(R.color.purple_500));

        // Set click listeners
        imgWrapper.setOnLongClickListener(v -> {
            if (!isDeletingImages) toggleToolbar();
            isDeletingImages = true;
            View overlayChild = ((ViewGroup) v).getChildAt(1);
            if (overlayChild.getVisibility() == View.VISIBLE) Utils.deselectImage(gallery, imagesToDeleteIndexes, v, toolbar);
            else if (overlayChild.getVisibility() == View.GONE) Utils.selectImage(gallery, imagesToDeleteIndexes, v, toolbar);
            return true;
        });

        imgWrapper.setOnClickListener(v -> {
            if (isDeletingImages) {
                View overlayChild = ((ViewGroup) v).getChildAt(1);
                if (overlayChild.getVisibility() == View.VISIBLE) Utils.deselectImage(gallery, imagesToDeleteIndexes, v, toolbar);
                else if (overlayChild.getVisibility() == View.GONE) Utils.selectImage(gallery, imagesToDeleteIndexes, v, toolbar);

            } else {
                ((SharedState) getApplicationContext()).slideshowImages = images;
                Intent intent = new Intent(this, SlideshowActivity.class);
                intent.putExtra("index", index);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void quitDeletingImages(GridLayout gallery) {
        isDeletingImages = false;
        imagesToDeleteIndexes.clear();
        toggleToolbar();
        Utils.quitDeletingImages(gallery);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void deleteImages(GridLayout gallery) {
        Utils.deleteImages(gallery, imagesToDeleteIndexes, images);
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
            Log.d(TAG, image.getAbsolutePath());
            currentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------------------- INPUT ------------------ ***/
    /*** -------------------------------------------- ***/

    private void setInput() {
        msgInput = findViewById(R.id.leave_comment_message_input);

        Objects.requireNonNull(msgInput.getEditText()).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                msgInput.setError(null);
            }
        });
    }

    private boolean checkForErrors(String msg) {
        boolean error = false;
        if (msg.isEmpty()) {
            msgInput.setError(getString(R.string.comment_required));
            error = true;
        }
        return error;
    }


    /*** -------------------------------------------- ***/
    /*** ----------------- COMMENT ------------------ ***/
    /*** -------------------------------------------- ***/

    private void addComment() {
        // Clean error message
        msgInput.setError(null);

        // Get message
        String message = Objects.requireNonNull(msgInput.getEditText()).getText().toString();

        // Check for errors
        boolean error = checkForErrors(message);

        if (!error) {
            LinearProgressIndicator progressIndicator = findViewById(R.id.progress_indicator2);
            progressIndicator.setVisibility(View.VISIBLE);
            poi.addComment(routeID, message, images, commentJson -> {
                progressIndicator.setVisibility(View.GONE);
                finish();
            });
        }
    }
}