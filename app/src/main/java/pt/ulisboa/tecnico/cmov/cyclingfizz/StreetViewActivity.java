package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
import com.mapbox.geojson.Point;

public class StreetViewActivity extends AppCompatActivity implements OnStreetViewPanoramaReadyCallback {

    Point coordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.street_view);

        coordinates = (Point) Point.fromJson(getIntent().getStringExtra(StationActivity.COORDINATES));

        SupportStreetViewPanoramaFragment streetViewPanoramaFragment = (SupportStreetViewPanoramaFragment) getSupportFragmentManager().findFragmentById(R.id.gms_street_view);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);

        // Set click listener for close btn
        MaterialToolbar materialToolbar = findViewById(R.id.backBar);
        materialToolbar.setNavigationOnClickListener(this::closeBtnClicked);
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
        streetViewPanorama.setPosition(new LatLng(coordinates.latitude(), coordinates.longitude()));
    }

    public void closeBtnClicked(View view) {
        finish();
    }
}