package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Point;

public class PathPlayer {
    static String TAG = "Cycling_Fizz@PathPlayer";
    static String SERVER_URL = "https://stations.cfservertest.ga";
    static final int MAX_DISTANCE_FROM_POI = 5;

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

    public Route getPlayingRoute() {
        return routePlaying;
    }

    public void playRoute(Route route) {
        routePlaying = route;
        Log.d(TAG, String.valueOf(routePlaying));
    }

    public void stopRoute() {
        routePlaying = null;
    }

    public boolean isPlayingRoute() {
        return routePlaying != null;
    }

    public PointOfInterest checkIfNearPOI(Point point) {

        if (!isPlayingRoute()) return null;

        for (PointOfInterest poi : routePlaying.getAllPOIs()) {
            if (poi.notAlreadyVisited() && Utils.distanceBetweenPointsInMeters(poi.getCoord(), point) < MAX_DISTANCE_FROM_POI) {
                poi.setAlreadyVisited(true);
                return poi;
            }
        }

        return null;
    }

    public void commentPOI(int POIindex, String comment, Utils.OnTaskCompleted<Boolean> callback) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                JsonObject data = new JsonObject();
                data.addProperty("id_token", idToken);
                data.addProperty("route_id", routePlaying.getId());
                data.addProperty("poi_id", POIindex);
                data.addProperty("comment", comment);
                //fixme add media link

                (new Utils.httpPostRequestJson(obj -> {
                    callback.onTaskCompleted(obj.get("status").getAsString().equals("success"));
                }, data.toString())).execute(SERVER_URL + "/comment-poi");
            });
        } else {
            callback.onTaskCompleted(false);
            Log.d(TAG, "Null User");
        }
    }

    public void commentRoute(String comment, Utils.OnTaskCompleted<Boolean> callback) { // FIXME: remove? only rate
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                JsonObject data = new JsonObject();
                data.addProperty("id_token", idToken);
                data.addProperty("route_id", routePlaying.getId());
                data.addProperty("comment", comment);
                //fixme add media link

                (new Utils.httpPostRequestJson(obj -> {
                    callback.onTaskCompleted(obj.get("status").getAsString().equals("success"));
                }, data.toString())).execute(SERVER_URL + "/comment-poi");
            });
        } else {
            callback.onTaskCompleted(false);
            Log.d(TAG, "Null User");
        }
    }

    public void rateRoute(int rate, Utils.OnTaskCompleted<Boolean> callback) { // FIXME: should swap with old rate if user has already rated
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                JsonObject data = new JsonObject();
                data.addProperty("id_token", idToken);
                data.addProperty("route_id", routePlaying.getId());
                data.addProperty("rate", rate);
                //fixme add media link

                (new Utils.httpPostRequestJson(obj -> {
                    callback.onTaskCompleted(obj.get("status").getAsString().equals("success"));
                }, data.toString())).execute(SERVER_URL + "/rate-poi");
            });
        } else {
            callback.onTaskCompleted(false);
            Log.d(TAG, "Null User");
        }
    }


}
