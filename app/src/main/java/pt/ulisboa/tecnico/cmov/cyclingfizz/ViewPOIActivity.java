package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;


public class ViewPOIActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@ViewPOI";
    static String SERVER_URL = "https://stations.cfservertest.ga";

    PointOfInterest poi;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poi);

        poi = (PointOfInterest) getIntent().getSerializableExtra(MapPreviewActivity.POI);

        setUI();
        uiSetClickListeners();
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUI() {
        // Set purple status bar
        getWindow().setStatusBarColor(getColor(R.color.purple_700));

        // Set toolbar title as POI name
        MaterialToolbar toolbar = findViewById(R.id.poi_toolbar).findViewById(R.id.topAppBar);
        toolbar.setTitle(poi.getName());

        // Hide / change btns
        MaterialButton takePhotosbtn = findViewById(R.id.poi_take_photo);
        takePhotosbtn.setVisibility(View.GONE);

        MaterialButton pickPhotosBtn = findViewById(R.id.poi_pick_photos);
        pickPhotosBtn.setVisibility(View.GONE);

        MaterialButton saveBtn = findViewById(R.id.save_poi);
        saveBtn.setVisibility(View.GONE);

        toolbar.getMenu().getItem(0).setVisible(false);
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24);

        // Hide inputs
        TextInputLayout nameInput = findViewById(R.id.poi_name_input);
        nameInput.setVisibility(View.GONE);

        TextInputLayout descriptionInput = findViewById(R.id.poi_description_input);
        descriptionInput.setVisibility(View.GONE);

        // Set description
        View description = findViewById(R.id.poi_description);
        uiUpdateCard(description, R.drawable.ic_description,
                getString(R.string.description), poi.getDescription());
        description.setVisibility(View.VISIBLE);

        // Set images
        CircularProgressIndicator progressIndicator = findViewById(R.id.progress_indicator);
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

        // Set comments
        setComments();
    }

    private void uiUpdateCard(View card, @DrawableRes int iconId, CharSequence textTitle, CharSequence textSubtitle) {
        // Set card icon
        ImageView icon = card.findViewById(R.id.card_icon);
        icon.setImageResource(iconId);

        // Set card title
        TextView title = card.findViewById(R.id.card_title);
        title.setText(textTitle);

        // Set card subtitle
        TextView subtitle = card.findViewById(R.id.card_subtitle);
        subtitle.setText(textSubtitle);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("IntentReset")
    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.poi_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void addImageToGallery(Bitmap bitmap) {
        GridLayout gallery = findViewById(R.id.poi_gallery);
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

    /*** -------------------------------------------- ***/
    /*** ----------------- COMMENTS ----------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setComments() {
        ArrayList<PointOfInterest.Comment> comments = poi.getComments();
        int commentsCount = comments.size();

        if (commentsCount > 0) {
            // Set total number comments
            TextView total = findViewById(R.id.comments_card_subtitle);
            String s = commentsCount + " " + getString(R.string.comments).toLowerCase();
            if (commentsCount == 1) s = s.substring(0, s.length() - 1);
            total.setText(s);

            LinearLayout linearLayout = findViewById(R.id.comments_list);
            for (PointOfInterest.Comment comment : comments) {
                LayoutInflater inflater = LayoutInflater.from(this);
                ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.comment_item, null, false);

                // Set avatar & name
                (new Utils.httpRequestJson(obj -> {
                    if (!obj.get("status").getAsString().equals("success")) return;

                    TextView name = layout.findViewById(R.id.comment_item_name);
                    String userName = obj.get("data").getAsJsonObject().get("name").getAsString();
                    name.setText(userName);

                    ImageView avatar = layout.findViewById(R.id.comment_item_avatar);
                    String avatarURL = obj.get("data").getAsJsonObject().get("avatar").getAsString();
                    (new Utils.httpRequestImage(avatar::setImageBitmap)).execute(avatarURL);

                })).execute(SERVER_URL + "/get-user-info?uid=" + comment.getAuthorUID());


                // Set comment
                TextView msg = layout.findViewById(R.id.comment_item_msg);
                msg.setText(comment.getMsg());

                // Set images
                // TODO

                // Set date
                TextView date = layout.findViewById(R.id.comment_item_date);
                Timestamp timestamp = new Timestamp(Long.parseLong(comment.getCreationTimestamp()));
                LocalDate localDate = timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                date.setText(localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));

                linearLayout.addView(layout);
            }

            // Show comments' card
            MaterialCardView commentsCard = findViewById(R.id.poi_comments);
            commentsCard.setVisibility(View.VISIBLE);
        }
    }
}