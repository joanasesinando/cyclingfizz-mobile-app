package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Point;

public class PathPlayer {
    static String TAG = "Cycling_Fizz@PathPlayer";
    static String SERVER_URL = "https://stations.cfservertest.ga";

    private static PathPlayer INSTANCE = null;
    private final FirebaseAuth mAuth;

    private Route routePlaying;

    private PathPlayer() {
        mAuth = FirebaseAuth.getInstance();
    };


    public static PathPlayer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PathPlayer();
        }
        return(INSTANCE);
    }

    public void playRouteFromRouteId(String id, Utils.OnTaskCompleted<Boolean> callback) {
        (new Utils.httpRequestJson(obj -> {
            if (!obj.get("status").getAsString().equals("success")) {
                callback.onTaskCompleted(false);
                return;
            }

            try {
                playRoute(Route.fromJson(obj.get("data").getAsJsonObject()));
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                callback.onTaskCompleted(false);
                return;
            }

            callback.onTaskCompleted(true);
        })).execute(SERVER_URL + "/get-route-by-id?routeID=" + id);
    }

    public void playRoute(Route route) {
        routePlaying = route;
    }

    public void stopRoute() {
        routePlaying = null;
    }

    public boolean isPlayingRoute() {
        return routePlaying != null;
    }

    public PointOfInterest checkIfNearPOI(Point point) {

        if (!isPlayingRoute()) return null;

        for (PointOfInterest poi : routePlaying.getPOIs()) {
            if (poi.notAlreadyVisited() && Utils.distanceBetweenPointsInMeters(poi.getCoord(), point) < 5) {
                poi.setAlreadyVisited(true);
                return poi;
            }
        }

        return null;
    }


}
