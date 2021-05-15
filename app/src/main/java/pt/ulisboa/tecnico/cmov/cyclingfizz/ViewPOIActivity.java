package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

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
                            updateComments();
                        });
                    })
                    .show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

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
                    int index = 0;
                    for (Bitmap bitmap : poi.getImages()) {
                        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, 256, 256);
                        addImageToGallery(thumbImage, gallery, 110, poi.getImages(), index++);
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
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void addImageToGallery(Bitmap bitmap, GridLayout gallery, int size, ArrayList<Bitmap> images, int index) {
        final float scale = getResources().getDisplayMetrics().density;

        // Create wrapper
        ConstraintLayout imgWrapper = new ConstraintLayout(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = (int) (size * scale);
        params.height = (int) (size * scale);
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

        imgWrapper.setOnClickListener(v -> {
            Log.d(TAG, String.valueOf(images.size()));
            ((SharedState) getApplicationContext()).slideshowImages = images;
            Intent intent = new Intent(this, SlideshowActivity.class);
            intent.putExtra("index", index);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
        });
    }

    /*** -------------------------------------------- ***/
    /*** ----------------- COMMENTS ----------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setComments() {
        getFlaggedCommentsId(flaggedComments -> {
            ArrayList<PointOfInterest.Comment> comments = poi.getCommentsNotFlagged(flaggedComments);
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
                        JsonElement avatarURLElement = obj.get("data").getAsJsonObject().get("avatar");
                        if (!avatarURLElement.isJsonNull()) {
                            String avatarURL = avatarURLElement.getAsString();
                            (new Utils.httpRequestImage(bitmap -> {
                                Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, 128, 128);
                                avatar.setImageBitmap(thumbImage);
                            })).execute(avatarURL);
                        }

                        // set context menu for flag
                        layout.setTag(comment);
                        registerForContextMenu(layout);

                    })).execute(SERVER_URL + "/get-user-info?uid=" + comment.getAuthorUID());

                    // Set comment
                    TextView msg = layout.findViewById(R.id.comment_item_msg);
                    msg.setText(comment.getMsg());

                    // Set images
                    (new Thread(() -> {
                        comment.downloadImages(ignored -> {
                            runOnUiThread(() -> {
                                GridLayout gallery = layout.findViewById(R.id.comment_item_gallery);
                                int index = 0;
                                for (Bitmap bitmap : comment.getImages()) {
                                    Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, 256, 256);
                                    addImageToGallery(thumbImage, gallery, 75, comment.getImages(), index++);
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

                    if (user != null && comment.getAuthorUID().equals(user.getUid())) {
                        ImageView deleteBtn = layout.findViewById(R.id.comment_item_delete);
                        deleteBtn.setVisibility(View.VISIBLE);
                        int commentIndex = i;
                        deleteBtn.setOnClickListener(v -> {
                            new MaterialAlertDialogBuilder(this, R.style.SecondaryAlertDialog)
                                    .setTitle(R.string.delete_comment)
                                    .setMessage(R.string.delete_comment_warning)
                                    .setNeutralButton(R.string.cancel, null)
                                    .setPositiveButton(R.string.delete, (dialog, which) -> deleteComment(commentIndex))
                                    .show();
                        });
                    }


                    linearLayout.addView(layout);
                    i++;


                }

                // Show comments' card
                MaterialCardView commentsCard = findViewById(R.id.poi_comments);
                commentsCard.setVisibility(View.VISIBLE);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateComments() {
        LinearLayout linearLayout = findViewById(R.id.comments_list);
        linearLayout.removeAllViews();
        setComments();
    }

    private void deleteComment(int commentIndex) {
        poi.removeComment(commentIndex, routeID, deleted -> {
            if (deleted) finish();
            else Toast.makeText(this, "Could not delete comment", Toast.LENGTH_SHORT).show();
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
        updateComments();
    }
}