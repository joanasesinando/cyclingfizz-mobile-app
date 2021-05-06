package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
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

import java.text.DecimalFormat;
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

public class RouteActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@RouteActivity";
    static String SERVER_URL = "https://stations.cfservertest.ga";

    static String PATH_RECORDED_SOURCE_ID = "path-recorded-source";
    static String PATH_RECORDED_LAYER_ID = "path-recorded-layer";

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route);

        mAuth = FirebaseAuth.getInstance();

        // Get route
        String routeID = getIntent().getStringExtra(RoutesListActivity.ROUTE_ID);
        (new Utils.httpRequestJson(obj -> {
            if (!obj.get("status").getAsString().equals("success")) {
                Toast.makeText(this, "Error: couldn't get route", Toast.LENGTH_LONG).show();
                return;
            }

            route = Route.fromJson(obj.get("data").getAsJsonObject());
            uiInit();
            initMap(savedInstanceState);

        })).execute(SERVER_URL + "/get-route-by-id?routeID=" + routeID);
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void uiInit() {
        // Update view
        uiUpdateTopBar(route.getTitle());
        uiUpdateRate();
        updateAuthor("Joana Sesinando"); //FIXME: get author from route
        uiUpdateCard(findViewById(R.id.route_description), R.drawable.ic_description,
                getString(R.string.description), route.getDescription());

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
        float rateAvg = 0;
        int rateCount = route.getRates().size();
        for (Integer rate : route.getRates()) {
            rateAvg += rate.floatValue() / rateCount;
        }
        Log.d(TAG, Arrays.toString(route.getRates().toArray()));

        // Update view
        TextView rateValue = findViewById(R.id.route_rate_value);
        rateValue.setText(oneDecimalFormatter.format(rateAvg));
        rateValue.setTextColor(getColorFromRate(rateAvg));

        ImageView rateIcon = findViewById(R.id.route_rate_icon);
        rateIcon.setColorFilter(getColorFromRate(rateAvg));

        TextView reviews = findViewById(R.id.route_nr_reviews);
        String s = "(" + rateCount + " reviews)";
        reviews.setText(s);
    }

    private void updateAuthor(String author) {
        TextView creator = findViewById(R.id.route_author);
        String s = getString(R.string.created_by) + " " + author;
        creator.setText(s);
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
        // TODO
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
        // Init path recorded layer
        style.addSource(new GeoJsonSource(PATH_RECORDED_SOURCE_ID,
                FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                        LineString.fromLngLats(route.getPath())
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
                            LineString.fromLngLats(route.getPath())
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
        intent.putExtras(bundle);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
    }
}