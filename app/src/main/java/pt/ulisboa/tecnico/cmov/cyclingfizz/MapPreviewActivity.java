package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;
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
import java.util.List;
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

public class MapPreviewActivity extends AppCompatActivity {

    static final String ROUTE_PATH = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_PATH";
    static final String ROUTE_POIS = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_POIS";
    public final static String POI = "pt.ulisboa.tecnico.cmov.cyclingfizz.POI";

    ArrayList<Point> path;
    ArrayList<PointOfInterest> pois;

    private MapView mapView;
    private MapboxMap mapboxMap;

    static String PATH_RECORDED_SOURCE_ID = "path-recorded-source";
    static String PATH_RECORDED_LAYER_ID = "path-recorded-layer";

    static String POI_SOURCE_ID = "poi-source";
    static String POI_ICON_ID = "poi-icon";
    static String POI_LAYER_ID = "poi-layer";
    static String POI_CLUSTER_LAYER_ID = "poi-cluster-layer";
    static String POI_COUNT_LAYER_ID = "poi-count-layer";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_preview);

        Intent intent = getIntent();
        path = (ArrayList<Point>) intent.getSerializableExtra(ROUTE_PATH);
        pois = (ArrayList<PointOfInterest>) intent.getSerializableExtra(ROUTE_POIS);
        initMap(savedInstanceState);

        // Set click listener for close btn
        MaterialToolbar materialToolbar = findViewById(R.id.backBar);
        materialToolbar.setNavigationOnClickListener(this::closeBtnClicked);
    }

    private void closeBtnClicked(View view) {
        finish();
    }

    private void initMap(Bundle savedInstanceState) {
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapbox -> {
            mapboxMap = mapbox;
            mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
                style.setTransition(new TransitionOptions(0, 0, false));

                LatLngBounds latLngBounds;
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

                    PointF pointf = mapboxMap.getProjection().toScreenLocation(point);
                    RectF rectF = new RectF(pointf.x - 10, pointf.y - 10, pointf.x + 10, pointf.y + 10);
                    List<Feature> poisFeatureList = mapboxMap.queryRenderedFeatures(rectF, POI_LAYER_ID);

                    String itemSelected = "";
                    Feature feature = null;
                    if (poisFeatureList.size() > 0) {
                        itemSelected = "POI";
                        feature = poisFeatureList.get(0);
                    }

                    Intent intent;
                    Bundle bundle;
                    switch (itemSelected) {
                        case "POI":
                            int poiIndex = feature.getNumberProperty("id").intValue();
                            PointOfInterest poi = pois.get(poiIndex);
                            intent = new Intent(this, ViewPOIActivity.class);
                            bundle = new Bundle();
                            bundle.putSerializable(POI, poi);
                            intent.putExtras(bundle);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                            break;

                        default:
                            return true;
                    }
                    return true;
                });

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
                        LineString.fromLngLats(path)
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
        for (PointOfInterest poi : pois) {
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
                            LineString.fromLngLats(path)
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
            for (PointOfInterest poi : pois) {
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