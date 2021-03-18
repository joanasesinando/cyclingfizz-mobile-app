package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
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

import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.has;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.not;
import static com.mapbox.mapboxsdk.style.expressions.Expression.step;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
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
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

    static String MAP_SERVER_URL = "https://7a594aacc4e1.ngrok.io";
//    static String MAP_SERVER_URL = "https://map.server.cyclingfizz.pt";

    static String GIRA_SOURCE_ID = "gira-source";
    static String GIRA_DATA_URL = MAP_SERVER_URL + "/get-gira";
    static String GIRA_ICON_ID = "gira-icon";
    static String GIRA_STATION_LAYER_ID = "gira-layer";
    static String GIRA_CLUSTER_LAYER_ID = "gira-cluster-layer";
    static String GIRA_COUNT_LAYER_ID = "gira-count-layer";

    static String MOBI_CASCAIS_SOURCE_ID = "mobi-cascais-source";
    static String MOBI_CASCAIS_DATA_URL = MAP_SERVER_URL + "/get-mobi-cascais";
    static String MOBI_CASCAIS_ICON_ID = "mobi-cascais-icon";
    static String MOBI_CASCAIS_STATION_LAYER_ID = "mobi-cascais-layer";
    static String MOBI_CASCAIS_CLUSTER_LAYER_ID = "mobi-cascais-cluster-layer";
    static String MOBI_CASCAIS_COUNT_LAYER_ID = "mobi-cascais-count-layer";

    static String CYCLEWAYS_SOURCE_ID = "cycleways-source";
    static String CYCLEWAYS_DATA_URL = MAP_SERVER_URL + "/get-cycleways";
//    static String CYCLEWAYS_ICON_ID = "mobi-cascais-icon";
    static String CYCLEWAYS_LAYER_ID = "cycleways-layer";
