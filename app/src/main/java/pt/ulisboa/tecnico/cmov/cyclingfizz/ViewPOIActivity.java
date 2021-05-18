package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;

import static android.view.View.GONE;


public class ViewPOIActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@ViewPOI";

    static String SERVER_URL = Utils.STATIONS_SERVER_URL;
    public final static String ROUTE_ID = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_ID";
    public final static String COMMENT_INDEX = "pt.ulisboa.tecnico.cmov.cyclingfizz.COMMENT_INDEX";

    PointOfInterest poi;
    String routeID;

    int sortBy = R.id.sort_most_recent;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn();
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

    PointOfInterest.Comment comment;
    // Menu for flag create
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        comment = (PointOfInterest.Comment) v.getTag();

        getMenuInflater().inflate(R.menu.flag_menu, menu);
    }


    // Menu for flag onclick
    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.flag_as_inappropriate) {

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.are_you_sure_flag)
                    .setMessage(R.string.are_you_sure_flag_message)
                    .setNeutralButton(R.string.cancel, null)
                    .setPositiveButton(R.string.are_you_sure_flag_positive, (dialog, which) -> {
                        comment.flag(routeID, poi.getId(),ignored -> {
                            uiSetComments();
                        });
                    })
                    .show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUI() {
        MaterialToolbar toolbar = findViewById(R.id.poi_toolbar).findViewById(R.id.topAppBar);

        Utils.setStatusBarColor(this, getColor(R.color.purple_700));
        uiSetToolbar(toolbar);
        uiUpdateButtons(toolbar);
        uiHideInputs();
        uiSetDescription();
        uiSetComments();
        uiSetImages();
    }

    private void uiSetToolbar(MaterialToolbar toolbar) {
        // Set toolbar title as POI name
        toolbar.setTitle(poi.getName());
    }

    private void uiUpdateButtons(MaterialToolbar toolbar) {
        // Hide / change btns
        MaterialButton takePhotosBtn = findViewById(R.id.poi_take_photo);
        takePhotosBtn.setVisibility(GONE);

        MaterialButton pickPhotosBtn = findViewById(R.id.poi_pick_photos);
        pickPhotosBtn.setVisibility(GONE);

        MaterialButton saveBtn = findViewById(R.id.save_poi);
        saveBtn.setVisibility(GONE);

        toolbar.getMenu().getItem(0).setVisible(false);
        toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24);

        MaterialButton commentBtn = findViewById(R.id.leave_comment);
        commentBtn.setVisibility(View.VISIBLE);
    }

    private void uiHideInputs() {
        TextInputLayout nameInput = findViewById(R.id.poi_name_input);
        nameInput.setVisibility(GONE);

        TextInputLayout descriptionInput = findViewById(R.id.poi_description_input);
        descriptionInput.setVisibility(GONE);
    }

    private void uiSetDescription() {
        View description = findViewById(R.id.poi_description);
        uiUpdateCard(description, R.drawable.ic_description, getString(R.string.description), poi.getDescription());
        description.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void uiSetImages() {
        CircularProgressIndicator progressIndicator = findViewById(R.id.progress_indicator);
        progressIndicator.setVisibility(View.VISIBLE);

        (new Thread(() -> {
            poi.downloadImages(ignored -> {
                runOnUiThread(() -> {
                    GridLayout gallery = findViewById(R.id.poi_gallery);
                    ArrayList<Bitmap> poiImages = poi.getImages();

                    for (int i = 0; i < poiImages.size(); i++) {
                        Bitmap bitmap = poiImages.get(i);
                        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, Utils.THUMBNAIL_SIZE_MEDIUM, Utils.THUMBNAIL_SIZE_MEDIUM);
                        ViewGroup imgWrapper = Utils.addImageToGallery(this, thumbImage, gallery, Utils.GALLERY_IMAGE_SIZE_MEDIUM, false, Utils.NO_COLOR);

                        // Set click listeners
                        final int index = i;
                        imgWrapper.setOnClickListener(v -> {
                            ((SharedState) getApplicationContext()).slideshowImages = poiImages;
                            Intent intent = new Intent(this, SlideshowActivity.class);
                            intent.putExtra("index", index);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                        });
                    }

                    if (poiImages.size() > 0) gallery.setVisibility(View.VISIBLE);
                    progressIndicator.setVisibility(GONE);
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.poi_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set add comment btn click listener
        MaterialButton addCommentBtn = findViewById(R.id.leave_comment);
        addCommentBtn.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                Intent intent = new Intent(this, AddCommentActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(ROUTE_ID, routeID);
                intent.putExtras(bundle);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);

            } else {
                new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.sign_in_required)
                    .setMessage(R.string.leaving_a_comment_dialog_warning)
                    .setNeutralButton(R.string.cancel, null)
                    .setPositiveButton(R.string.sign_in, (dialog, which) -> {
                        // Respond to positive button press
                        Intent intent = new Intent(this, LoginActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
                    })
                    .show();
            }
        });

        findViewById(R.id.sort).setOnClickListener(view -> {
            View customDialog = LayoutInflater.from(this)
                    .inflate(R.layout.sort_dialog, null, false);

            RadioGroup radioGroup = customDialog.findViewById(R.id.sort_radio_group);
            radioGroup.check(sortBy);

            radioGroup.findViewById(R.id.sort_best_rate).setVisibility(GONE);
            radioGroup.findViewById(R.id.sort_worst_rate).setVisibility(GONE);

            // Show end trip dialog
            new MaterialAlertDialogBuilder(this)
                    .setView(customDialog)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.apply, (dialog, which) -> {
                        sortBy = radioGroup.getCheckedRadioButtonId();
                        uiSetComments();
                    })
                    .show();
        });
    }

    /*** -------------------------------------------- ***/
    /*** ----------------- COMMENTS ----------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uiSetComments() {
        getFlaggedCommentsId(flaggedComments -> {

            ArrayList<PointOfInterest.Comment> comments = new ArrayList<>();
            for (PointOfInterest.Comment comment : poi.getCommentsNotFlagged()) {
                if (!flaggedComments.contains(comment.getId())) comments.add(comment);
            }

            comments.sort(getSorter());

            // Init RecyclerView
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            RecyclerViewFragment fragment = new RecyclerViewFragment(comments, RecyclerViewFragment.DatasetType.COMMENTS);
            transaction.replace(R.id.comments_list, fragment);
            transaction.commit();

            int commentsCount = poi.getCommentsNotFlagged().size();

            if (commentsCount > 0) {
                // Set total number comments
                TextView total = findViewById(R.id.comments_card_subtitle);
                String s = commentsCount + " " + getString(R.string.comments).toLowerCase();
                if (commentsCount == 1) s = s.substring(0, s.length() - 1);
                total.setText(s);

                // Make comments card visible if has comments
                if (comments.size() > 0) {
                    MaterialCardView commentsCard = findViewById(R.id.poi_comments);
                    commentsCard.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    Comparator<PointOfInterest.Comment> getSorter() {
        switch (sortBy) {
            default:
            case R.id.sort_most_recent:
                return new PointOfInterest.Comment.SortByMostRecent();
            case R.id.sort_least_recent:
                return new PointOfInterest.Comment.SortByLeastRecent();
        }
    }

    private void deleteComment(String commentID) {
        poi.removeComment(commentID, routeID, deleted -> {
            if (deleted) finish();
            else Toast.makeText(this, R.string.could_not_delete_comment, Toast.LENGTH_SHORT).show();
        });
    }

    private void getFlaggedCommentsId(Utils.OnTaskCompleted<ArrayList<String>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();
                (new Utils.httpRequestJson(response -> {
                    if (!response.get("status").getAsString().equals("success")) {
                        callback.onTaskCompleted(new ArrayList<>());
                        return;
                    }
                    ArrayList<String> flaggedComments = new ArrayList<>();
                    JsonArray flaggedCommentsJson = response.get("flagged_comments").getAsJsonArray();

                    for (JsonElement flaggedReviewJson : flaggedCommentsJson) {
                        flaggedComments.add(flaggedReviewJson.getAsString());
                    }

                    callback.onTaskCompleted(flaggedComments);
                })).execute(SERVER_URL + "/get-flagged-comments-by-user-and-route-and-poi?idToken=" + idToken + "&route_id=" + routeID + "&poi_id=" + poi.getId());

            });
        } else {
            callback.onTaskCompleted(new ArrayList<>());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRestart() {
        super.onRestart();
        poi = ((SharedState) getApplicationContext()).viewingPOI;
        uiSetComments();
    }
}