 package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
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

import java.util.ArrayList;
import java.util.Arrays;
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

public class NewRouteActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@NewRoute";
    static final int PICK_IMAGES = 1;

    PathRecorder pathRecorder;

    TextInputLayout nameInputLayout;
    TextInputLayout descriptionInputLayout;

    static String PATH_RECORDED_SOURCE_ID = "path-recorded-source";
    static String PATH_RECORDED_LAYER_ID = "path-recorded-layer";

    static String POI_SOURCE_ID = "poi-source";
    static String POI_ICON_ID = "poi-icon";
    static String POI_LAYER_ID = "poi-layer";
    static String POI_CLUSTER_LAYER_ID = "poi-cluster-layer";
    static String POI_COUNT_LAYER_ID = "poi-count-layer";

    private MapView mapView;
    private MapboxMap mapboxMap;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_route);

        pathRecorder = PathRecorder.getInstance();

        uiSetClickListeners();
        setInputs();
        setPOIs();

        initMap(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == PICK_IMAGES && resultCode == RESULT_OK && data != null) {
                Log.d(TAG, String.valueOf(data.getData()));
                // Get URI
                Uri uri = data.getData();

                // Update view
                ImageView thumbnail = findViewById(R.id.route_thumbnail);
                thumbnail.setImageURI(uri);

            } else {
                Toast.makeText(this, "No photo selected", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, Arrays.toString(e.getStackTrace()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @SuppressLint("IntentReset")
    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.new_route_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_route)
                    .setMessage(R.string.delete_route_warning)
                    .setNeutralButton(R.string.cancel, (dialog, which) -> {
                        // Respond to neutral button press
                    })
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        // Respond to positive button press
                        finish();
                    })
                    .show();
        });

        // Set toolbar action btn click listener
        toolbar.setOnMenuItemClickListener(item -> {
            saveRoute();
            return false;
        });

        // Set thumbnail btn click listener
        CardView thumbnail = findViewById(R.id.new_route_thumbnail);
        thumbnail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGES);
        });

        // Set save btn click listener
        MaterialButton saveBtn = findViewById(R.id.save_route);
        saveBtn.setOnClickListener(v -> saveRoute());
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    latLngBounds = new LatLngBounds.Builder()
                            .includes(pathRecorder.getPath().stream().map(p -> new LatLng(p.latitude(), p.longitude())).collect(Collectors.toList()))
                            .build();

                } else {
                    Point startPoint = pathRecorder.getPath().get(0);
                    Point centerPoint = pathRecorder.getCenterPoint();
                    Point endPoint = pathRecorder.getPath().get(pathRecorder.getPath().size() - 1);

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

    private void expandMap() {
        Intent intent = new Intent(this, MapPreviewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(MapPreviewActivity.ROUTE_PATH, pathRecorder.getPath());
        bundle.putSerializable(MapPreviewActivity.ROUTE_POIS, pathRecorder.getAllPOIs());
        intent.putExtras(bundle);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
    }

    private void initRouteLayer(@NonNull Style style) {
        // Init path recorded layer
        style.addSource(new GeoJsonSource(PATH_RECORDED_SOURCE_ID,
                FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                        LineString.fromLngLats(pathRecorder.getPath())
                )})));

        LineLayer pathRecorded = new LineLayer(PATH_RECORDED_LAYER_ID, PATH_RECORDED_SOURCE_ID);
        pathRecorded.setProperties(
                lineJoin(Property.LINE_JOIN_ROUND),
                lineCap(Property.LINE_CAP_ROUND),
                lineColor(getResources().getColor(R.color.purple)),
                lineWidth(5f),
                lineOpacity(.8f)
        );

        style.addLayer(pathRecorded);

        // Init POIs layer
        ArrayList<Feature> features = new ArrayList<>();
        int i = 0;
        for (PointOfInterest poi : pathRecorder.getAllPOIs()) {
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
                circleColor(getResources().getColor(R.color.purple)),
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

        GeoJsonSource pathRecordedSource = mapboxMap.getStyle().getSourceAs(PATH_RECORDED_SOURCE_ID);
        if (pathRecordedSource != null) {
            FeatureCollection featureCollection = FeatureCollection.fromFeatures(
                    new Feature[] {Feature.fromGeometry(
                            LineString.fromLngLats(pathRecorder.getPath())
                    )}
            );
            runOnUiThread(() -> pathRecordedSource.setGeoJson(featureCollection));
        }
    }

    private void showPOIsOnMap() {

        if (mapboxMap.getStyle() == null) return;

        GeoJsonSource POIsSource = mapboxMap.getStyle().getSourceAs(POI_SOURCE_ID);
        if (POIsSource != null) {
            ArrayList<Feature> features = new ArrayList<>();
            int i = 0;
            for (PointOfInterest poi : pathRecorder.getAllPOIs()) {
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


    /*** -------------------------------------------- ***/
    /*** ------------------ INPUTS ------------------ ***/
    /*** -------------------------------------------- ***/

    private void setInputs() {
        nameInputLayout = findViewById(R.id.poi_name);
        descriptionInputLayout = findViewById(R.id.poi_description);

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
    /*** ------------------- POIs ------------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setPOIs() {
        LinearLayout linearLayout = findViewById(R.id.poi_list);

        for (PointOfInterest poi : pathRecorder.getAllPOIs()){
            LayoutInflater inflater = LayoutInflater.from(this);
            ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.poi_item, null, false);

            //Set title for POI
            TextView title = layout.findViewById(R.id.poi_item_title);
            title.setText(poi.getName());
            //Set description for POI
            TextView description = layout.findViewById(R.id.route_card_description);
            description.setText(poi.getDescription());
            //Add POI's images
            for (Bitmap bitmap: poi.getImages()) {
                addImageToGallery(bitmap, layout);
            }
            GridLayout gallery = layout.findViewById(R.id.poi_gallery);
            if (poi.getImages().size() > 0) gallery.setVisibility(View.VISIBLE);

            linearLayout.addView(layout);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void addImageToGallery(Bitmap bitmap, ConstraintLayout layout) {
        GridLayout gallery = layout.findViewById(R.id.poi_gallery);
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
    }


    /*** -------------------------------------------- ***/
    /*** ------------------- ROUTE ------------------ ***/
    /*** -------------------------------------------- ***/

    public void saveRoute() {
        // Clean error messages
        nameInputLayout.setError(null);
        descriptionInputLayout.setError(null);

        // Get name & description
        String name = Objects.requireNonNull(nameInputLayout.getEditText()).getText().toString();
        String description = Objects.requireNonNull(descriptionInputLayout.getEditText()).getText().toString();

        // Check for errors
        boolean error = checkForErrors(name, description);

        if (!error) {
            LinearProgressIndicator progressIndicator = findViewById(R.id.progress_indicator);
            pathRecorder.saveRecording(name, description, result -> {
                progressIndicator.setVisibility(View.GONE);
                finish();
            });
            progressIndicator.setVisibility(View.VISIBLE);
        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}