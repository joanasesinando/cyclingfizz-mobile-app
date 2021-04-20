package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.util.Log;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import java.util.ArrayList;

public class PathRecorder {
    static String TAG = "Cycling_Fizz@PathRecorder";
    static String SERVER_URL = "https://9a5dd377f58b.ngrok.io";

    private static PathRecorder INSTANCE = null;

    private boolean preparingToRecord;
    private boolean isRecording;

    private final ArrayList<Point> path = new ArrayList<>();

    private PathRecorder() {};

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
        Log.v(TAG, "add " + point.toString());
        return true;
    }

    public void cleanGeoJson() {
        path.clear();
    }

    private Feature getFeature() {
        Feature feature = Feature.fromGeometry(LineString.fromLngLats(path));
        feature.addStringProperty("", "");

        return feature;
    }

    private void printFeature() {
        if (path.isEmpty()) return;


        String jsonString = getFeature().toJson();
        Log.d(TAG, jsonString);
    }

    public void saveFeature() {
        if (path.isEmpty()) return;

        JsonObject data = new JsonObject();
        data.addProperty("route", getFeature().toJson());

        (new Utils.httpPostRequestJson(response -> {
            Log.d(TAG, String.valueOf(response));
        }, data.toString())).execute(SERVER_URL + "/save-route");
    }


}