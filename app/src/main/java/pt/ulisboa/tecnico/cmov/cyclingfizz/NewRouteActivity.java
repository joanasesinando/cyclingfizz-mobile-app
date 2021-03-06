 package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    public final static String POI_INDEX = "pt.ulisboa.tecnico.cmov.cyclingfizz.POI_INDEX";
    static final int PICK_IMAGES = 1;
    static final int PICK_VIDEO = 2;

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

    private Bitmap image;

    private File videoFile;
    private String currentVideoPath;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_route);

        pathRecorder = PathRecorder.getInstance();

        setInputs();
        uiSetClickListeners();

        initMap(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == PICK_IMAGES && resultCode == RESULT_OK && data != null) {
                // Get URI
                Uri uri = data.getData();
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                Bitmap thumbImage = ThumbnailUtils.extractThumbnail(image, Utils.THUMBNAIL_SIZE_SMALL, Utils.THUMBNAIL_SIZE_SMALL);

                // Update view
                ImageView thumbnail = findViewById(R.id.route_thumbnail);
                thumbnail.setImageBitmap(thumbImage);

            } else if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null) {
                // Get URI
                Uri uri = data.getData();
                InputStream inputStream = getContentResolver().openInputStream(uri);

                videoFile = createVideoFile();
                if (videoFile != null) {
                    readInputStreamToVideoFile(inputStream);

                    Bitmap thumbnailVideo = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath(), MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);

                    // Update view
                    ImageView thumbnailVideoView = findViewById(R.id.route_video_thumbnail);
                    View routeVideoThumbnailLayout = findViewById(R.id.route_video_thumbnail_layout);
                    thumbnailVideoView.setImageBitmap(thumbnailVideo);
                    routeVideoThumbnailLayout.setVisibility(View.VISIBLE);

                    // set onClick
                    routeVideoThumbnailLayout.setOnClickListener(view -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri videoURI = FileProvider.getUriForFile(this,
                                "pt.ulisboa.tecnico.cmov.cyclingfizz.fileprovider",
                                videoFile);
                        intent.setDataAndType(videoURI, "video/*");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    });
                }


            } else {
                Toast.makeText(this, R.string.no_photos_selected, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private File createVideoFile() {

        try {
            // Create an image file name
            @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "Video_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File video = File.createTempFile(
                    fileName,  /* prefix */
                    ".mp4",         /* suffix */
                    storageDir      /* directory */
            );

            currentVideoPath = video.getAbsolutePath();
            return video;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    private void readInputStreamToVideoFile(InputStream inputStream) throws IOException {
        try (OutputStream output = new FileOutputStream(videoFile)) {
            byte[] buffer = new byte[4 * 1024]; // or other buffer size
            int read;

            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            output.flush();
        }
    }




    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
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

        // Set pick photos btn listener
        MaterialButton pickVideoBtn = findViewById(R.id.route_pick_video);
        pickVideoBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setType("video/*");
            startActivityForResult(intent, PICK_VIDEO);
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
                lineColor(getResources().getColor(R.color.purple_500)),
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
        nameInputLayout = findViewById(R.id.poi_name_input);
        descriptionInputLayout = findViewById(R.id.poi_description_input);

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
        ArrayList<PointOfInterest> pois = pathRecorder.getAllPOIs();

        // Init RecyclerView
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        RecyclerViewFragment fragment = new RecyclerViewFragment(pois, RecyclerViewFragment.DatasetType.POIS, true, null);
        transaction.replace(R.id.poi_list, fragment);
        transaction.commit();

        // Make POIs card visible if has POIs
        if (pois.size() > 0) {
            MaterialCardView poisLayout = findViewById(R.id.route_pois);
            poisLayout.setVisibility(View.VISIBLE);
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------------------- ROUTE ------------------ ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
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
            progressIndicator.setVisibility(View.VISIBLE);
            pathRecorder.saveRecording(name, description, image, videoFile, result -> {
                progressIndicator.setVisibility(View.GONE);
                finish();
            });
        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        setPOIs();
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