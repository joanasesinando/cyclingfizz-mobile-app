package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;


import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PathRecorder {
    static String TAG = "Cycling_Fizz@PathRecorder";
    static String SERVER_URL = "https://stations.cfservertest.ga";

    private static PathRecorder INSTANCE = null;

    private final FirebaseAuth mAuth;

    private boolean preparingToRecord;
    private boolean isRecording;

    private boolean POIAdded;

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
        return preparingToRecord;
    }

    public void setPreparingToRecord(boolean preparingToRecord) {
        if (preparingToRecord)
            cleanGeoJson();
        this.preparingToRecord = preparingToRecord;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean POIAdded() {
        return POIAdded;
    }

    public void setPOIAdded(boolean POIadded) {
        this.POIAdded = POIadded;
    }

    public ArrayList<Point> getPath() {
        return path;
    }

    public ArrayList<PointOfInterest> getAllPOIs() {
        return POIs;
    }

    public PointOfInterest getPOI(int index) {
        return POIs.get(index);
    }

    public void startRecording() {
        cleanGeoJson();
        isRecording = true;
    }

    public void stopRecording() {
        isRecording = false;
    }

    public void saveRecording(String name, String description, Utils.OnTaskCompleted<Boolean> callback) {
        printFeature();
        saveFeature(name, description, callback);
    }

    public boolean addPointToPath(Point point) {
        if (path.size() > 0 && Utils.distanceBetweenPointsInMeters(path.get(path.size() - 1), point) < 5) {
            return false;
        }
        path.add(point);
        return true;
    }

    public void addPOI(List<Bitmap> images, String name, String description, Point coord) {
        PointOfInterest pointOfInterest = new PointOfInterest(images, name, description, coord);
        POIs.add(pointOfInterest);
        POIAdded = true;
    }

    public void editPOI(int index, String name, String description, ArrayList<Bitmap> images) {
        PointOfInterest poi = getPOI(index);
        poi.name = name;
        poi.description = description;
        poi.images = images;
    }

    public void removePOI(int index) {
        POIs.remove(index);
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

    public void saveFeature(String name, String description, Utils.OnTaskCompleted<Boolean> callback) {
        if (path.size() < 2) return; // TODO: show dialog before if is

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                Route route = new Route(getFeature().toJson(), idToken, name, description, POIs);

                route.getJsonAsync(data -> {
                    (new Utils.httpPostRequestJson(response -> {
                        Log.d(TAG, String.valueOf(response));
                        callback.onTaskCompleted(true);
                    }, data.toString())).execute(SERVER_URL + "/save-route");
                });


            });
        } else {
            callback.onTaskCompleted(false);
            Log.d(TAG, "Null User");
        }
    }
}