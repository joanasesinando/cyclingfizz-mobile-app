package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.content.Context;
import android.util.Log;
import android.view.MenuItem;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

public final class Toolbar { // FIXME: refactor this and delete

    static String TAG = "Cycling_Fizz@Toolbar";

    public static boolean mapToolbarItemClicked(MenuItem item, Context context, MapboxMap mapboxMap) {
        Log.d(TAG, "Clicked on \"" + item.getTitle() + "\"");

        int id = item.getItemId();
        if (id == R.id.filter_cycleways) {
            try {
                Layer cyclewaysLayer = mapboxMap.getStyle().getLayer(MapActivity.CYCLEWAYS_LAYER_ID);

                cyclewaysLayer.setProperties(visibility(item.isChecked() ? Property.NONE : Property.VISIBLE));
                item.setChecked(!item.isChecked());
            } catch (NullPointerException ignored) { }

            // Keep the popup menu open
            Utils.keepMenuOpen(item, context);

        } else if (id == R.id.filter_gira) {
            try {
                Layer giraLayer = mapboxMap.getStyle().getLayer(MapActivity.GIRA_STATION_LAYER_ID);
                Layer giraClustersLayer = mapboxMap.getStyle().getLayer(MapActivity.GIRA_CLUSTER_LAYER_ID);
                Layer giraCountLayer = mapboxMap.getStyle().getLayer(MapActivity.GIRA_COUNT_LAYER_ID);

                PropertyValue<String> visibility = visibility(item.isChecked() ? Property.NONE : Property.VISIBLE);

                giraLayer.setProperties(visibility);
                giraClustersLayer.setProperties(visibility);
                giraCountLayer.setProperties(visibility);
                item.setChecked(!item.isChecked());

            } catch (NullPointerException ignored) { }

            // Keep the popup menu open
            Utils.keepMenuOpen(item, context);

        }
        return false;
    }
}
