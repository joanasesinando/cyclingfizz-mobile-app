package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Messenger;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
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
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.layers.TransitionOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;

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

enum TrackingMode {
    FREE, FOLLOW_USER, FOLLOW_USER_WITH_BEARING
}

enum BikeLocking {
    UNLOCKED(0), LOCKED(1);

    private final int state;

    BikeLocking(int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }
}

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
        SimWifiP2pManager.PeerListListener, SimWifiP2PActivityListener {

    static String TAG = "Cycling_Fizz@MapActivity";

    SharedState sharedState;
    private FirebaseAuth mAuth;
    Sidebar sidebar;

    /// -------------- MAP RELATED -------------- ///

    String MAP_SERVER_URL = "https://map.cfservertest.ga";
    String STATIONS_SERVER_URL = "https://stations.cfservertest.ga";

    static String GIRA_SOURCE_ID = "gira-source";
    String GIRA_DATA_URL = MAP_SERVER_URL + "/get-gira";
    static String GIRA_ICON_ID = "gira-icon";
    static String GIRA_STATION_LAYER_ID = "gira-layer";
    static String GIRA_CLUSTER_LAYER_ID = "gira-cluster-layer";
    static String GIRA_COUNT_LAYER_ID = "gira-count-layer";

    static String CYCLEWAYS_SOURCE_ID = "cycleways-source";
    String CYCLEWAYS_DATA_URL = MAP_SERVER_URL + "/get-cycleways";
    static String CYCLEWAYS_LAYER_ID = "cycleways-layer";

    static String PATH_RECORDED_SOURCE_ID = "path-recorded-source";
    static String PATH_RECORDED_LAYER_ID = "path-recorded-layer";

    static Long LOCATION_UPDATE_INTERVAL = 1000L;
    static Long LOCATION_UPDATE_MAX_WAIT_INTERVAL = LOCATION_UPDATE_INTERVAL * 5;

    public final static String STATION_INFO = "pt.ulisboa.tecnico.cmov.cyclingfizz.STATION_INFO";
    public final static String CYCLEWAY_INFO = "pt.ulisboa.tecnico.cmov.cyclingfizz.CYCLEWAY_INFO";
    public final static String USER_LOCATION = "pt.ulisboa.tecnico.cmov.cyclingfizz.USER_LOCATION";

    private MapView mapView;
    private MapboxMap mapboxMap;
    private PathRecorder pathRecorder;

    /// -------------- PERMISSIONS -------------- ///

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    startLocation();  // afterPermissionGranted
                } else {
                    updateCurrentLocationBtn();
                    Toast.makeText(this, "Permission not Granted", Toast.LENGTH_SHORT).show();
                }
            });

    static int REQUEST_CHECK_LOCATION_SETTINGS = 1;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private Location userLocation;
    private TrackingMode trackingMode;

    /// -------------- WIFI DIRECT -------------- ///

    SimWifiP2pBroadcastReceiver mReceiver = null;
    static SimWifiP2pManager mManager = null;
    static SimWifiP2pManager.Channel mChannel = null;
    static boolean mBound = false;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        forceLightModeOn(); // FIXME: remove when dark mode implemented
        checkIfFirstTimeOpening();
        super.onCreate(savedInstanceState);

        sharedState = (SharedState) getApplicationContext();
        mAuth = FirebaseAuth.getInstance();

        checkIfRenting();

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        pathRecorder = PathRecorder.getInstance();

        setContentView(R.layout.map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Log.d(TAG, "Got Location");

                // Pass the new location to the Maps SDK's LocationComponent
                if (mapboxMap != null) {
                    userLocation = locationResult.getLastLocation();
                    mapboxMap.getLocationComponent().forceLocationUpdate(userLocation);
                }

                // Create thread waiting for path recording
                (new Thread(() -> {
                    if (pathRecorder.isRecording()) {
                        boolean pointAdded = pathRecorder.addPointToPath(Point.fromLngLat(userLocation.getLongitude(), userLocation.getLatitude()));
                        if (pointAdded) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addPathRecorded();
                                }
                            });
                        }
                    }
                })).start();
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                if (locationAvailability == null) return;
                if (!locationAvailability.isLocationAvailable()) checkIfLocationOn();
            }
        };

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Set layers btn click listener
        FloatingActionButton layersBtn = findViewById(R.id.btn_map_layers);
        layersBtn.setOnClickListener(this::changeLayerStyle);

        // Set sidebar
        sidebar = new Sidebar(this);

        // Set menu click listener for sidebar opening/closing
        MaterialToolbar toolbar = findViewById(R.id.map_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> sidebar.toggleSidebar(v));

        // Init Wifi Direct
        registerBroadcastReceiver();
        turnWifiOn();
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    private void forceLightModeOn() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void checkIfFirstTimeOpening() {
        // Set flag for first time opening
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        boolean firstOpen = sharedPref.getBoolean("firstOpenSaved", true);

        if (firstOpen) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("firstOpenSaved", false);
            editor.apply();

            // Redirect to the welcome creen
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }
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


    /*** -------------------------------------------- ***/
    /*** ------------------ MAPBOX ------------------ ***/
    /*** -------------------------------------------- ***/

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
            initPathRecordedLayer(style);

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
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(USER_LOCATION, userLocation);
                    intent.putExtras(bundle);
                    startActivity(intent);

                } else {
                    feature = cyclewaysFeatureList.size() > 0 ? cyclewaysFeatureList.get(0) : null;
                    if (feature == null) return true;

                    Intent intent = new Intent(this, CyclewayActivity.class);
                    intent.putExtra(CYCLEWAY_INFO, feature.toJson());
                    intent.putExtra(CYCLEWAY_INFO + ".point", (Point.fromLngLat(point.getLongitude(), point.getLatitude())).toJson());
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(USER_LOCATION, userLocation);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
                overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                return true;
            });

            setMapboxCameraFollowUser();
            startLocation();  // onMapReady

            mapboxMap.addOnCameraMoveListener(this::updateCompassBearing);
            mapView.addOnCameraIsChangingListener(this::updateCompassBearing);
            mapView.addOnCameraDidChangeListener(animated -> updateCompassBearing());

            mapboxMap.addOnMoveListener(new MapboxMap.OnMoveListener() {
                @Override
                public void onMoveBegin(@NonNull MoveGestureDetector detector) {
                }

                @Override
                public void onMove(@NonNull MoveGestureDetector detector) {
                    setMapboxCameraFree();
                    updateCurrentLocationBtn();
                }

                @Override
                public void onMoveEnd(@NonNull MoveGestureDetector detector) {
                }
            });

            FloatingActionButton currentLocationBtn = findViewById(R.id.btn_map_current_location);
            currentLocationBtn.setOnClickListener(view -> {
                switch (trackingMode) {
                    case FREE:
                    default:
                        setMapboxCameraFollowUser();
                        break;

                    case FOLLOW_USER:
                        setMapboxCameraFollowUserWithBearing();
                        break;

                    case FOLLOW_USER_WITH_BEARING:
                        setMapboxCameraFollowUser();
                        pointToNorth();
                        new Handler().postDelayed(this::startLocation , 800); // onLocBtnClick delayed
                        return;
                }
                startLocation(); // onLocBtnClick
            });

            FloatingActionButton bearingBtn = findViewById(R.id.btn_map_bearing);
            bearingBtn.setOnClickListener(view -> pointToNorth());

            MaterialToolbar toolbar = findViewById(R.id.map_toolbar).findViewById(R.id.topAppBar);
            toolbar.setOnMenuItemClickListener(item -> Toolbar.mapToolbarItemClicked(item, getApplicationContext(), mapboxMap));
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
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


    /*** -------------------------------------------- ***/
    /*** ---------------- LOCATION ------------------ ***/
    /*** -------------------------------------------- ***/

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

            Log.d(TAG, "Location ON");
            turnOnLocationTrackerMapbox();  // ifLocationOn
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                Log.d(TAG, "Location OFF " + e);

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

        updateCurrentLocationBtn();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (mapboxMap != null && location != null) {
                mapboxMap.getLocationComponent().forceLocationUpdate(location);
                userLocation = location;
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
                updateCurrentLocationBtn();
            }
        }
    }

    private boolean isGpsOn() {
        try {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            return locationComponent.isLocationComponentActivated() && locationComponent.isLocationComponentEnabled();
        } catch (Exception e) {
            Log.e(TAG + "_isGpsOn()", e.getMessage());
            return false;
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------------ CAMERA & CONTROLS ------------- ***/
    /*** -------------------------------------------- ***/

    private void changeLayerStyle(View view) {
        //TODO
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

    private void updateCurrentLocationBtn() {
        FloatingActionButton currentLocationBtn = findViewById(R.id.btn_map_current_location);

        if (!isGpsOn()) {
            // GPS off
            currentLocationBtn.setImageResource(R.drawable.ic_round_gps_off_24);
            currentLocationBtn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.danger)));
        } else if (trackingMode == TrackingMode.FOLLOW_USER) {
            // following_user
            currentLocationBtn.setImageResource(R.drawable.ic_round_gps_fixed_24);
            currentLocationBtn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.gps_blue)));
        } else if (trackingMode == TrackingMode.FOLLOW_USER_WITH_BEARING) {
            // following_user_with_bearing
            currentLocationBtn.setImageResource(R.drawable.ic_round_explore_24);
            currentLocationBtn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.gps_blue)));
        } else {
            // free
            currentLocationBtn.setImageResource(R.drawable.ic_round_gps_fixed_24);
            currentLocationBtn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.dark)));
        }
    }

    private void pointToNorth() {
        mapboxMap.resetNorth();
    }

    private void setMapboxCameraFollowUser() {
        this.trackingMode = TrackingMode.FOLLOW_USER;
    }

    private void setMapboxCameraFollowUserWithBearing() {
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
            default:
                break;
        }

    }


    /*** -------------------------------------------- ***/
    /*** ------------- RECORDING ROUTE -------------- ***/
    /*** -------------------------------------------- ***/

    private void recordNewRoute(View view) {
        // Start recording
        pathRecorder.startRecording();
        pathRecorder.setPreparingToRecord(false);
        setMapboxCameraFollowUserWithBearing();
        updateCurrentLocationBtn();
        updateMapboxCamera(mapboxMap.getLocationComponent());

        // Update view
        ExtendedFloatingActionButton recordBtn = findViewById(R.id.btn_map_record_route);
        recordBtn.setVisibility(View.GONE);
        FloatingActionButton cancelRecordingBtn = findViewById(R.id.btn_cancel_recording);
        cancelRecordingBtn.setVisibility(View.GONE);

        FloatingActionButton addPOIBtn = findViewById(R.id.btn_map_add_poi);
        addPOIBtn.setVisibility(View.VISIBLE);
        // TODO: add click listener

        FloatingActionButton stopRecordingBtn = findViewById(R.id.btn_map_stop_recording);
        stopRecordingBtn.setVisibility(View.VISIBLE);
        stopRecordingBtn.setOnClickListener(this::stopRecordingRoute);

        ExtendedFloatingActionButton recordingFlag = findViewById(R.id.flag_recording);
        recordingFlag.setVisibility(View.VISIBLE);
    }

    private void stopRecordingRoute(View view) {
        // Stop recording
        pathRecorder.stopRecording();
        cleanPathRecorded();
        setMapboxCameraFollowUser();
        updateCurrentLocationBtn();
        updateMapboxCamera(mapboxMap.getLocationComponent());
        pointToNorth();

        // Update view
        FloatingActionButton addPOIBtn = findViewById(R.id.btn_map_add_poi);
        addPOIBtn.setVisibility(View.GONE);

        FloatingActionButton stopRecordingBtn = findViewById(R.id.btn_map_stop_recording);
        stopRecordingBtn.setVisibility(View.GONE);

        ExtendedFloatingActionButton recordingFlag = findViewById(R.id.flag_recording);
        recordingFlag.setVisibility(View.GONE);

        // Show dialog for saving/deleting route
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.route_recorded)
                .setMessage(R.string.route_recorded_message)
                .setNeutralButton(R.string.delete, (dialog, which) -> {
                    // Respond to neutral button press
                })
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    // Respond to positive button press
                    //TODO: create dialog for saving route
                    pathRecorder.saveRecording();
                })
                .show();
    }

    private void initPathRecordedLayer(@NonNull Style style) {
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
    }

    private void addPathRecorded() {
        Log.d(TAG, "updating path recorded on map");
        GeoJsonSource pathRecordedSource = mapboxMap.getStyle().getSourceAs(PATH_RECORDED_SOURCE_ID);
        if (pathRecordedSource != null) {
            pathRecordedSource.setGeoJson(FeatureCollection.fromFeatures(
                    new Feature[] {Feature.fromGeometry(
                            LineString.fromLngLats(pathRecorder.getPath())
                    )}
            ));
        }
    }

    private void cleanPathRecorded() {
        Log.d(TAG, "cleaning path recorded on map");
        GeoJsonSource pathRecordedSource = mapboxMap.getStyle().getSourceAs(PATH_RECORDED_SOURCE_ID);
        if (pathRecordedSource != null) {
            pathRecordedSource.setGeoJson(FeatureCollection.fromFeatures(
                    new Feature[] {Feature.fromGeometry(
                            LineString.fromLngLats(new ArrayList<Point>())
                    )}
            ));
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------------------ RENTING ----------------- ***/
    /*** -------------------------------------------- ***/

    private void checkIfRenting() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                (new Utils.httpRequestJson(obj -> {

                    if (obj.get("status").getAsString().equals("success")) {
                        JsonObject data = obj.get("data").getAsJsonObject();

                        boolean renting = data.get("renting").getAsBoolean();
                        View rentingView = findViewById(R.id.renting_info);

                        if (renting) {
                            sharedState.setRenting(true);
                            rentingView.setVisibility(View.VISIBLE);

                            long rentTimestamp = data.get("rent_timestamp").getAsLong();
                            long timeElapsedInMilSeconds = System.currentTimeMillis() - rentTimestamp;

                            // Start timer
                            Chronometer rentChronometer = findViewById(R.id.time_counter);
                            rentChronometer.setBase(SystemClock.elapsedRealtime() - timeElapsedInMilSeconds);
                            rentChronometer.start();

                            // Set end ride btn listener
                            MaterialButton btnStop = findViewById(R.id.end_ride);
                            btnStop.setOnClickListener(v -> checkForStationsInRange());

                            // Set lock bike listener
                            MaterialButton btnLock = findViewById(R.id.lock_bike);
                            int bikeStatus = data.get("bike_status").getAsInt();
                            if (bikeStatus == BikeLocking.UNLOCKED.getState()) {
                                btnLock.setText(R.string.lock_bike);
                                btnLock.setIconResource(R.drawable.ic_round_lock_24);
                                findViewById(R.id.locked_status).setVisibility(View.GONE);
                                btnLock.setOnClickListener(this::lockBike);

                            } else if (bikeStatus == BikeLocking.LOCKED.getState()) {
                                btnLock.setText(R.string.unlock_bike);
                                btnLock.setIconResource(R.drawable.ic_round_lock_open_24);
                                findViewById(R.id.locked_status).setVisibility(View.VISIBLE);
                                btnLock.setOnClickListener(this::unlockBike);
                            }
                        } else {
                            sharedState.setRenting(false);
                            rentingView.setVisibility(View.GONE);
                        }

                    } else {
                        Log.e(TAG, "Could not get renting status");
                    }

                })).execute(STATIONS_SERVER_URL + "/get-rent-status?idToken=" + idToken);
            });
        }
    }

    private void endTrip(String stationID) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();
                (new Utils.httpRequestJson(obj -> {
                    if (obj.get("status").getAsString().equals("success")) {
                        sharedState.setRenting(false);

                        JsonObject data = obj.get("data").getAsJsonObject();
                        long tripTime = data.get("trip_time").getAsLong();
                        String originStationID = data.get("start_station_id").getAsString();
                        String destinationStationID = data.get("end_station_id").getAsString();
                        String originStationName = data.get("start_station_name").getAsString();
                        String destinationStationName = data.get("end_station_name").getAsString();

                        // Remove renting view
                        View rentingView = findViewById(R.id.renting_info);
                        rentingView.setVisibility(View.GONE);

                        // Inflate end trip dialog view
                        View customDialog = LayoutInflater.from(this)
                                .inflate(R.layout.end_trip_dialog, null, false);

                        // Update dialog view
                        Chronometer totalTime = customDialog.findViewById(R.id.end_trip_time_counter);
                        totalTime.setBase(SystemClock.elapsedRealtime() - tripTime);
                        uiUpdateCard(customDialog.findViewById(R.id.end_trip_origin), R.drawable.ic_end_trip_origin,
                                originStationID, originStationName);
                        uiUpdateCard(customDialog.findViewById(R.id.end_trip_destination), R.drawable.ic_end_trip_destination,
                                destinationStationID, destinationStationName);

                        // Show end trip dialog
                        new MaterialAlertDialogBuilder(this)
                            .setView(customDialog)
                            .setTitle(R.string.bike_delivered)
                            .setMessage(R.string.bike_delivered_message)
                            .setPositiveButton(R.string.ok, (dialog, which) -> {})
                            .show();

                    } else {
                        Toast.makeText(this, "Error: " + obj.get("msg").getAsString(), Toast.LENGTH_SHORT).show();
                    }
                })).execute(STATIONS_SERVER_URL + "/stop-trip?idToken=" + idToken + "&stationID=" + stationID);
            });
        }
    }

    private void lockBike(View view) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();
                (new Utils.httpRequestJson(obj -> {
                    if (obj.get("status").getAsString().equals("success")) {
                        MaterialButton btn = (MaterialButton) view;
                        btn.setText(R.string.unlock_bike);
                        findViewById(R.id.locked_status).setVisibility(View.VISIBLE);
                        btn.setIconResource(R.drawable.ic_round_lock_open_24);
                        btn.setOnClickListener(this::unlockBike);
                    } else {
                        Toast.makeText(this, "Error: " + obj.get("msg").getAsString(), Toast.LENGTH_SHORT).show();
                    }
                })).execute(STATIONS_SERVER_URL + "/lock-bike?idToken=" + idToken);
            });
        }
    }

    private void unlockBike(View view) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();
                (new Utils.httpRequestJson(obj -> {
                    if (obj.get("status").getAsString().equals("success")) {
                        MaterialButton btn = (MaterialButton) view;
                        btn.setText(R.string.lock_bike);
                        findViewById(R.id.locked_status).setVisibility(View.GONE);
                        btn.setIconResource(R.drawable.ic_round_lock_24);
                        btn.setOnClickListener(this::lockBike);
                    } else {
                        Toast.makeText(this, "Error: " + obj.get("msg").getAsString(), Toast.LENGTH_SHORT).show();
                    }
                })).execute(STATIONS_SERVER_URL + "/unlock-bike?idToken=" + idToken);
            });
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------- (WIFI DIRECT) STATIONS IN RANGE ---- ***/
    /*** -------------------------------------------- ***/

    private void registerBroadcastReceiver() {
        // Register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
        mReceiver = new SimWifiP2pBroadcastReceiver(this);
        registerReceiver(mReceiver, filter);
    }

    private void turnWifiOn() {
        Intent intent = new Intent(this, SimWifiP2pService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
    }

    private void turnWifiOff() { // FIXME: onde desligar?
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void checkForStationsInRange() {
        if (mBound) {
            mManager.requestPeers(mChannel, MapActivity.this);
        } else {
            Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        // callbacks for service binding, passed to bindService()

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mManager = new SimWifiP2pManager(new Messenger(service));
            mChannel = mManager.initialize(getApplication(), getMainLooper(), null);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mManager = null;
            mChannel = null;
            mBound = false;
        }
    };

    @Override
    public void onPeersAvailable(SimWifiP2pDeviceList peers) {
        Log.e("Cycling_Fizz_peers", "onPeersAvailable - map activity");
        boolean isClose = peers.getDeviceList().size() > 0;

        if (!isClose) {
            Toast.makeText(this, "No stations nearby", Toast.LENGTH_SHORT).show();
            return;
        }

        for (SimWifiP2pDevice device : peers.getDeviceList()) {
            // Get beacon ID
            String beaconName = device.deviceName;
            String beaconID = beaconName.contains("_") ? beaconName.split("_")[1] : beaconName;

            Toast.makeText(this, "Station " + beaconID + " is in range", Toast.LENGTH_SHORT).show();
            endTrip(beaconID);
            break;
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------------ ACTIVITY LIFECYCLE ------------ ***/
    /*** -------------------------------------------- ***/

    @Override
    protected void onStart() {
        super.onStart();

        sidebar.changeUserUI();
        mapView.onStart();

        if (pathRecorder.isPreparingToRecord()) {
            ExtendedFloatingActionButton recordBtn = findViewById(R.id.btn_map_record_route);
            recordBtn.setVisibility(View.VISIBLE);
            recordBtn.setOnClickListener(MapActivity.this::recordNewRoute);

            FloatingActionButton cancelRecordingBtn = findViewById(R.id.btn_cancel_recording);
            cancelRecordingBtn.setVisibility(View.VISIBLE);
            cancelRecordingBtn.setOnClickListener(v -> {
                pathRecorder.setPreparingToRecord(false);
                recordBtn.setVisibility(View.GONE);
                cancelRecordingBtn.setVisibility(View.GONE);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isGpsOn()) {
            startLocationUpdates();
        }
        sidebar.changeUserUI();
        checkIfRenting();
        mapView.onResume();
        registerBroadcastReceiver();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause - map activity");
        super.onPause();
        if (isGpsOn() && !pathRecorder.isRecording()) {
            stopLocationUpdates();
        }
        mapView.onPause();
        if (mReceiver != null ) {
            Log.d(TAG, "unregistering - map activity");
            unregisterReceiver(mReceiver);
        }
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