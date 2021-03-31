package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
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

enum TrackingMode {
    FREE, FOLLOW_USER
}

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    static String APP_NAME_DEBUGGER = "Cycling_Fizz";

//    static String MAP_SERVER_URL = "https://7a594aacc4e1.ngrok.io";
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

    public final static String STATION_NAME = "pt.ulisboa.tecnico.cmov.cyclingfizz.STATION_NAME";
    public final static String CYCLEWAY_NAME = "pt.ulisboa.tecnico.cmov.cyclingfizz.CYCLEWAY_NAME";

    private MapView mapView;
    private MapboxMap mapboxMap;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    startLocation();
                } else {
                    Toast.makeText(this, "Permission not Granted", Toast.LENGTH_SHORT).show();
                    trackingUser = false;
                }
            });

    static int REQUEST_CHECK_LOCATION_SETTINGS = 1;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private boolean trackingUser;
    private TrackingMode trackingMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_map);
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

            mapboxMap.addOnMapClickListener(point -> {

                PointF pointf = mapboxMap.getProjection().toScreenLocation(point);
                RectF rectF = new RectF(pointf.x - 10, pointf.y - 10, pointf.x + 10, pointf.y + 10);

                List<Feature> giraFeatureList = mapboxMap.queryRenderedFeatures(rectF, GIRA_STATION_LAYER_ID);
                List<Feature> cyclewaysFeatureList = mapboxMap.queryRenderedFeatures(rectF, CYCLEWAYS_LAYER_ID);

                // Open only one
                Feature feature = giraFeatureList.size() > 0 ? giraFeatureList.get(0) : null;
                Log.d("feature", String.valueOf(feature));

                if (feature != null) {
                    Log.d("Feature found with %1$s", feature.toJson());

                    Toast.makeText(MapActivity.this, "Id = " + feature.getProperty("id_expl").getAsString(),
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, StationActivity.class);
                    intent.putExtra(STATION_NAME, feature.getProperty("desig_comercial").getAsString());
                    startActivity(intent);

                } else {
                    feature = cyclewaysFeatureList.size() > 0 ? cyclewaysFeatureList.get(0) : null;
                    if (feature == null) return true;
                    String cyclewayName;
                    if (feature.getProperty("tags").getAsJsonObject().get("name") != null) {
                        cyclewayName = feature.getProperty("tags").getAsJsonObject().get("name").getAsString();
                    } else {
                        cyclewayName = "Cycleway with no name";
                    }

                    Intent intent = new Intent(this, CyclewayActivity.class);
                    intent.putExtra(CYCLEWAY_NAME, cyclewayName);
                    startActivity(intent);
                }
                overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                return true;
            });

            Runnable r = new Runnable() {
                public void run() {
                    while(true) {
                        Log.d("Zoom", String.valueOf(mapboxMap.getCameraPosition().zoom));
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread t = new Thread(r);
            t.start();

            setMapboxCameraFollowUser();
            startLocation();
            mapboxMap.getLocationComponent().addOnCameraTrackingChangedListener(new OnCameraTrackingChangedListener() {

                @Override
                public void onCameraTrackingDismissed() {
                    Log.d(APP_NAME_DEBUGGER, "onCameraTrackingDismissed");

                }

                @Override
                public void onCameraTrackingChanged(int currentMode) {
                    Log.d(APP_NAME_DEBUGGER, String.valueOf(currentMode));
                    if (currentMode == CameraMode.TRACKING) {
                        setMapboxCameraFollowUser();
                    } else {
                        setMapboxCameraFree();
                    }
                    updateCurrentLocationBtn();
                }
            });

            FloatingActionButton map_current_location_btn = findViewById(R.id.btn_map_current_location);
            map_current_location_btn.setOnClickListener(view -> {
                setMapboxCameraFollowUser();
                turnOnLocationTrackerMapbox();
            });

            updateCurrentLocationBtn();
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments

        });
    }

    private void updateCurrentLocationBtn() {
        FloatingActionButton map_current_location_btn = findViewById(R.id.btn_map_current_location);

        if (!trackingUser) {
            map_current_location_btn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.danger)));
        } else if (trackingMode == TrackingMode.FOLLOW_USER) {
            map_current_location_btn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.orange_500)));
        } else {
            map_current_location_btn.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.dark)));
        }
    }


    private void addIcons(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage(GIRA_ICON_ID, Objects.requireNonNull(BitmapUtils.getBitmapFromDrawable(
                getResources().getDrawable(R.drawable.ic_gira))));
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
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            Log.d(APP_NAME_DEBUGGER, "Location ON");
            turnOnLocationTrackerMapbox();
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
        trackingUser = true;
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission already Granted", Toast.LENGTH_SHORT).show();
            createLocationRequest();
            checkIfLocationOn();
        } else {
            Toast.makeText(this, "ask for permission", Toast.LENGTH_SHORT).show();
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void setMapboxCameraFollowUser() {
        this.trackingMode = TrackingMode.FOLLOW_USER;
        Log.d(APP_NAME_DEBUGGER, "set to " + trackingMode.name());
    }

    private void setMapboxCameraFree() {
        this.trackingMode = TrackingMode.FREE;
        Log.d(APP_NAME_DEBUGGER, "set to " + trackingMode.name());
    }

    private void updateMapboxCamera(LocationComponent locationComponent) {
        Log.d(APP_NAME_DEBUGGER, trackingMode.name());
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
            case FREE:
                break;
        }

    }


    @SuppressLint("MissingPermission")
    private void turnOnLocationTrackerMapbox() {
        Log.d(APP_NAME_DEBUGGER, "entra");
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
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }


    @SuppressLint("MissingPermission")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS) {
            if (resultCode == -1) {
                startLocation();
            } else {
                trackingUser = false;
                Toast.makeText(this, "Bem pelo menos tou aqui " + resultCode,
                        Toast.LENGTH_SHORT).show();
                updateCurrentLocationBtn();
                LocationComponent locationComponent = mapboxMap.getLocationComponent();
                if (locationComponent.isLocationComponentActivated() && locationComponent.isLocationComponentEnabled()) {
                    locationComponent.setLocationComponentEnabled(false);
                }
            }
        }
    }

//    public void askForGPS() {
//
//        new MaterialAlertDialogBuilder(this)
//                .setTitle(getString(R.string.user_location_turn_on_title))
//                .setMessage(getString(R.string.user_location_turn_on_text))
//                .setNegativeButton(getString(R.string.user_location_turn_on_setting), (dialogInterface, i) -> {
//                    Intent gpsOptionsIntent = new Intent(
//                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                    startActivityForResult(gpsOptionsIntent, 1);
//                })
//                .setPositiveButton(getString(R.string.user_location_turn_on_ignore), (dialogInterface, i) -> {
//
//                })
//                .show();
//
//    }




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