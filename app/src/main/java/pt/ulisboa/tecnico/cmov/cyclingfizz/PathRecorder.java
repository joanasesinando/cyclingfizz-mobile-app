package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;


import java.util.ArrayList;

public class PathRecorder {
    static String TAG = "Cycling_Fizz@PathRecorder";
//    static String SERVER_URL = "https://stations.cfservertest.ga";
    static String SERVER_URL = "https://d7fa2510995a.ngrok.io";

    private static PathRecorder INSTANCE = null;

    private final FirebaseAuth mAuth;

    private boolean preparingToRecord;
    private boolean isRecording;

    private final ArrayList<Point> path = new ArrayList<>();

    private final ArrayList<PointOfInterest> POIs = new ArrayList<>();

    private PathRecorder() {
        mAuth = FirebaseAuth.getInstance();
    };

    public static PathRecorder getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PathRecorder();
        }
        return(INSTANCE);
    }

    public boolean isPreparingToRecord() {
        cleanGeoJson();
        return preparingToRecord;
    }

    public void setPreparingToRecord(boolean preparingToRecord) {
        this.preparingToRecord = preparingToRecord;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public ArrayList<Point> getPath() {
        return path;
    }

    public void startRecording() {
        cleanGeoJson();
        isRecording = true;
        Log.v(TAG, "Gravando");
    }

    public void stopRecording() {
        isRecording = false;
    }

    public void saveRecording() {
        printFeature();
        saveFeature();
    }

    public boolean addPointToPath(Point point) {
        if (path.size() > 0 && Utils.distanceBetweenPointsInMeters(path.get(path.size() - 1), point) < 5) {
            Log.v(TAG, "ponto ignorado, menos de 5 metros");
            return false;
        }
        path.add(point);
        Log.v(TAG, "add -> " + point.toString());
        return true;
    }

    public void addPOI(String mediaLink, String title, String text, Point point) {

        PointOfInterest pointOfInterest = new PointOfInterest(mediaLink, title, text, point);
        POIs.add(pointOfInterest);
        Log.v(TAG, "add POI -> " + pointOfInterest.toJson().toString());
    }

    public void cleanGeoJson() {
        path.clear();
        POIs.clear();
    }

    private Feature getFeature() {
        Feature feature = Feature.fromGeometry(LineString.fromLngLats(path));
        feature.addStringProperty("", "");

        return feature;
    }

    private void printFeature() {
        if (path.size() < 2) return;


        String jsonString = getFeature().toJson();
        Log.d(TAG, jsonString);
    }

    public void saveFeature() {
        if (path.size() < 2) return;

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                Route route = new Route(getFeature().toJson(), idToken, POIs);
                JsonObject data = route.toJson();

                (new Utils.httpPostRequestJson(response -> {
                    Log.d(TAG, String.valueOf(response));
                }, data.toString())).execute(SERVER_URL + "/save-route");

            });
        } else {
            Log.d(TAG, "Null User");
        }



    }

    private static class Route {

        private final String routeJson;
        private final String idToken;
        private final ArrayList<PointOfInterest> POIs;


        public Route(String routeJson, String idToken, ArrayList<PointOfInterest> POIs) {
            this.routeJson = routeJson;
            this.idToken = idToken;
            this.POIs = POIs;
        }

        public JsonObject toJson() {
            JsonObject data = new JsonObject();
            data.addProperty("route", routeJson);
            data.addProperty("id_token", idToken);

            JsonArray jsonPOIArray = new JsonArray();

            for (PointOfInterest POI : POIs) {
                Log.d(TAG, "POI -> " + POI.toJson().toString());
                jsonPOIArray.add(POI.toJson());
            }

            data.addProperty("POIs", jsonPOIArray.toString());
            return data;
        }
    }

    private static class PointOfInterest {

        private final String mediaLink;
        private final String title;
        private final String text;
        private final Point point;

        public PointOfInterest(String mediaLink, String title, String text, Point point) {
            this.mediaLink = mediaLink;
            this.title = title;
            this.text = text;
            this.point = point;
        }

        public JsonObject toJson() {
            JsonObject data = new JsonObject();
            data.addProperty("mediaLink", mediaLink);
            data.addProperty("title", title);
            data.addProperty("text", text);
            data.addProperty("point", Feature.fromGeometry(point).toJson());

            return data;
        }

    }

}