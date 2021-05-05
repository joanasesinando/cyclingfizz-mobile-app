package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PointOfInterest {

    static String SERVER_URL = "https://stations.cfservertest.ga";
    static String TAG = "Cycling_Fizz@POI";


    private ArrayList<Bitmap> images = new ArrayList<>();
    private ArrayList<String> mediaLinks = new ArrayList<>();
    private final String name;
    private final String description;
    private final Point coord;
    private boolean alreadyVisited = false;

    public PointOfInterest(List<Bitmap> images, String name, String description, Point coord) {
        this.images = new ArrayList<>(images);
        this.name = name;
        this.description = description;
        this.coord = coord;
    }

    public PointOfInterest(String name, String description, Point coord, List<String> mediaLinks) {
        this.mediaLinks = new ArrayList<>(mediaLinks);
        this.name = name;
        this.description = description;
        this.coord = coord;
    }

    public void uploadImages(Utils.OnTaskCompleted<Void> callback) {
        if (images.size() == mediaLinks.size()) {
            callback.onTaskCompleted(null);
        }
        for (Bitmap image : images) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                user.getIdToken(true).addOnSuccessListener(result -> {
                    String idToken = result.getToken();

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
                    byte[] byteArray = outputStream.toByteArray();

                    String mediaBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);


                    JsonObject data = new JsonObject();
                    data.addProperty("media_base64", mediaBase64);
                    data.addProperty("id_token", idToken);

                    (new Utils.httpPostRequestJson(response -> {
                        if (response.get("status").getAsString().equals("success"))
                            mediaLinks.add(response.get("media_link").getAsString());

                        if (images.size() == mediaLinks.size()) {
                            callback.onTaskCompleted(null);
                        }
                    }, data.toString())).execute(SERVER_URL + "/upload-media");

                });
            } else {
                Log.d(TAG, "Null User");
            }

        }
    }

    public Point getCoord() {
        return coord;
    }

    public String getName() {
        return name;
    }

    public void getJsonAsync(Utils.OnTaskCompleted<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("title", name);
        data.addProperty("description", description);
        data.addProperty("point", Feature.fromGeometry(coord).toJson());

        uploadImages(res -> {
            JsonArray jsonMediaLinks = new JsonArray();
            for (String mediaLink : mediaLinks) {
                jsonMediaLinks.add(mediaLink);
            }
            data.addProperty("media_links", jsonMediaLinks.toString());

            callback.onTaskCompleted(data);
        });


    }

    public boolean notAlreadyVisited() {
        return !alreadyVisited;
    }

    public void setAlreadyVisited(boolean alreadyVisited) {
        this.alreadyVisited = alreadyVisited;
    }

    public static PointOfInterest fromJson(JsonObject json) {

        ArrayList<String> mediaLinks = new ArrayList<>();

        for (JsonElement jsonElement :  json.get("media_links").getAsJsonArray()) {
            mediaLinks.add(jsonElement.getAsString());
        }

        Log.e(TAG, json.get("point").getAsString());

        return new PointOfInterest(
                json.get("title").getAsString(),
                json.get("description").getAsString(),
                (Point) Feature.fromJson(json.get("point").getAsString()).geometry(),
                mediaLinks
        );
    }
}
