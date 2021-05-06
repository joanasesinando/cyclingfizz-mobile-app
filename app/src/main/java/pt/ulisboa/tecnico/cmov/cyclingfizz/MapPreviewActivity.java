package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.TransitionOptions;

public class MapPreviewActivity extends AppCompatActivity {

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_preview);

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

        mapView = findViewById(R.id.previewRouteMapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapboxMap -> mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            style.setTransition(new TransitionOptions(0, 0, false));

            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                    38.722252, -9.139337), 13)); //fixme zoom and center on root

            mapboxMap.getUiSettings().setAllGesturesEnabled(false);


            // Map is set up and the style has loaded. Now you can add data or make other map adjustments.

        }));


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