//    static String MOBI_CASCAIS_CLUSTER_LAYER_ID = "mobi-cascais-cluster-layer";
//    static String MOBI_CASCAIS_COUNT_LAYER_ID = "mobi-cascais-count-layer";


    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationChangeListeningActivityLocationCallback callback =
            new LocationChangeListeningActivityLocationCallback(this);


    private MapView mapView;
    private MapboxMap mapboxMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_map);



        mapView = (MapView) findViewById(R.id.mapView);
        System.out.println("MY_log: " + mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            style.setTransition(new TransitionOptions(0, 0, false));

            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                    38.722252, -9.139337), 13));


            addIcons(style);
            addCycleways(style);
            addGiraStations(style);
            addMobiCascaisStations(style);


            mapboxMap.addOnMapClickListener(point -> {

                PointF pointf = mapboxMap.getProjection().toScreenLocation(point);
                RectF rectF = new RectF(pointf.x - 10, pointf.y - 10, pointf.x + 10, pointf.y + 10);

                List<Feature> giraFeatureList = mapboxMap.queryRenderedFeatures(rectF, GIRA_STATION_LAYER_ID);


                for (Feature feature : giraFeatureList) {
                    Log.d("Feature found with %1$s", feature.toJson());

                    Toast.makeText(MapActivity.this, "Id = " + feature.getProperty("id_expl").getAsString(),
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            enableLocationComponent(style);
            mapboxMap.getLocationComponent().addOnCameraTrackingChangedListener(new OnCameraTrackingChangedListener() {
                @Override
                public void onCameraTrackingDismissed() {

                }

                @Override
                public void onCameraTrackingChanged(int currentMode) {
                    updateCurrentLocationBtnColor();
                }
            });

            FloatingActionButton map_current_location_btn = findViewById(R.id.btn_map_current_location);
            map_current_location_btn.setOnClickListener(view -> this.enableLocationComponent(style));

            updateCurrentLocationBtnColor();
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments

        });
    }

    private void updateCurrentLocationBtnColor() {
        FloatingActionButton map_current_location_btn = findViewById(R.id.btn_map_current_location);

        if (mapboxMap.getLocationComponent().getCameraMode() == CameraMode.TRACKING) {
            map_current_location_btn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.orange_500)));
        } else {
            map_current_location_btn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.dark)));
        }
    }


    private void addIcons(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage(GIRA_ICON_ID, Objects.requireNonNull(BitmapUtils.getBitmapFromDrawable(
                getResources().getDrawable(R.drawable.ic_gira_icon))));

        loadedMapStyle.addImage(MOBI_CASCAIS_ICON_ID, BitmapUtils.getBitmapFromDrawable(
                getResources().getDrawable(R.drawable.ic_mobi_cascais_icon)));
    }


    private void addGiraStations(@NonNull Style loadedMapStyle) {

        try {
            loadedMapStyle.addSource(
                    new GeoJsonSource(GIRA_SOURCE_ID,
                            new URI(GIRA_DATA_URL),
                            new GeoJsonOptions()
                                    .withCluster(true)
                                    .withClusterMaxZoom(14)
                                    .withClusterRadius(50)
                    )
            );
        } catch (URISyntaxException uriSyntaxException) {
            System.err.println("Check the URL " + uriSyntaxException.getMessage());
        }

        //Creating a marker layer for single data points
        SymbolLayer unclustered = new SymbolLayer(GIRA_STATION_LAYER_ID, GIRA_SOURCE_ID);


        unclustered.setProperties(
                iconImage(GIRA_ICON_ID),
                iconSize(literal(1.5f))
        );

        unclustered.setFilter(not(has("point_count")));

        loadedMapStyle.addLayer(unclustered);


        CircleLayer circles = new CircleLayer(GIRA_CLUSTER_LAYER_ID, GIRA_SOURCE_ID);

        //Add clusters' circles
        circles.setProperties(
                circleColor(getResources().getColor(R.color.gira_green)),
                circleRadius(step(get("point_count"), 25,
                        stop(5, 35),
                        stop(10, 45),
                        stop(20, 55))),
                circleOpacity(0.7f)
        );

        circles.setFilter(has("point_count"));

        loadedMapStyle.addLayer(circles);


        //Add the count labels
        SymbolLayer count = new SymbolLayer(GIRA_COUNT_LAYER_ID, GIRA_SOURCE_ID);
        count.setProperties(
                textField(Expression.toString(get("point_count"))),
                textSize(12f),
                textColor(Color.BLACK),
                textIgnorePlacement(true),
                textAllowOverlap(true)
        );
        loadedMapStyle.addLayer(count);
    }


    private void addMobiCascaisStations(@NonNull Style loadedMapStyle) {

        try {
            loadedMapStyle.addSource(
                    new GeoJsonSource(MOBI_CASCAIS_SOURCE_ID,
                            new URI(MOBI_CASCAIS_DATA_URL),
                            new GeoJsonOptions()
                                    .withCluster(true)
                                    .withClusterMaxZoom(14)
                                    .withClusterRadius(50)
                    )
            );
        } catch (URISyntaxException uriSyntaxException) {
            System.err.println("Check the URL " + uriSyntaxException.getMessage());
        }

        //Creating a marker layer for single data points
        SymbolLayer unclustered = new SymbolLayer(MOBI_CASCAIS_STATION_LAYER_ID, MOBI_CASCAIS_SOURCE_ID);


        unclustered.setProperties(
                iconImage(MOBI_CASCAIS_ICON_ID),
                iconSize(literal(1.5f))
        );

        unclustered.setFilter(not(has("point_count")));

        loadedMapStyle.addLayer(unclustered);


        CircleLayer circles = new CircleLayer(MOBI_CASCAIS_CLUSTER_LAYER_ID, MOBI_CASCAIS_SOURCE_ID);

        //Add clusters' circles
        circles.setProperties(
                circleColor(getResources().getColor(R.color.mobi_cascais_blue)),
                circleRadius(step(get("point_count"), 25,
                        stop(5, 35),
                        stop(10, 45),
                        stop(20, 55))),
                circleOpacity(0.7f)
        );

        circles.setFilter(has("point_count"));

        loadedMapStyle.addLayer(circles);


        //Add the count labels
        SymbolLayer count = new SymbolLayer(MOBI_CASCAIS_COUNT_LAYER_ID, MOBI_CASCAIS_SOURCE_ID);
        count.setProperties(
                textField(Expression.toString(get("point_count"))),
                textSize(12f),
                textColor(Color.BLACK),
                textIgnorePlacement(true),
                textAllowOverlap(true)
        );
        loadedMapStyle.addLayer(count);
    }


    private void addCycleways(@NonNull Style loadedMapStyle) {

        try {
            loadedMapStyle.addSource(
                    new GeoJsonSource(CYCLEWAYS_SOURCE_ID,
                            new URI(CYCLEWAYS_DATA_URL),
                            new GeoJsonOptions())
            );
        } catch (URISyntaxException uriSyntaxException) {
            System.err.println("Check the URL " + uriSyntaxException.getMessage());
        }

        //Creating a marker layer for single data points
        LineLayer cycleways = new LineLayer(CYCLEWAYS_LAYER_ID, CYCLEWAYS_SOURCE_ID);

        cycleways.setProperties(
                lineJoin(Property.LINE_JOIN_ROUND),
                lineCap(Property.LINE_CAP_ROUND),
                lineColor("#d99b15"),
                lineWidth(5f),
                lineOpacity(0.5f)
        );

        loadedMapStyle.addLayer(cycleways);
    }



    /**
     * Initialize the Maps SDK's LocationComponent
     */
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING, 750L /*duration*/,
                    16.0 /*zoom*/,
                    null /*bearing, use current/determine based on the tracking mode*/,
                    null /*tilt*/,
                    null /*transition listener*/);

            locationComponent.zoomWhileTracking(16.0);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(this::enableLocationComponent);
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
        }
    }

    private static class LocationChangeListeningActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MapActivity> activityWeakReference;

        LocationChangeListeningActivityLocationCallback(MapActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            MapActivity activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }

                // Create a Toast which displays the new location's coordinates
//                Toast.makeText(activity, String.format(
//                        String.valueOf(result.getLastLocation().getLatitude()),
//                        String.valueOf(result.getLastLocation().getLongitude())),
//                        Toast.LENGTH_SHORT).show();

                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            MapActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
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
}