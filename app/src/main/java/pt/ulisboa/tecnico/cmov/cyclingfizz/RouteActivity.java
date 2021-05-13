package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
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
    static String SERVER_URL = Utils.STATIONS_SERVER_URL;
    public final static String POI = "pt.ulisboa.tecnico.cmov.cyclingfizz.POI";
    public final static String ROUTE_ID = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_ID";

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route);

        mAuth = FirebaseAuth.getInstance();

        hasStartedSnapshotGeneration = false;

        // Get route
        String routeID = getIntent().getStringExtra(RoutesListActivity.ROUTE_ID);
        (new Utils.httpRequestJson(obj -> {
            if (!obj.get("status").getAsString().equals("success")) {
                Toast.makeText(this, "Error: couldn't get route", Toast.LENGTH_LONG).show();
                return;
            }

            route = Route.fromJson(obj.get("data").getAsJsonObject());
            initMap(savedInstanceState);
            uiInit();

        })).execute(SERVER_URL + "/get-route-by-id?routeID=" + routeID);
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uiInit() {
        // Update view
        uiUpdateTopBar(route.getTitle());
        uiUpdateRate();
        updateAuthor();
        uiUpdateCard(findViewById(R.id.route_description), R.drawable.ic_description,
                getString(R.string.description), route.getDescription());
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
    private void uiUpdateRate() {
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

        })).execute(SERVER_URL + "/get-user-info?uid=" + route.getAuthorUID());
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
            };
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

            PathPlayer.getInstance().playRoute(route);
            Intent intent = new Intent(this, MapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
        });
    }

    private void shareRouteShot() {
        mapboxMap.snapshot((snapshot -> {
            Uri bmpUri = getLocalBitmapUri(snapshot);
            if (bmpUri != null) {

                Intent shareIntent = new Intent();
                shareIntent.putExtra(Intent.EXTRA_TEXT, "I just found this awesome bicycle route in Cycling Fizz: \"" + route.getTitle() + "\"!");
                shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                shareIntent.setType("*/*");
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share map image"));
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
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
    }


    /*** -------------------------------------------- ***/
    /*** ------------------- POIs ------------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updatePOIs() {
        LinearLayout linearLayout = findViewById(R.id.poi_list);
        int i = 1;

        for (PointOfInterest poi : route.getAllPOIs()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.poi_item, null, false);

            // Set order
            TextView order = layout.findViewById(R.id.poi_item_order);
            order.setText(String.valueOf(i));

            // Set thumbnail
            if (poi.getMediaLinks().size() > 0) {
                poi.downloadAndGetImage(0, bitmap -> {
                    ImageView thumbnail = layout.findViewById(R.id.poi_item_thumbnail);
                    Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, 128, 128);
                    thumbnail.setImageBitmap(thumbImage);
                });
            }

            //Set title
            TextView title = layout.findViewById(R.id.poi_item_title);
            title.setText(poi.getName());

            //Set description
            TextView description = layout.findViewById(R.id.route_card_description);
            description.setText(poi.getDescription());

            linearLayout.addView(layout);

            // Set poi click listener
            layout.setOnClickListener(v -> {
                SharedState sharedState = (SharedState) getApplicationContext();
                sharedState.viewingPOI = poi;

                Intent intent = new Intent(this, ViewPOIActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(ROUTE_ID, route.getId());
                intent.putExtras(bundle);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
            });

            i++;
        }

        if (route.getAllPOIs().size() > 0) {
            MaterialCardView poisLayout = findViewById(R.id.route_pois);
            poisLayout.setVisibility(View.VISIBLE);
        }
    }


    /*** -------------------------------------------- ***/
    /*** ----------------- REVIEWS ------------------ ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateReviews() {
        ArrayList<Route.Review> reviews = route.getReviews();
        int reviewsCount = reviews.size();

        if (reviewsCount > 0) {
            // Set total number reviews
            TextView total = findViewById(R.id.reviews_card_subtitle);
            String s = reviewsCount + " " + getString(R.string.reviews).toLowerCase();
            if (reviewsCount == 1) s = s.substring(0, s.length() - 1);
            total.setText(s);

            int rateSum = 0;
            Integer[] histogramCounts = Collections.nCopies(5, 0).toArray(new Integer[0]);
            LinearLayout linearLayout = findViewById(R.id.reviews_list);
            for (Route.Review review : reviews) {
                rateSum += review.getRate();
                LayoutInflater inflater = LayoutInflater.from(this);
                ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.review_item, null, false);

                // Set avatar & name
                (new Utils.httpRequestJson(obj -> {
                    if (!obj.get("status").getAsString().equals("success")) return;

                    TextView name = layout.findViewById(R.id.review_item_name);
                    String userName = obj.get("data").getAsJsonObject().get("name").getAsString();
                    name.setText(userName);

                    ImageView avatar = layout.findViewById(R.id.review_item_avatar);
                    String avatarURL = obj.get("data").getAsJsonObject().get("avatar").getAsString();
                    (new Utils.httpRequestImage(bitmap -> {
                        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, 128, 128);
                        avatar.setImageBitmap(thumbImage);
                    })).execute(avatarURL);

                })).execute(SERVER_URL + "/get-user-info?uid=" + review.getAuthorUID());


                // Set comment
                TextView comment = layout.findViewById(R.id.review_item_comment);
                comment.setText(review.getMsg() != null ? review.getMsg() : getString(R.string.no_comment));

                // Set rate
                int rate = review.getRate();
                histogramCounts[Math.round(rate) > 5 ? 4 : Math.round(rate) - 1]++;
                TextView rateValue = layout.findViewById(R.id.review_item_rate_value);
                ImageView rateIcon = layout.findViewById(R.id.review_item_rate_icon);
                rateValue.setText(String.valueOf(rate));
                rateValue.setTextColor(getColorFromRate(rate));
                rateIcon.setColorFilter(getColorFromRate(rate));

                // Set images
                (new Thread(() -> {
                    review.downloadImages(ignored -> {
                        runOnUiThread(() -> {
                            GridLayout gallery = findViewById(R.id.review_item_gallery);
                            for (Bitmap bitmap : review.getImages()) {
                                Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, 128, 128);
                                addImageToGallery(thumbImage, gallery);
                            }
                            if (review.getImages().size() > 0) gallery.setVisibility(View.VISIBLE);
                        });
                    });
                })).start();

                // Set date
                TextView date = layout.findViewById(R.id.review_item_date);
                Timestamp timestamp = new Timestamp(Long.parseLong(review.getCreationTimestamp()));
                LocalDate localDate = timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                date.setText(localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));

                linearLayout.addView(layout);
            }

            // Set histogram
            float rateAvg = (float) rateSum / reviewsCount;
            TextView avg = findViewById(R.id.histogram_avg);
            avg.setText(oneDecimalFormatter.format(rateAvg));
            avg.setTextColor(getColorFromRate(rateAvg));

            int maxRate = Math.round(rateAvg) > 5 ? 5 : Math.round(rateAvg);
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

            // Show reviews' card
            MaterialCardView reviewsCard = findViewById(R.id.route_reviews);
            reviewsCard.setVisibility(View.VISIBLE);
        }
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
}