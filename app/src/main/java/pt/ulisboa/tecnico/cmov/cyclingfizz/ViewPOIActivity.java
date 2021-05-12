package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;


public class ViewPOIActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@ViewPOI";
    static String SERVER_URL = Utils.STATIONS_SERVER_URL;
    public final static String ROUTE_ID = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_ID";
    public final static String COMMENT_INDEX = "pt.ulisboa.tecnico.cmov.cyclingfizz.COMMENT_INDEX";

    PointOfInterest poi;
    String routeID;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poi);

        poi = ((SharedState) getApplicationContext()).viewingPOI;
        routeID = getIntent().getStringExtra(MapPreviewActivity.ROUTE_ID);

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

        MaterialButton commentBtn = findViewById(R.id.leave_comment);
        commentBtn.setVisibility(View.VISIBLE);

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

        // Set comments
        setComments();

        // Set images
        CircularProgressIndicator progressIndicator = findViewById(R.id.progress_indicator);
        progressIndicator.setVisibility(View.VISIBLE);
        (new Thread(() -> {
            poi.downloadImages(ignored -> {
                runOnUiThread(() -> {
                    GridLayout gallery = findViewById(R.id.poi_gallery);
                    for (Bitmap bitmap : poi.getImages()) {
                        addImageToGallery(bitmap, gallery);
                    }
                    if (poi.getImages().size() > 0) gallery.setVisibility(View.VISIBLE);
                    progressIndicator.setVisibility(View.GONE);
                });
            });
        })).start();
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

    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.poi_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set add comment btn click listener
        MaterialButton addCommentBtn = findViewById(R.id.leave_comment);
        addCommentBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCommentActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(ROUTE_ID, routeID);
            intent.putExtras(bundle);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void addImageToGallery(Bitmap bitmap, GridLayout gallery) {
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

            int i = 0;
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
                (new Thread(() -> {
                    comment.downloadImages(ignored -> {
                        runOnUiThread(() -> {
                            GridLayout gallery = findViewById(R.id.comment_item_gallery);
                            for (Bitmap bitmap : comment.getImages()) {
                                addImageToGallery(bitmap, gallery);
                            }
                            if (comment.getImages().size() > 0) gallery.setVisibility(View.VISIBLE);
                        });
                    });
                })).start();

                // Set date
                TextView date = layout.findViewById(R.id.comment_item_date);
                Timestamp timestamp = new Timestamp(Long.parseLong(comment.getCreationTimestamp()));
                LocalDate localDate = timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                date.setText(localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));

                // Enable editing if created by user
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                assert user != null;
                if (comment.getAuthorUID().equals(user.getUid())) {
                    ImageView editBtn = layout.findViewById(R.id.comment_item_edit);
                    editBtn.setVisibility(View.VISIBLE);
                    int commentIndex = i;
                    editBtn.setOnClickListener(v -> {
                        Intent intent = new Intent(this, EditCommentActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString(ROUTE_ID, routeID);
                        bundle.putInt(COMMENT_INDEX, commentIndex);
                        intent.putExtras(bundle);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                    });
                }

                linearLayout.addView(layout);
                i++;
            }

            // Show comments' card
            MaterialCardView commentsCard = findViewById(R.id.poi_comments);
            commentsCard.setVisibility(View.VISIBLE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateComments() {
        LinearLayout linearLayout = findViewById(R.id.comments_list);
        linearLayout.removeAllViews();
        setComments();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onResume() {
        super.onResume();
        poi = ((SharedState) getApplicationContext()).viewingPOI;
        updateComments();
    }
}