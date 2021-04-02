package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.layers.TransitionOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

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
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

enum TrackingMode {
    FREE, FOLLOW_USER, FOLLOW_USER_WITH_BEARING
}

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    static String APP_NAME_DEBUGGER = "Cycling_Fizz@MapActivity";

//    static String MAP_SERVER_URL = "https://ff58c3b926c9.ngrok.io";
    static String MAP_SERVER_URL = "https://map.server.cyclingfizz.pt";

    static String GIRA_SOURCE_ID = "gira-source";
    static String GIRA_DATA_URL = MAP_SERVER_URL + "/get-gira";
    static String GIRA_ICON_ID = "gira-icon";
    static String GIRA_STATION_LAYER_ID = "gira-layer";
    static String GIRA_CLUSTER_LAYER_ID = "gira-cluster-layer";
    static String GIRA_COUNT_LAYER_ID = "gira-count-layer";

    static String CYCLEWAYS_SOURCE_ID = "cycleways-source";
    static String CYCLEWAYS_DATA_URL = MAP_SERVER_URL + "/get-cycleways";
    static String CYCLEWAYS_LAYER_ID = "cycleways-layer";

    static Long LOCATION_UPDATE_INTERVAL = 1000L;
    static Long LOCATION_UPDATE_MAX_WAIT_INTERVAL = LOCATION_UPDATE_INTERVAL * 5;

    public final static String STATION_INFO = "pt.ulisboa.tecnico.cmov.cyclingfizz.STATION_INFO";
    public final static String CYCLEWAY_INFO = "pt.ulisboa.tecnico.cmov.cyclingfizz.CYCLEWAY_INFO";

    private MapView mapView;
    private MapboxMap mapboxMap;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    startLocation();  // afterPermissionGranted
                } else {
                    updateCurrentLocationBtn("PermissionNotGranted");
                    Toast.makeText(this, "Permission not Granted", Toast.LENGTH_SHORT).show();
                }
            });

    static int REQUEST_CHECK_LOCATION_SETTINGS = 1;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;

    private TrackingMode trackingMode;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(APP_NAME_DEBUGGER, "Called");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Log.d(APP_NAME_DEBUGGER, "Click");

        int id = item.getItemId();
        if (id == R.id.filter) {
            Log.d(APP_NAME_DEBUGGER, "Click Filter");
            try {
                Layer cyclewaysLayer = mapboxMap.getStyle().getLayer(CYCLEWAYS_LAYER_ID);

                Log.d(APP_NAME_DEBUGGER, String.valueOf(cyclewaysLayer.getVisibility().getValue()));
                Log.d(APP_NAME_DEBUGGER, String.valueOf(cyclewaysLayer.getVisibility().getValue().equals(Property.VISIBLE)));

                cyclewaysLayer.setProperties(visibility(cyclewaysLayer.getVisibility().getValue().equals(Property.VISIBLE) ? Property.NONE : Property.VISIBLE));
            } catch (NullPointerException ignored) {
                Log.e(APP_NAME_DEBUGGER, ignored.getMessage());
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void checkFirstOpen() {
        // Set light mode on for now
        // FIXME: remove when dark mode implemented
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        boolean firstOpen = sharedPref.getBoolean("firstOpenSaved", true);

        if (firstOpen) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("firstOpenSaved", false);
            editor.apply();

            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkFirstOpen();
        Log.d(APP_NAME_DEBUGGER, "passou pah");

        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        setContentView(R.layout.map);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Log.d(APP_NAME_DEBUGGER, "Got Location");

                // Pass the new location to the Maps SDK's LocationComponent
                if (mapboxMap != null && locationResult.getLastLocation() != null) {
                    mapboxMap.getLocationComponent().forceLocationUpdate(locationResult.getLastLocation());
                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                if (locationAvailability == null) {
                    return;
                }

                if (!locationAvailability.isLocationAvailable()) {
                    checkIfLocationOn();
                }
            }
        };

        mapView = (MapView) findViewById(R.id.mapView);
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

            mapboxMap.addOnMapClickListener(point -> {

                PointF pointf = mapboxMap.getProjection().toScreenLocation(point);
                RectF rectF = new RectF(pointf.x - 10, pointf.y - 10, pointf.x + 10, pointf.y + 10);

                List<Feature> giraFeatureList = mapboxMap.queryRenderedFeatures(rectF, GIRA_STATION_LAYER_ID);
                List<Feature> cyclewaysFeatureList = mapboxMap.queryRenderedFeatures(rectF, CYCLEWAYS_LAYER_ID);

                // Open only one
                Feature feature = giraFeatureList.size() > 0 ? giraFeatureList.get(0) : null;

                if (feature != null) {
                    Intent intent = new Intent(this, StationActivity.class);
                    intent.putExtra(STATION_INFO, feature.toJson());
                    startActivity(intent);

                } else {
                    feature = cyclewaysFeatureList.size() > 0 ? cyclewaysFeatureList.get(0) : null;
                    if (feature == null) return true;

                    Intent intent = new Intent(this, CyclewayActivity.class);
                    intent.putExtra(CYCLEWAY_INFO, feature.toJson());
                    intent.putExtra(CYCLEWAY_INFO + ".point", (Point.fromLngLat(point.getLongitude(), point.getLatitude())).toJson());
                    startActivity(intent);
                }
                overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                return true;
            });

            setMapboxCameraFollowUser();
            startLocation();  // onMapReady

            mapboxMap.addOnCameraMoveListener(() -> {
                updateCompassBearing();
//                Log.d(APP_NAME_DEBUGGER + "_Camera", "move");
            });

            mapView.addOnCameraIsChangingListener(() -> updateCompassBearing());

            mapView.addOnCameraDidChangeListener(animated -> updateCompassBearing());

            mapboxMap.addOnMoveListener(new MapboxMap.OnMoveListener() {
                @Override
                public void onMoveBegin(@NonNull MoveGestureDetector detector) {

                }

                @Override
                public void onMove(@NonNull MoveGestureDetector detector) {
                    setMapboxCameraFree();
                    updateCurrentLocationBtn("onCameraTrackingChanged");
                }

                @Override
                public void onMoveEnd(@NonNull MoveGestureDetector detector) {

                }

            });


            FloatingActionButton map_current_location_btn = findViewById(R.id.btn_map_current_location);

            map_current_location_btn.setOnClickListener(view -> {

                switch (trackingMode) {
                    case FREE:
                    default:
                        setMapboxCameraFollowUser();
                        break;
                    case FOLLOW_USER:
                        setMapboxCameraFollowUserWithHeading();
                        break;
                    case FOLLOW_USER_WITH_BEARING:
                        setMapboxCameraFollowUser();
                        pointToNorth();
                        new Handler().postDelayed(this::startLocation , 800); // onLocBtnClick delayed
                        return;
                }

                startLocation(); // onLocBtnClick
            });

            FloatingActionButton btn_map_bearing = findViewById(R.id.btn_map_bearing);

            btn_map_bearing.setOnClickListener(view -> {

               pointToNorth();
            });





            // Map is set up and the style has loaded. Now you can add data or make other map adjustments

        });
    }



    Handler hideCompassBtn = new Handler();

    private void updateCompassBearing() {
        long bearing = Math.round(mapboxMap.getCameraPosition().bearing) % 360;
        FloatingActionButton btn_map_bearing = findViewById(R.id.btn_map_bearing);


        if (bearing == 0) {
            btn_map_bearing.setRotation(0f);
            btn_map_bearing.setImageResource(R.drawable.ic_north);

            hideCompassBtn.postDelayed(() -> btn_map_bearing.setVisibility(View.GONE), 4000);

        } else {
            hideCompassBtn.removeCallbacksAndMessages(null);

            btn_map_bearing.setVisibility(View.VISIBLE);
            btn_map_bearing.setImageResource(R.drawable.ic_compass);
            btn_map_bearing.setRotation((float) - bearing);
        }

    }

    private void updateCurrentLocationBtn(String debugger) {
//        Log.d(APP_NAME_DEBUGGER, "Update Btn " + debugger);
        FloatingActionButton map_current_location_btn = findViewById(R.id.btn_map_current_location);

        if (!isGpsOn()) {
            // GPS off
            map_current_location_btn.setImageResource(R.drawable.ic_round_gps_off_24);
            map_current_location_btn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.danger)));
        } else if (trackingMode == TrackingMode.FOLLOW_USER) {
            // following_user
            map_current_location_btn.setImageResource(R.drawable.ic_round_gps_fixed_24);
            map_current_location_btn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.gps_blue)));
        } else if (trackingMode == TrackingMode.FOLLOW_USER_WITH_BEARING) {
            // following_user
            map_current_location_btn.setImageResource(R.drawable.ic_round_explore_24);
            map_current_location_btn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.gps_blue)));
        } else {
            // free
            map_current_location_btn.setImageResource(R.drawable.ic_round_gps_fixed_24);
            map_current_location_btn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.dark)));
        }
    }


    private void addIcons(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage(GIRA_ICON_ID, Objects.requireNonNull(BitmapUtils.getBitmapFromDrawable(
                getResources().getDrawable(R.drawable.ic_gira_marker))));
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
                iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconSize(literal(.5f))
        );

        unclustered.setFilter(not(has("point_count")));
        loadedMapStyle.addLayer(unclustered);

        CircleLayer circles = new CircleLayer(GIRA_CLUSTER_LAYER_ID, GIRA_SOURCE_ID);

        //Add clusters' circles
        circles.setProperties(
                circleColor(getResources().getColor(R.color.gira)),
                circleRadius(step(get("point_count"), 25,
                        stop(5, 35),
                        stop(10, 45),
                        stop(20, 55))),
                circleOpacity(0.8f)
        );
        circles.setFilter(has("point_count"));
        loadedMapStyle.addLayer(circles);

        //Add the count labels
        SymbolLayer count = new SymbolLayer(GIRA_COUNT_LAYER_ID, GIRA_SOURCE_ID);
        count.setProperties(
                textField(Expression.toString(get("point_count"))),
                textSize(12f),
                textColor(getResources().getColor(R.color.white)),
                textIgnorePlacement(true),
                textAllowOverlap(true),
                textFont(Expression.literal(R.font.quicksand_bold))
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
                lineColor(getResources().getColor(R.color.pink)),
                lineWidth(5f),
                lineOpacity(.8f)
        );

        loadedMapStyle.addLayer(cycleways);
    }


    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setMaxWaitTime(LOCATION_UPDATE_MAX_WAIT_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    void checkIfLocationOn() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {

            Log.d(APP_NAME_DEBUGGER, "Location ON");
            turnOnLocationTrackerMapbox();  // ifLocationOn
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MapActivity.this,
                            REQUEST_CHECK_LOCATION_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });

    }

    private void startLocation() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission already Granted", Toast.LENGTH_SHORT).show();

            if (locationRequest == null) createLocationRequest();
            checkIfLocationOn();

        } else {
            Toast.makeText(this, "ask for permission", Toast.LENGTH_SHORT).show();
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void pointToNorth() {
        mapboxMap.resetNorth();
    }

    private void setMapboxCameraFollowUser() {
        this.trackingMode = TrackingMode.FOLLOW_USER;
    }

    private void setMapboxCameraFollowUserWithHeading() {
        this.trackingMode = TrackingMode.FOLLOW_USER_WITH_BEARING;
    }

    private void setMapboxCameraFree() {
        this.trackingMode = TrackingMode.FREE;
    }

    private void updateMapboxCamera(LocationComponent locationComponent) {
        switch (trackingMode) {
            case FOLLOW_USER:
                // Set the component's camera mode
                locationComponent.setCameraMode(CameraMode.TRACKING, 750L /*duration*/,
                        16.0 /*zoom*/,
                        null /*bearing, use current/determine based on the tracking mode*/,
                        null /*tilt*/,
                        null /*transition listener*/);

                locationComponent.zoomWhileTracking(16.0);

                // Set the component's render mode
                locationComponent.setRenderMode(RenderMode.COMPASS);
                break;
            case FOLLOW_USER_WITH_BEARING:
                // Set the component's camera mode
                locationComponent.setCameraMode(CameraMode.TRACKING_COMPASS	, 750L /*duration*/,
                        16.0 /*zoom*/,
                        null /*bearing, use current/determine based on the tracking mode*/,
                        null /*tilt*/,
                        null /*transition listener*/);

                locationComponent.zoomWhileTracking(16.0);

                // Set the component's render mode
                locationComponent.setRenderMode(RenderMode.COMPASS);
                break;
            case FREE:
                break;
        }

    }


    @SuppressLint("MissingPermission")
    private void turnOnLocationTrackerMapbox() {
        // Get an instance of the component
        LocationComponent locationComponent = mapboxMap.getLocationComponent();

        // Set the LocationComponent activation options
        LocationComponentActivationOptions locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, mapboxMap.getStyle())
                        .useDefaultLocationEngine(false)
                        .build();

        // Activate with the LocationComponentActivationOptions object
        locationComponent.activateLocationComponent(locationComponentActivationOptions);

        // Enable to make component visible
        locationComponent.setLocationComponentEnabled(true);

        updateMapboxCamera(locationComponent);

        // start location updates
        startLocationUpdates();

        updateCurrentLocationBtn("turnOnLocationTrackerMapbox");
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (mapboxMap != null && location != null) {
                mapboxMap.getLocationComponent().forceLocationUpdate(location);
            }
        });
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }


    @SuppressLint("MissingPermission")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS) {
            if (resultCode == -1) {
                startLocation();  // afterGpsEnabled
            } else {
                LocationComponent locationComponent = mapboxMap.getLocationComponent();
                if (locationComponent.isLocationComponentActivated() && locationComponent.isLocationComponentEnabled()) {
                    locationComponent.setLocationComponentEnabled(false);
                }
                updateCurrentLocationBtn("onActivityResult");
            }
        }
    }

    private boolean isGpsOn() {
        try {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            return locationComponent.isLocationComponentActivated() && locationComponent.isLocationComponentEnabled();
        } catch (Exception e) {
            Log.e(APP_NAME_DEBUGGER + "_isGpsOn()", e.getMessage());
            return false;
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
        if (isGpsOn()) {
            startLocationUpdates();
        }
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isGpsOn()) {
            stopLocationUpdates();
        }
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