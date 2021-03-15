package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
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
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;

public class MapActivity extends AppCompatActivity {

    static String MAP_SERVER_URL = "https://map.server.cyclingfizz.pt";

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

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_map);

        mapView = (MapView) findViewById(R.id.mapView);
        System.out.println("MY_log: " + mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            style.setTransition(new TransitionOptions(0, 0, false));

            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                    38.722252, -9.139337), 13));


            addIcons(style);
            addGiraStations(style);
            addMobiCascaisStations(style);


            mapboxMap.addOnMapClickListener(point -> {

                PointF pointf = mapboxMap.getProjection().toScreenLocation(point);
                RectF rectF = new RectF(pointf.x - 10, pointf.y - 10, pointf.x + 10, pointf.y + 10);

                List<Feature> giraFeatureList = mapboxMap.queryRenderedFeatures(rectF, GIRA_STATION_LAYER_ID);


                for (Feature feature : giraFeatureList) {
                    Log.d("Feature found with %1$s", feature.toJson());

                    Toast.makeText(MapActivity.this, "Id = " + feature.getProperty("tags").getAsJsonObject().get("ref").getAsString(),
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            // Map is set up and the style has loaded. Now you can add data or make other map adjustments


        }));
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
                    // Point to GeoJSON data. This example visualizes all M1.0+ gira from
                    // 12/22/15 to 1/21/16 as logged by USGS' Earthquake hazards program.
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
                iconSize(literal(1f))
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
                    // Point to GeoJSON data. This example visualizes all M1.0+ gira from
                    // 12/22/15 to 1/21/16 as logged by USGS' Earthquake hazards program.
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
                iconSize(literal(1f))
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