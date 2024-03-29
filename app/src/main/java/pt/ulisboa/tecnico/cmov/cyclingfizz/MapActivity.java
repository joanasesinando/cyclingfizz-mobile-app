package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
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
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
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
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.layers.TransitionOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

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
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static pt.ulisboa.tecnico.cmov.cyclingfizz.ViewPOIActivity.ROUTE_ID;

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

    String MAP_SERVER_URL = Utils.MAP_SERVER_URL;
    String STATIONS_SERVER_URL = Utils.STATIONS_SERVER_URL;

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

    static String POI_SOURCE_ID = "poi-source";
    static String POI_ICON_ID = "poi-icon";
    static String POI_LAYER_ID = "poi-layer";
    static String POI_CLUSTER_LAYER_ID = "poi-cluster-layer";
    static String POI_COUNT_LAYER_ID = "poi-count-layer";

    static Long LOCATION_UPDATE_INTERVAL_LOW = 1000L;
    static Long LOCATION_UPDATE_MAX_WAIT_INTERVAL_LOW = LOCATION_UPDATE_INTERVAL_LOW;

    static Long LOCATION_UPDATE_INTERVAL_HIGH = 15000L;
    static Long LOCATION_UPDATE_MAX_WAIT_INTERVAL_HIGH = LOCATION_UPDATE_INTERVAL_HIGH * 2;

    public final static String STATION_INFO = "pt.ulisboa.tecnico.cmov.cyclingfizz.STATION_INFO";
    public final static String CYCLEWAY_INFO = "pt.ulisboa.tecnico.cmov.cyclingfizz.CYCLEWAY_INFO";
    public final static String USER_LOCATION = "pt.ulisboa.tecnico.cmov.cyclingfizz.USER_LOCATION";
    public final static String POI_INDEX = "pt.ulisboa.tecnico.cmov.cyclingfizz.POI_INDEX";
    public final static String POI_LOCATION = "pt.ulisboa.tecnico.cmov.cyclingfizz.POI_LOCATION";
    public final static String ROUTE_ID = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_ID";

    private MapView mapView;
    private MapboxMap mapboxMap;
    private String mapStyle;

    private boolean cyclewaysVisible = true;
    private boolean giraStationsVisible = true;

    private PathRecorder pathRecorder;
    private PathPlayer pathPlayer;

    private boolean endTripFlag = false;

    TextToSpeech t2s;

    /// -------------- PERMISSIONS -------------- ///

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startLocation();  // afterPermissionGranted
                } else {
                    updateCurrentLocationBtn();
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
        Utils.forceLightModeOn();
        checkIfFirstTimeOpening();
        super.onCreate(savedInstanceState);

        sharedState = (SharedState) getApplicationContext();
        mAuth = FirebaseAuth.getInstance();
        Cache.getInstanceBigFiles(getApplicationContext());
        Cache.getInstanceSmallFiles(getApplicationContext());

        checkIfRenting();

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        pathRecorder = PathRecorder.getInstance();
        pathPlayer = PathPlayer.getInstance();

        t2s = new TextToSpeech(getApplicationContext(), status -> {
            if(status != TextToSpeech.ERROR) {
                t2s.setLanguage(Locale.US);
            }
        });

        setContentView(R.layout.map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location previousLocation = userLocation;

                // Pass the new location to the Maps SDK's LocationComponent
                if (mapboxMap != null) {
                    userLocation = locationResult.getLastLocation();
                    mapboxMap.getLocationComponent().forceLocationUpdate(userLocation);
                }

                // Create thread waiting for path recording
                (new Thread(() -> {
                    if (pathRecorder.isRecording()) {
                        boolean pointAdded = pathRecorder.addPointToPath(Point.fromLngLat(userLocation.getLongitude(), userLocation.getLatitude()));
                        if (pointAdded || pathRecorder.POIAdded()) {
                            updateRoute();
                            pathRecorder.setPOIAdded(false);
                        }
                    }
                })).start();

                (new Thread(() -> {
                    if (pathPlayer.isPlayingRoute()) {

                        Point userPoint = Point.fromLngLat(userLocation.getLongitude(), userLocation.getLatitude());

                        if (previousLocation != null && userLocation != null && Utils.distanceBetweenPointsInMeters(
                                Point.fromLngLat(previousLocation.getLongitude(), previousLocation.getLatitude()),
                                userPoint) < 2.5) return;

                        PointOfInterest poi = pathPlayer.checkIfNearPOI(userPoint);
                        if (poi != null) {
                            runOnUiThread(() -> {
                                speakPOI(poi);
                                new MaterialAlertDialogBuilder(MapActivity.this)
                                        .setTitle(poi.getName())
                                        .setMessage(poi.getDescription())
                                        .setNeutralButton(R.string.ignore, null)
                                        .setPositiveButton(R.string.view_more, (dialog, which) -> {
                                            SharedState sharedState = (SharedState) getApplicationContext();
                                            sharedState.viewingPOI = poi;

                                            Intent intent = new Intent(MapActivity.this, ViewPOIActivity.class);
                                            Bundle bundle = new Bundle();
                                            bundle.putString(ROUTE_ID, pathPlayer.getPlayingRoute().getId());
                                            intent.putExtras(bundle);
                                            startActivity(intent);
                                            overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                                        }).show();
                            });
                        }

                        if (pathPlayer.checkIfEnd(userPoint)) {
                            runOnUiThread(() -> {
                                speakEndRoute();
                                new MaterialAlertDialogBuilder(MapActivity.this)
                                        .setTitle(R.string.route_play_finished)
                                        .setMessage(R.string.route_play_finished_message)
                                        .setNeutralButton(R.string.no, null)
                                        .setPositiveButton(R.string.stop_route, (dialog, which) -> {
                                            if (!pathPlayer.isRouteAlreadyRated()) {
                                                 new MaterialAlertDialogBuilder(MapActivity.this)
                                                         .setTitle(R.string.ask_rate_route)
                                                         .setMessage(R.string.ask_rate_route_message)
                                                         .setNeutralButton(R.string.ignore, (dialogRate, whichRate) -> {
                                                             stopPlaying();
                                                         })
                                                         .setPositiveButton(R.string.rate_route, (dialogRate, whichRate) -> {
                                                             Intent intent = new Intent(MapActivity.this, RouteActivity.class);
                                                             intent.putExtra(ROUTE_ID, pathPlayer.getPlayingRoute().getId());
                                                             stopPlaying();
                                                             startActivity(intent);
                                                             overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                                                         }).show();
                                            } else {
                                                stopPlaying();
                                            }
                                        }).show();
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

        // Set sidebar
        sidebar = new Sidebar(this);

        // Set click listeners
        uiSetClickListeners();

        // Init Wifi Direct
        registerBroadcastReceiver();
        turnWifiOn();
    }


    private void speakPOI(PointOfInterest pointOfInterest) {
        if (t2s == null || t2s.isSpeaking()) return;
        t2s.speak("You just reached, " + pointOfInterest.getName(), TextToSpeech.QUEUE_FLUSH, null);
    }

    private void speakEndRoute() {
        if (t2s == null || t2s.isSpeaking()) return;
        t2s.speak("You just reached the end of the route!", TextToSpeech.QUEUE_FLUSH, null);
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    private void checkIfFirstTimeOpening() {
        // Set flag for first time opening
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        boolean firstOpen = sharedPref.getBoolean("firstOpenSaved", true);

        if (firstOpen) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("firstOpenSaved", false);
            editor.apply();

            // Redirect to the welcome screen
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void uiSetClickListeners() {
        // Set layers btn click listener
        FloatingActionButton layersBtn = findViewById(R.id.btn_map_layers);
        layersBtn.setOnClickListener(v -> changeLayerStyle(this.mapStyle.equals(Style.MAPBOX_STREETS) ?
                Style.SATELLITE_STREETS : Style.MAPBOX_STREETS));

        // Set menu click listener for sidebar opening/closing
        MaterialToolbar toolbar = findViewById(R.id.map_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> sidebar.toggleSidebar());
    }

    private boolean toolbarItemClicked(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.filter_cycleways) {
            filterItems("cycleways", !item.isChecked(), item);
            // Keep the popup menu open
            Utils.keepMenuOpen(item, this);

        } else if (id == R.id.filter_gira) {
            filterItems("gira-stations", !item.isChecked(), item);
            // Keep the popup menu open
            Utils.keepMenuOpen(item, this);
        }
        return false;
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

    private void showRecordingUI() {
        ExtendedFloatingActionButton recordBtn = findViewById(R.id.btn_map_record_route);
        recordBtn.setVisibility(View.GONE);
        FloatingActionButton cancelRecordingBtn = findViewById(R.id.btn_cancel_recording);
        cancelRecordingBtn.setVisibility(View.GONE);

        FloatingActionButton addPOIBtn = findViewById(R.id.btn_map_add_poi);
        addPOIBtn.setVisibility(View.VISIBLE);
        addPOIBtn.setOnClickListener(v -> addPOI());

        FloatingActionButton stopRecordingBtn = findViewById(R.id.btn_map_stop);
        stopRecordingBtn.setVisibility(View.VISIBLE);
        stopRecordingBtn.setOnClickListener(v -> stopRecordingRoute());

        ExtendedFloatingActionButton recordingFlag = findViewById(R.id.flag_recording);
        recordingFlag.setVisibility(View.VISIBLE);
    }


    /*** -------------------------------------------- ***/
    /*** ------------------ MAPBOX ------------------ ***/
    /*** -------------------------------------------- ***/

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        this.mapStyle = Style.MAPBOX_STREETS;

        mapboxMap.setStyle(this.mapStyle, style -> {
            style.setTransition(new TransitionOptions(0, 0, false));

            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                    38.722252, -9.139337), 13));

            addIcons(style);
            addCycleways(style);
            addGiraStations(style);
            initRouteLayer(style);

            mapboxMap.addOnMapClickListener(point -> {

                PointF pointf = mapboxMap.getProjection().toScreenLocation(point);
                RectF rectF = new RectF(pointf.x - 10, pointf.y - 10, pointf.x + 10, pointf.y + 10);

                List<Feature> giraFeatureList = mapboxMap.queryRenderedFeatures(rectF, GIRA_STATION_LAYER_ID);
                List<Feature> cyclewaysFeatureList = mapboxMap.queryRenderedFeatures(rectF, CYCLEWAYS_LAYER_ID);
                List<Feature> poisFeatureList = mapboxMap.queryRenderedFeatures(rectF, POI_LAYER_ID);

                // Open only one
                // Priority: POI > Gira > cycleway
                String itemSelected = "";
                Feature feature = null;
                if (poisFeatureList.size() > 0) {
                    itemSelected = "POI";
                    feature = poisFeatureList.get(0);

                } else if (giraFeatureList.size() > 0) {
                    itemSelected = "gira-station";
                    feature = giraFeatureList.get(0);

                } else if (cyclewaysFeatureList.size() > 0){
                    itemSelected = "cycleway";
                    feature = cyclewaysFeatureList.get(0);
                }

                Intent intent;
                Bundle bundle;
                switch (itemSelected) {
                    case "POI":
                        int poiIndex = feature.getNumberProperty("id").intValue();
                        if (pathRecorder.isRecording()) {
                            intent = new Intent(this, EditPOIActivity.class);
                            intent.putExtra(POI_INDEX, poiIndex);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);

                        } else if (pathPlayer.isPlayingRoute()) {
                            Route routePlaying = pathPlayer.getPlayingRoute();
                            PointOfInterest poi = routePlaying.getAllPOIs().get(poiIndex);
                            SharedState sharedState = (SharedState) getApplicationContext();
                            sharedState.viewingPOI = poi;

                            intent = new Intent(this, ViewPOIActivity.class);
                            bundle = new Bundle();
                            bundle.putString(ROUTE_ID, routePlaying.getId());
                            intent.putExtras(bundle);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                        }
                        break;

                    case "gira-station":
                        intent = new Intent(this, StationActivity.class);
                        intent.putExtra(STATION_INFO, feature.toJson());
                        bundle = new Bundle();
                        bundle.putParcelable(USER_LOCATION, userLocation);
                        intent.putExtras(bundle);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                        break;

                    case "cycleway":
                        intent = new Intent(this, CyclewayActivity.class);
                        intent.putExtra(CYCLEWAY_INFO, feature.toJson());
                        intent.putExtra(CYCLEWAY_INFO + ".point", (Point.fromLngLat(point.getLongitude(), point.getLatitude())).toJson());
                        bundle = new Bundle();
                        bundle.putParcelable(USER_LOCATION, userLocation);
                        intent.putExtras(bundle);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                        break;

                    default:
                        return true;
                }
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
            toolbar.setOnMenuItemClickListener(this::toolbarItemClicked);

            returnedToMap();
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void addIcons(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage(GIRA_ICON_ID, Objects.requireNonNull(BitmapUtils.getBitmapFromDrawable(
                getResources().getDrawable(R.drawable.ic_gira_marker))));

        loadedMapStyle.addImage(POI_ICON_ID, Objects.requireNonNull(BitmapUtils.getBitmapFromDrawable(
                getResources().getDrawable(R.drawable.ic_poi_marker))));
    }

    private void addGiraStations(@NonNull Style loadedMapStyle) {
        Log.i(TAG, "addGiraStations: ");
        try {
            loadedMapStyle.addSource(
                new GeoJsonSource(GIRA_SOURCE_ID,
                    new URI(GIRA_DATA_URL),
                    new GeoJsonOptions()
                        .withCluster(true)
                        .withClusterRadius(50)
                        .withClusterMaxZoom(14)
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

    private void filterItems(String type, boolean visible, MenuItem item) {
        try {
            switch (type) {
                case "cycleways":
                    Layer cyclewaysLayer = mapboxMap.getStyle().getLayer(MapActivity.CYCLEWAYS_LAYER_ID);
                    cyclewaysLayer.setProperties(visibility(visible ? Property.VISIBLE : Property.NONE));
                    item.setChecked(visible);
                    cyclewaysVisible = visible;
                    break;

                case "gira-stations":
                    Layer giraLayer = mapboxMap.getStyle().getLayer(MapActivity.GIRA_STATION_LAYER_ID);
                    Layer giraClustersLayer = mapboxMap.getStyle().getLayer(MapActivity.GIRA_CLUSTER_LAYER_ID);
                    Layer giraCountLayer = mapboxMap.getStyle().getLayer(MapActivity.GIRA_COUNT_LAYER_ID);

                    PropertyValue<String> visibility = visibility(visible ? Property.VISIBLE : Property.NONE);

                    giraLayer.setProperties(visibility);
                    giraClustersLayer.setProperties(visibility);
                    giraCountLayer.setProperties(visibility);
                    item.setChecked(visible);
                    giraStationsVisible = visible;
                    break;

                default:
            }
        } catch (NullPointerException ignored) { }
    }


    /*** -------------------------------------------- ***/
    /*** ---------------- LOCATION ------------------ ***/
    /*** -------------------------------------------- ***/

    protected void createLocationRequestHighInterval() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL_HIGH);
        locationRequest.setMaxWaitTime(LOCATION_UPDATE_MAX_WAIT_INTERVAL_HIGH);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void createLocationRequestSmallInterval() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL_LOW);
        locationRequest.setMaxWaitTime(LOCATION_UPDATE_MAX_WAIT_INTERVAL_LOW);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    void updateLocationRequestToHighInterval() {
        createLocationRequestHighInterval();
        restartLocationUpdates();
    }

    void updateLocationRequestToSmallInterval() {
        createLocationRequestSmallInterval();
        restartLocationUpdates();
    }

    void restartLocationUpdates() {
        stopLocationUpdates();
        startLocationUpdates();
    }

    void checkIfLocationOn() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
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
            if (locationRequest == null) createLocationRequestHighInterval();
            checkIfLocationOn();

        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void turnOnLocationTrackerMapbox() {
        // Get an instance of the component
        LocationComponent locationComponent = mapboxMap.getLocationComponent();

        if (mapboxMap == null || mapboxMap.getStyle() == null) return;

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
            if (mapboxMap != null && location != null && mapboxMap.getLocationComponent().isLocationComponentActivated()) {
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
            return false;
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------------ CAMERA & CONTROLS ------------- ***/
    /*** -------------------------------------------- ***/

    private void changeLayerStyle(String styleType) {
        this.mapStyle = styleType;
        mapboxMap.setStyle(styleType, style -> {
            addIcons(style);
            addCycleways(style);
            addGiraStations(style);
            initRouteLayer(style);

            // Keep filters the same as they were
            MaterialToolbar toolbar = findViewById(R.id.map_toolbar).findViewById(R.id.topAppBar);

            filterItems("cycleways", cyclewaysVisible, toolbar.getMenu().getItem(0).getSubMenu().getItem(0));
            filterItems("gira-stations", giraStationsVisible, toolbar.getMenu().getItem(0).getSubMenu().getItem(1));
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
        updateLocationRequestToSmallInterval();
        pathRecorder.setPreparingToRecord(false);
        setMapboxCameraFollowUserWithBearing();
        updateCurrentLocationBtn();
        updateMapboxCamera(mapboxMap.getLocationComponent());

        // Prevent screen from turning off while recording
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Update view
        showRecordingUI();
    }

    private void stopRecordingRoute() {
        // Stop recording
        pathRecorder.stopRecording();
        cleanRoute();
        setMapboxCameraFollowUser();
        updateCurrentLocationBtn();
        updateMapboxCamera(mapboxMap.getLocationComponent());
        updateLocationRequestToHighInterval();
        pointToNorth();

        // Allow screen to turn off
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Update view
        FloatingActionButton addPOIBtn = findViewById(R.id.btn_map_add_poi);
        addPOIBtn.setVisibility(View.GONE);

        FloatingActionButton stopRecordingBtn = findViewById(R.id.btn_map_stop);
        stopRecordingBtn.setVisibility(View.GONE);

        ExtendedFloatingActionButton recordingFlag = findViewById(R.id.flag_recording);
        recordingFlag.setVisibility(View.GONE);

        if (pathRecorder.getPath().size() < 2) {
            // Show dialog for short route
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.short_route)
                    .setMessage(R.string.short_route_warning)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        // Respond to positive button press
                        pathRecorder.cleanGeoJson();
                    })
                    .show();

        } else {
            // Show dialog for saving/deleting route
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.route_recorded)
                .setMessage(R.string.route_recorded_message)
                .setNeutralButton(R.string.delete, (dialog, which) -> {
                    // Respond to neutral button press
                    pathRecorder.cleanGeoJson();
                })
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    // Respond to positive button press
                    Intent intent = new Intent(this, NewRouteActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
                })
                .show();
        }
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

    private void updateRoute() {
        updatePathRecordedOnMap();
        updatePOIsOnMap();
    }

    private void updatePathRecordedOnMap() {
        if (mapboxMap == null || mapboxMap.getStyle() == null) return;

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

    private void updatePOIsOnMap() {
        if (mapboxMap == null || mapboxMap.getStyle() == null) return;

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

    private void cleanRoute() {
        // Cleaning path recorded
        GeoJsonSource pathRecordedSource = mapboxMap.getStyle().getSourceAs(PATH_RECORDED_SOURCE_ID);
        if (pathRecordedSource != null) {
            pathRecordedSource.setGeoJson(FeatureCollection.fromFeatures(
                    new Feature[] {Feature.fromGeometry(
                            LineString.fromLngLats(new ArrayList<Point>())
                    )}
            ));
        }

        // Cleaning POIs
        GeoJsonSource POIsSource = mapboxMap.getStyle().getSourceAs(POI_SOURCE_ID);
        if (POIsSource != null) {
            POIsSource.setGeoJson(FeatureCollection.fromFeatures(new Feature[] {}));
        }
    }

    private void addPOI() {
        Intent intent = new Intent(this, AddPOIActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(POI_LOCATION, userLocation);
        intent.putExtras(bundle);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
    }


    /*** -------------------------------------------- ***/
    /*** --------------- PLAYING ROUTE -------------- ***/
    /*** -------------------------------------------- ***/

    private void showPlayingRouteUI() {
        ExtendedFloatingActionButton playingFlag = findViewById(R.id.flag_playing);
        playingFlag.setVisibility(View.VISIBLE);

        FloatingActionButton stopBtn = findViewById(R.id.btn_map_stop);
        stopBtn.setVisibility(View.VISIBLE);
        stopBtn.setOnClickListener(v -> stopPlaying());
    }

    private void hidePlayingRouteUI() {
        ExtendedFloatingActionButton playingFlag = findViewById(R.id.flag_playing);
        playingFlag.setVisibility(View.GONE);

        FloatingActionButton stopBtn = findViewById(R.id.btn_map_stop);
        stopBtn.setVisibility(View.GONE);
    }

    private void showPlayingRouteOnMap() {
        cleanRoute();
        if (mapboxMap == null || mapboxMap.getStyle() == null) return;

        GeoJsonSource pathPlayingSource = mapboxMap.getStyle().getSourceAs(PATH_RECORDED_SOURCE_ID);
        if (pathPlayingSource != null) {
            FeatureCollection featureCollection = FeatureCollection.fromFeatures(
                    new Feature[] {pathPlayer.getPlayingRoute().getRouteFeature()}
            );
            runOnUiThread(() -> {
                pathPlayingSource.setGeoJson(featureCollection);
                zoomToRoute();
            });
        }
    }

    private void showPlayingPOIsOnMap() {

        if (mapboxMap == null || mapboxMap.getStyle() == null) return;

        GeoJsonSource POIsSource = mapboxMap.getStyle().getSourceAs(POI_SOURCE_ID);
        if (POIsSource != null) {
            ArrayList<Feature> features = new ArrayList<>();
            int i = 0;
            for (PointOfInterest poi : pathPlayer.getPlayingRoute().getAllPOIs()) {
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

    private void zoomToRoute() {
        setMapboxCameraFree();

        LatLngBounds latLngBounds;
        ArrayList<Point> path = pathPlayer.getPlayingRoute().getPath();

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
    }

    private void stopPlaying() {
        // Allow screen to turn off
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateLocationRequestToHighInterval();

        cleanRoute();
        pathPlayer.stopRoute();
        hidePlayingRouteUI();
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
                    if (obj == null) return;

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
                            btnStop.setOnClickListener(v -> {
                                endTripFlag = true;
                                checkForStationsInRange();
                            });

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
        boolean isClose = peers.getDeviceList().size() > 0;

        MaterialButton btnStop = findViewById(R.id.end_ride);
        btnStop.setEnabled(isClose);
        btnStop.setAlpha(isClose ? 1f : 0.5f);

        if (!isClose) {
            endTripFlag = false;
            return;
        }


        for (SimWifiP2pDevice device : peers.getDeviceList()) {
            // Get beacon ID
            String beaconName = device.deviceName;
            String beaconID = beaconName.contains("_") ? beaconName.split("_")[1] : beaconName;

            if (endTripFlag)
                endTrip(beaconID);
            break;
        }

        endTripFlag = false;
    }


    /*** -------------------------------------------- ***/
    /*** ------------ ACTIVITY LIFECYCLE ------------ ***/
    /*** -------------------------------------------- ***/

    private void returnedToMap() {
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

        if (pathRecorder.isRecording()) {
            showRecordingUI();
            updateRoute();
        }

        if (pathPlayer.isPlayingRoute()) {
            if (locationRequest.getInterval() == LOCATION_UPDATE_INTERVAL_HIGH) {
                updateLocationRequestToSmallInterval();
            }
            showPlayingRouteUI();
            showPlayingRouteOnMap();
            showPlayingPOIsOnMap();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        sidebar.changeUserUI();
        mapView.onStart();
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
    protected void onRestart() {
        super.onRestart();
        returnedToMap();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isGpsOn() && !pathRecorder.isRecording()) {
            stopLocationUpdates();
        }
        mapView.onPause();
        if (mReceiver != null ) {
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