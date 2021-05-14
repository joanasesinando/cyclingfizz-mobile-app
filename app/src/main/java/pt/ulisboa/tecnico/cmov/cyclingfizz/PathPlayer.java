package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Point;

public class PathPlayer {
    static String TAG = "Cycling_Fizz@PathPlayer";
    static String SERVER_URL = Utils.STATIONS_SERVER_URL;
    static final int MAX_DISTANCE_FROM_POI = 7;

    private static PathPlayer INSTANCE = null;
    private final FirebaseAuth mAuth;
    private boolean reachedEnd = false;
    private boolean routeAlreadyRated = false;

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

    public void playRoute(Route route, Utils.OnTaskCompleted<Boolean> callback) {
        routePlaying = route;

        routePlaying.preload(preloaded -> {
            (new Thread(() -> {
                checkIfRouteRated();
                route.setRouteAsPlayedInServer(ignored -> {});
            })).start();
            callback.onTaskCompleted(preloaded);
        });


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

    public boolean checkIfEnd(Point point) {
        if (!reachedEnd && isPlayingRoute() && Utils.distanceBetweenPointsInMeters(routePlaying.getPath().get(routePlaying.getPath().size() - 1), point) < MAX_DISTANCE_FROM_POI) {
            reachedEnd = true;
            return true;
        }
        return false;
    }

    private void checkIfRouteRated() {
        routePlaying.getReviewOfCurrentUser(review -> {
            routeAlreadyRated = review != null;
        });
    }

    public boolean isRouteAlreadyRated() {
        return routeAlreadyRated;
    }
}
