package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.layers.TransitionOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.has;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.not;
import static com.mapbox.mapboxsdk.style.expressions.Expression.step;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textFont;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;

public class RouteActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@RouteActivity";
    public final static String ROUTE_ID = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_ID";
    public final static String RATE = "pt.ulisboa.tecnico.cmov.cyclingfizz.RATE";

    static String PATH_SOURCE_ID = "path-source";
    static String PATH_LAYER_ID = "path-layer";

    static String POI_SOURCE_ID = "poi-source";
    static String POI_ICON_ID = "poi-icon";
    static String POI_LAYER_ID = "poi-layer";
    static String POI_CLUSTER_LAYER_ID = "poi-cluster-layer";
    static String POI_COUNT_LAYER_ID = "poi-count-layer";

    FirebaseAuth mAuth;
    Route route;

    private MapView mapView;
    private MapboxMap mapboxMap;

    DecimalFormat oneDecimalFormatter = new DecimalFormat("#.0");
    private boolean hasStartedSnapshotGeneration;

    int sortBy = R.id.sort_best_rate;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn();
        super.onCreate(savedInstanceState);

        if (!Mapbox.hasInstance()) {
            Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        }
        setContentView(R.layout.route);

        mAuth = FirebaseAuth.getInstance();

        hasStartedSnapshotGeneration = false;

        // Get route
        Intent intent = getIntent();
        Uri data = intent.getData();
        String routeID;

        if (data != null) {
            routeID = data.getQueryParameter("routeID");
        } else {
            routeID = intent.getStringExtra(RoutesListActivity.ROUTE_ID);
        }

        (new Utils.httpRequestJson(obj -> {
            if (!obj.get("status").getAsString().equals("success")) {
                Toast.makeText(this, "Error: couldn't get route", Toast.LENGTH_LONG).show();
                return;
            }

            route = Route.fromJson(obj.get("data").getAsJsonObject());
            initMap(savedInstanceState);
            uiInit();

        })).execute(Utils.STATIONS_SERVER_URL + "/get-route-by-id?routeID=" + routeID);
    }



    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/
    Route.Review review;
    // Menu for flag create
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        review = (Route.Review) v.getTag();

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
                        review.flag(route.getId(), ignored -> {
                            resetRate();
                            updateReviews();
                        });
                    })
                    .show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uiInit() {
        // Update view
        uiUpdateUserRate();
        uiUpdateTopBar(route.getTitle());
        uiUpdateRouteRate();
        updateAuthor();
        uiUpdateCard(findViewById(R.id.route_description), R.drawable.ic_description,
                getString(R.string.description), route.getDescription());
        updateVideo();
        updatePOIs();
        updateReviews();

        // Set click listeners
        uiSetClickListeners();
    }

    private void uiUpdateTopBar(String name) {
        // Set top bar title
        TextView title = findViewById(R.id.taller_top_bar_name);
        title.setText(name);

        // Set top bar icon
        ImageView icon = findViewById(R.id.taller_top_bar_icon);
        icon.setImageResource(R.drawable.ic_route);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void uiUpdateRouteRate() {
        int rateCount = route.getRates().size();

        TextView reviews = findViewById(R.id.route_nr_reviews);
        String s = "(" + rateCount + " reviews)";
        reviews.setText(s);

        if (rateCount == 0) return;

        float rateAvg = 0;
        for (Integer rate : route.getRates()) {
            rateAvg += rate.floatValue() / rateCount;
        }

        // Update view
        TextView rateValue = findViewById(R.id.route_rate_value);
        rateValue.setText(oneDecimalFormatter.format(rateAvg));
        rateValue.setTextColor(getColorFromRate(rateAvg));

        ImageView rateIcon = findViewById(R.id.route_rate_icon);
        rateIcon.setColorFilter(getColorFromRate(rateAvg));
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void uiUpdateUserRate() {
        // Check if user played this route
        route.checkIfUserPlayedRoute(hasPlayed -> {
            if (hasPlayed) {

                CircularProgressIndicator progressIndicator = findViewById(R.id.route_rate_card_progress_indicator);
                progressIndicator.setVisibility(View.VISIBLE);

                // Check if review already posted
                route.getReviewOfCurrentUser(review -> {
                    if (review != null) {
                        MaterialButton editReviewBtn = findViewById(R.id.edit_review);
                        editReviewBtn.setVisibility(View.VISIBLE);
                        editReviewBtn.setOnClickListener(v -> {
                            SharedState sharedState = (SharedState) getApplicationContext();
                            sharedState.editingReview = review;
                            sharedState.reviewingRoute = route;

                            Intent intent = new Intent(this, EditReviewActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                        });

                        for (int i = 1; i <= 5; i++) {
                            ImageView star = findViewById(getResources().getIdentifier("rate_star" + i, "id", getPackageName()));

                            if (i <= review.getRate()) {
                                star.setImageDrawable(getDrawable(R.drawable.ic_round_star_24));
                                star.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.orange_500)));

                            } else {
                                star.setImageDrawable(getDrawable(R.drawable.ic_round_star_border_24));
                                star.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.mtrl_textinput_default_box_stroke_color)));
                            }

                            int rate = i;
                            star.setOnClickListener(v -> {
                                SharedState sharedState = (SharedState) getApplicationContext();
                                sharedState.editingReview = review;
                                sharedState.reviewingRoute = route;

                                Intent intent = new Intent(this, EditReviewActivity.class);
                                intent.putExtra(RATE, rate);
                                startActivity(intent);
                                overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                            });
                        }

                    } else {
                        for (int i = 1; i <= 5; i++) {
                            ImageView star = findViewById(getResources().getIdentifier("rate_star" + i, "id", getPackageName()));
                            int rate = i;

                            // Reset rate
                            star.setImageDrawable(getDrawable(R.drawable.ic_round_star_border_24));
                            star.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.mtrl_textinput_default_box_stroke_color)));

                            star.setOnClickListener(v -> {
                                setRate(rate);

                                SharedState sharedState = (SharedState) getApplicationContext();
                                sharedState.reviewingRoute = route;

                                Intent intent = new Intent(this, AddReviewActivity.class);
                                intent.putExtra(RATE, rate);
                                startActivity(intent);
                                overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                            });
                        }
                    }
                    View rateView = findViewById(R.id.route_rate_card);
                    rateView.setVisibility(View.VISIBLE);
                    progressIndicator.setVisibility(View.GONE);
                });
            }
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setRate(int rate) {
        for (int i = 1; i <= rate; i++) {
            ImageView star = findViewById(getResources().getIdentifier("rate_star" + i, "id", getPackageName()));
            star.setImageDrawable(getDrawable(R.drawable.ic_round_star_24));
            star.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.orange_500)));
        }
    }

    private void updateAuthor() {
        (new Utils.httpRequestJson(obj -> {
            if (!obj.get("status").getAsString().equals("success")) return;

            String authorName = "Anonymous";
            JsonElement authorNameEle = obj.get("data").getAsJsonObject().get("name");
            if (!authorNameEle.isJsonNull()) {
                authorName = authorNameEle.getAsString();
            } else if (!obj.get("data").getAsJsonObject().get("email").isJsonNull()) {
                authorName = Utils.capitalize(obj.get("data").getAsJsonObject().get("email").getAsString()).split("@")[0];
            }
            TextView creator = findViewById(R.id.route_author);
            String s = getString(R.string.created_by) + " " + authorName;
            creator.setText(s);

        })).execute(Utils.STATIONS_SERVER_URL + "/get-user-info?uid=" + route.getAuthorUID());
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
        MaterialToolbar toolbar = findViewById(R.id.taller_top_bar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set share btn click listener
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.share) {
                if (!hasStartedSnapshotGeneration) {
                    hasStartedSnapshotGeneration = true;
                    shareRouteShot();
                }
            } else if (id == R.id.flag){
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.are_you_sure_flag)
                        .setMessage(R.string.are_you_sure_flag_message)
                        .setNeutralButton(R.string.cancel, null)
                        .setPositiveButton(R.string.are_you_sure_flag_positive, (dialog, which) -> {
                            route.flag(ignored -> {
                                finish();
                            });
                        })
                        .show();
            }
            return false;
        });

        // Set play btn click listener
        FloatingActionButton playBtn = findViewById(R.id.route_play);
        playBtn.setOnClickListener(v -> {
            PathRecorder pathRecorder = PathRecorder.getInstance();
            if (pathRecorder.isRecording() || pathRecorder.isPreparingToRecord()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.route_being_recorded)
                        .setMessage(R.string.route_being_recorded_warning)
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                        })
                        .show();
                return;
            }
            // Prevent screen from turning off while recording
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            PathPlayer.getInstance().playRoute(route, preloaded -> {
                Intent intent = new Intent(this, MapActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
            });
        });

        // Set sort btn click listener
        MaterialButton sortBtn = findViewById(R.id.sort);
        sortBtn.setOnClickListener(view -> {
            View customDialog = LayoutInflater.from(this)
                    .inflate(R.layout.sort_dialog, null, false);
            RadioGroup radioGroup = customDialog.findViewById(R.id.sort_radio_group);
            radioGroup.check(sortBy);

            // Show sort dialog
            new MaterialAlertDialogBuilder(this)
                .setView(customDialog)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    sortBy = radioGroup.getCheckedRadioButtonId();
                    resetRate();
                    updateReviews();
                })
                .show();
        });

        // Set video click listener
        View routeVideoThumbnailLayout = findViewById(R.id.route_video_thumbnail_layout);
        routeVideoThumbnailLayout.setOnClickListener(view -> {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri videoURI = FileProvider.getUriForFile(this,
                    "pt.ulisboa.tecnico.cmov.cyclingfizz.fileprovider",
                    route.getVideoFile());
            intent.setDataAndType(videoURI, "video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });
    }

    @SuppressLint("NonConstantResourceId")
    Comparator<Route.Review> getSorter() {
        switch (sortBy) {
            default:
            case R.id.sort_best_rate:
                return new Route.Review.SortByBestRate();
            case R.id.sort_worst_rate:
                return new Route.Review.SortByWorstRate();
            case R.id.sort_most_recent:
                return new Route.Review.SortByMostRecent();
            case R.id.sort_least_recent:
                return new Route.Review.SortByLeastRecent();
        }
    }

    private void shareRouteShot() {
        mapboxMap.snapshot((snapshot -> {
            Uri bmpUri = getLocalBitmapUri(snapshot);
            if (bmpUri != null) {

                Intent shareIntent = new Intent();
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check this route in Cycling Fizz!");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "I just found this awesome bicycle route in Cycling Fizz named, \"" + route.getTitle() + "\"!\n\n" +
                        "https://stations.cfservertest.ga/route?routeID=" + route.getId());
                shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                shareIntent.setType("image/*");
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share Route"));
            }
            hasStartedSnapshotGeneration = false;
        }));
    }


    private Uri getLocalBitmapUri(Bitmap bmp) {
        Uri bmpUri = null;
        FileOutputStream out = null;

        try {
            // Create an image file name
            String imageFileName = "share_image_" + System.currentTimeMillis();
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File file = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".png",         /* suffix */
                    storageDir      /* directory */
            );

            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            try {
                out.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            bmpUri = FileProvider.getUriForFile(this,
                    "pt.ulisboa.tecnico.cmov.cyclingfizz.fileprovider",
                    file);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return bmpUri;

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private int getColorFromRate(float rate) {
        if (rate < 2.5f) return getColor(R.color.pink);
        if (rate < 4.0f) return getColor(R.color.warning);
        return getColor(R.color.success);
    }

    private void getFlaggedReviewsId(Utils.OnTaskCompleted<ArrayList<String>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();
                (new Utils.httpRequestJson(response -> {
                    if (!response.get("status").getAsString().equals("success")) {
                        callback.onTaskCompleted(new ArrayList<>());
                        return;
                    }
                    ArrayList<String> flaggedReviews = new ArrayList<>();
                    JsonArray flaggedReviewsJson = response.get("flagged_reviews").getAsJsonArray();

                    for (JsonElement flaggedReviewJson : flaggedReviewsJson) {
                        flaggedReviews.add(flaggedReviewJson.getAsString());
                    }

                    callback.onTaskCompleted(flaggedReviews);
                })).execute(Utils.STATIONS_SERVER_URL + "/get-flagged-reviews-by-user-and-route?idToken=" + idToken + "&route_id=" + route.getId());

            });
        } else {
            callback.onTaskCompleted(new ArrayList<>());
        }
    }

    private void updateVideo() {

        if (route.getVideoLink() == null) return;

        route.downloadVideo(this, ignored -> {

            if (route.getVideoFile() == null) return;

            Bitmap thumbnailVideo = ThumbnailUtils.createVideoThumbnail(route.getVideoFile().getAbsolutePath(), MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);

            // Update view
            ImageView thumbnailVideoView = findViewById(R.id.route_video_thumbnail);
            View routeVideoThumbnailLayout = findViewById(R.id.route_video_thumbnail_layout);
            thumbnailVideoView.setImageBitmap(thumbnailVideo);
            routeVideoThumbnailLayout.setVisibility(View.VISIBLE);
        });
    }


    /*** -------------------------------------------- ***/
    /*** --------------- ROUTE PREVIEW -------------- ***/
    /*** -------------------------------------------- ***/

    private void initMap(Bundle savedInstanceState) {
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        mapView = findViewById(R.id.previewRouteMapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapbox -> {
            mapboxMap = mapbox;
            mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
                style.setTransition(new TransitionOptions(0, 0, false));

                mapboxMap.getUiSettings().setAllGesturesEnabled(false);

                LatLngBounds latLngBounds;
                ArrayList<Point> path = route.getPath();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    latLngBounds = new LatLngBounds.Builder()
                            .includes(path.stream().map(p -> new LatLng(p.latitude(), p.longitude())).collect(Collectors.toList()))
                            .build();

                } else {
                    Point startPoint = path.get(0);
                    Point centerPoint = path.get((int) (path.size()/2));
                    Point endPoint = path.get(path.size() - 1);

                    latLngBounds = new LatLngBounds.Builder()
                            .include(new LatLng(startPoint.latitude(), startPoint.longitude()))
                            .include(new LatLng(centerPoint.latitude(), centerPoint.longitude()))
                            .include(new LatLng(endPoint.latitude(), endPoint.longitude()))
                            .build();
                }

                mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));

                addIcons(style);
                initRouteLayer(style);
                showRouteOnMap();
                showPOIsOnMap();

                mapboxMap.addOnMapClickListener(point -> {
                    expandMap();
                    return true;
                });
                TextView expandView = findViewById(R.id.preview_route_thumbnail_text);
                expandView.setOnClickListener(v -> expandMap());

                // Map is set up and the style has loaded. Now you can add data or make other map adjustments.
            });
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void addIcons(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage(POI_ICON_ID, Objects.requireNonNull(BitmapUtils.getBitmapFromDrawable(
                getResources().getDrawable(R.drawable.ic_poi_marker))));
    }

    private void initRouteLayer(@NonNull Style style) {
        // Init path layer
        style.addSource(new GeoJsonSource(PATH_SOURCE_ID,
                FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                        LineString.fromLngLats(route.getPath())
                )})));

        LineLayer path = new LineLayer(PATH_LAYER_ID, PATH_SOURCE_ID);
        path.setProperties(
                lineJoin(Property.LINE_JOIN_ROUND),
                lineCap(Property.LINE_CAP_ROUND),
                lineColor(getResources().getColor(R.color.purple_500)),
                lineWidth(5f),
                lineOpacity(.8f)
        );

        style.addLayer(path);

        // Init POIs layer
        ArrayList<Feature> features = new ArrayList<>();
        int i = 0;
        for (PointOfInterest poi : route.getAllPOIs()) {
            Feature poiFeature = Feature.fromGeometry(poi.getCoord());
            poiFeature.addNumberProperty("id", i++);
            features.add(poiFeature);
        }
        style.addSource(new GeoJsonSource(POI_SOURCE_ID,
                        FeatureCollection.fromFeatures(features.toArray(new Feature[0])),
                        new GeoJsonOptions()
                                .withCluster(true)
                                .withClusterRadius(50)
                                .withClusterMaxZoom(14)
                )
        );

        //Creating a marker layer for single data points
        SymbolLayer unclustered = new SymbolLayer(POI_LAYER_ID, POI_SOURCE_ID);

        unclustered.setProperties(
                iconImage(POI_ICON_ID),
                iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconSize(literal(.5f))
        );

        unclustered.setFilter(not(has("point_count")));
        style.addLayer(unclustered);

        CircleLayer circles = new CircleLayer(POI_CLUSTER_LAYER_ID, POI_SOURCE_ID);

        //Add clusters' circles
        circles.setProperties(
                circleColor(getResources().getColor(R.color.purple_500)),
                circleRadius(step(get("point_count"), 25,
                        stop(5, 35),
                        stop(10, 45),
                        stop(20, 55))),
                circleOpacity(0.8f)
        );
        circles.setFilter(has("point_count"));
        style.addLayer(circles);

        //Add the count labels
        SymbolLayer count = new SymbolLayer(POI_COUNT_LAYER_ID, POI_SOURCE_ID);
        count.setProperties(
                textField(Expression.toString(get("point_count"))),
                textSize(12f),
                textColor(getResources().getColor(R.color.white)),
                textIgnorePlacement(true),
                textAllowOverlap(true),
                textFont(Expression.literal(R.font.quicksand_bold))
        );
        style.addLayer(count);
    }

    private void showRouteOnMap() {
        if (mapboxMap.getStyle() == null) return;

        GeoJsonSource pathSource = mapboxMap.getStyle().getSourceAs(PATH_SOURCE_ID);
        if (pathSource != null) {
            FeatureCollection featureCollection = FeatureCollection.fromFeatures(
                    new Feature[] {Feature.fromGeometry(
                            LineString.fromLngLats(route.getPath())
                    )}
            );
            runOnUiThread(() -> pathSource.setGeoJson(featureCollection));
        }
    }

    private void showPOIsOnMap() {

        if (mapboxMap.getStyle() == null) return;

        GeoJsonSource POIsSource = mapboxMap.getStyle().getSourceAs(POI_SOURCE_ID);
        if (POIsSource != null) {
            ArrayList<Feature> features = new ArrayList<>();
            int i = 0;
            for (PointOfInterest poi : route.getAllPOIs()) {
                Feature poiFeature = Feature.fromGeometry(poi.getCoord());
                poiFeature.addNumberProperty("id", i++);
                features.add(poiFeature);
            }
            FeatureCollection featureCollection = FeatureCollection.fromFeatures(
                    features.toArray(new Feature[0])
            );
            runOnUiThread(() -> POIsSource.setGeoJson(featureCollection));
        }
    }

    private void expandMap() {
        Intent intent = new Intent(this, MapPreviewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(MapPreviewActivity.ROUTE_PATH, route.getPath());
        bundle.putSerializable(MapPreviewActivity.ROUTE_POIS, route.getAllPOIs());
        bundle.putString(ROUTE_ID, route.getId());
        intent.putExtras(bundle);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
    }


    /*** -------------------------------------------- ***/
    /*** ------------------- POIs ------------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updatePOIs() {
        ArrayList<PointOfInterest> pois = route.getAllPOIs();

        // Init RecyclerView
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        RecyclerViewFragment fragment = new RecyclerViewFragment(pois, RecyclerViewFragment.DatasetType.POIS, false, route.getId());
        transaction.replace(R.id.poi_list, fragment);
        transaction.commit();

        // Make POIs card visible if has POIs
        if (pois.size() > 0) {
            MaterialCardView poisLayout = findViewById(R.id.route_pois);
            poisLayout.setVisibility(View.VISIBLE);
        }
    }


    /*** -------------------------------------------- ***/
    /*** ----------------- REVIEWS ------------------ ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateReviews() {
        getFlaggedReviewsId(flaggedReviews -> {

            ArrayList<Route.Review> reviews = new ArrayList<>();
            int rateSum = 0;
            Integer[] histogramCounts = Collections.nCopies(5, 0).toArray(new Integer[0]);

            for (Route.Review review : route.getReviewsNotFlagged()) {
                int rate = review.getRate();
                rateSum += rate;
                histogramCounts[Math.round(rate) > 5 ? 4 : Math.round(rate) - 1]++;

                if (!flaggedReviews.contains(review.getId())) reviews.add(review);
            }

            reviews.sort(getSorter());

            // Init RecyclerView
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            RecyclerViewFragment fragment = new RecyclerViewFragment(reviews, RecyclerViewFragment.DatasetType.REVIEWS);
            transaction.replace(R.id.reviews_list, fragment);
            transaction.commit();

            int reviewsCount = route.getReviewsNotFlagged().size();

            if (reviewsCount > 0) {
                // Set total number reviews
                TextView total = findViewById(R.id.reviews_card_subtitle);
                String s = reviewsCount + " " + getString(R.string.reviews).toLowerCase();
                if (reviewsCount == 1) s = s.substring(0, s.length() - 1);
                total.setText(s);

                // Set histogram
                float rateAvg = (float) rateSum / reviewsCount;
                TextView avg = findViewById(R.id.histogram_avg);
                avg.setText(oneDecimalFormatter.format(rateAvg));
                avg.setTextColor(getColorFromRate(rateAvg));

                int maxRate = Math.min(Math.round(rateAvg), 5);
                for (int i = 1; i <= maxRate; i++) {
                    ImageView star = findViewById(getResources().getIdentifier("histogram_star" + i, "id", getPackageName()));
                    star.setColorFilter(getColorFromRate(rateAvg));
                }

                final float scale = getResources().getDisplayMetrics().density;
                for (int i = 1; i <= 5; i++) {
                    LinearLayout bar = findViewById(getResources().getIdentifier("histogram_bar" + i, "id", getPackageName()));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, (int) (5 * scale));
                    params.weight = (float) histogramCounts[i - 1] / reviewsCount;
                    bar.setLayoutParams(params);
                }

                TextView totalReviews = findViewById(R.id.histogram_total);
                String str = "(" + reviewsCount + ")";
                totalReviews.setText(str);

                // Make reviews card visible if has reviews
                if (reviews.size() > 0) {
                    MaterialCardView reviewsCard = findViewById(R.id.route_reviews);
                    reviewsCard.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void resetRate() {
        // Reset rate
        View rateCard = findViewById(R.id.route_rate_card);
        rateCard.setVisibility(View.GONE);

        for (int i = 1; i <= 5; i++) {
            ImageView star = findViewById(getResources().getIdentifier("rate_star" + i, "id", getPackageName()));
            star.setImageDrawable(getDrawable(R.drawable.ic_round_star_border_24));
            star.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.mtrl_textinput_default_box_stroke_color)));
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------------ ACTIVITY LIFECYCLE ------------ ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRestart() {
        super.onRestart();

        (new Utils.httpRequestJson(obj -> {
            if (!obj.get("status").getAsString().equals("success")) {
                Toast.makeText(this, "Error: couldn't get route", Toast.LENGTH_LONG).show();
                return;
            }

            route = Route.fromJson(obj.get("data").getAsJsonObject());

            resetRate();

            uiUpdateUserRate();
            updateReviews();
            uiUpdateRouteRate();

        })).execute(Utils.STATIONS_SERVER_URL + "/get-route-by-id?routeID=" + route.getId());
    }
}