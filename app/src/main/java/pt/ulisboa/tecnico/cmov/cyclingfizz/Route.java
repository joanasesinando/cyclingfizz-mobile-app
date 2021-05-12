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
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Route implements Serializable {

    static String TAG = "Cycling_Fizz@Route";
    static String SERVER_URL = "https://stations.cfservertest.ga";

    private final String id;
    private final String routeJson;
    private final String idToken;
    private final String authorUID;
    private final ArrayList<PointOfInterest> POIs;
    private final String title;
    private final String description;
    private ArrayList<Review> reviews = new ArrayList<>();
    private Bitmap image;
    private String mediaLink;


    private Route(String routeJson, String idToken, String title, String description, ArrayList<PointOfInterest> POIs, String id, String authorUID, Bitmap bitmap, String mediaLink) {
        this.routeJson = routeJson;
        this.idToken = idToken;
        this.title = title;
        this.description = description;
        this.POIs = POIs;
        this.id = id;
        this.authorUID = authorUID;
        this.image = bitmap;
        this.mediaLink = mediaLink;
    }

    private Route(String routeJson, String title, String description, ArrayList<PointOfInterest> POIs, String id, String authorUID, String mediaLink) {
        this(routeJson, null, title, description, POIs, id, authorUID, null, mediaLink);
    }

    public Route(String routeJson, String idToken, String title, String description, ArrayList<PointOfInterest> POIs, Bitmap bitmap) {
        this(routeJson, idToken, title, description, POIs, null, null, bitmap, null);
    }

    public Feature getRouteFeature() {
        return Feature.fromJson(routeJson);
    }

    public void getJsonAsync(Utils.OnTaskCompleted<JsonObject> callback) {

        uploadImage(ignored -> {
            JsonObject data = new JsonObject();
            data.addProperty("route", routeJson);
            data.addProperty("id_token", idToken);
            data.addProperty("title", title);
            data.addProperty("description", description);
            data.addProperty("media_link", mediaLink);

            JsonArray jsonPOIArray = new JsonArray();

            if (jsonPOIArray.size() == POIs.size()) {
                data.addProperty("POIs", jsonPOIArray.toString());
                callback.onTaskCompleted(data);
            }

            for (PointOfInterest POI : POIs) {

                POI.getJsonAsync(json -> {
                    jsonPOIArray.add(json);
                    if (jsonPOIArray.size() == POIs.size()) {
                        data.addProperty("POIs", jsonPOIArray.toString());
                        callback.onTaskCompleted(data);
                    }
                });
            }
        });
    }

    public ArrayList<PointOfInterest> getAllPOIs() {
        return POIs;
    }

    public String getId() {
        return id;
    }

    public Bitmap getImage() {
        return image;
    }

    public static Route fromJson(JsonObject json) {

        ArrayList<PointOfInterest> POISs = new ArrayList<>();

        for (JsonElement jsonElement : json.get("POIs").getAsJsonArray()) {
            POISs.add(PointOfInterest.fromJson(jsonElement.getAsJsonObject()));
        }

        Route route = new Route(
                json.get("route").getAsString(),
                json.get("title").getAsString(),
                json.get("description").getAsString(),
                POISs,
                json.get("id").getAsString(),
                json.get("author_uid").getAsString(),
                json.has("media_link") ? json.get("media_link").getAsString() : null
        );

        if (json.get("reviews") != null && json.has("reviews")) {
            JsonArray reviewsJson = json.get("reviews").getAsJsonArray();
            for (JsonElement reviewJson : reviewsJson) {
              route.addReviewFromJson(reviewJson.getAsJsonObject());
            }
        }

        return route;
    }

    public String getRoute() {
        return routeJson;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getAuthorUID() {
        return authorUID;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ArrayList<Review> getReviews() {
        return reviews;
    }

    public void setReviews(ArrayList<Review> reviews) {
        this.reviews = reviews;
    }

    public void addReviewFromJson(JsonObject json) {
        this.reviews.add(Review.fromJson(json));
    }

    public void uploadImage(Utils.OnTaskCompleted<Void> callback) {
        if (image == null) {
            callback.onTaskCompleted(null);
        }

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
                    if (response.get("status").getAsString().equals("success")) {
                        mediaLink = response.get("media_link").getAsString();
                    }
                    callback.onTaskCompleted(null);

                }, data.toString())).execute(SERVER_URL + "/upload-media");

            });
        } else {
            Log.d(TAG, "Null User");
        }
    }

    public void downloadImage(Utils.OnTaskCompleted<Void> callback) {
        if (mediaLink == null) {
            callback.onTaskCompleted(null);
        }

        (new Utils.httpRequestImage(response -> {
            image = response;
            callback.onTaskCompleted(null);

        })).execute(mediaLink);
    }

    public ArrayList<Integer> getRates() {
        ArrayList<Integer> rates = new ArrayList<>();
        for (Review review : reviews) {
            rates.add(review.getRate());
        }
        return rates;
    }

    public ArrayList<Point> getPath() {
        LineString line = (LineString) getRouteFeature().geometry();
        assert line != null;
        return new ArrayList<>(line.coordinates());
    }


    public void reviewRoute(String msg, int rate, ArrayList<Bitmap> images, Utils.OnTaskCompleted<Review> callback) {
        Review review = new Review(msg, rate, images);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                review.uploadImages(ignored -> {
                    review.getJsonAsync(data -> {
                        data.addProperty("idToken", idToken);
                        (new Utils.httpPostRequestJson(response -> {
                            addReviewFromJson(response);
                            callback.onTaskCompleted(Review.fromJson(response));
                        }, data.toString())).execute(SERVER_URL + "/review-route");
                    });
                });

            });
        } else {
            callback.onTaskCompleted(null);
            Log.d(TAG, "Null User");
        }
    }

    public static class Review implements Serializable {

        private final String creationTimestamp;
        private final String authorUID;
        private final String msg;
        private final int rate;

        private ArrayList<Bitmap> images = new ArrayList<>();
        private ArrayList<String> mediaLinks = new ArrayList<>();

        private Review(String authorUID, String msg, int rate, String creationTimestamp, ArrayList<String> mediaLinks, ArrayList<Bitmap> images) {
            this.creationTimestamp = creationTimestamp;
            this.authorUID = authorUID;
            this.msg = msg;
            this.rate = rate;
            this.mediaLinks = mediaLinks;
            this.images = images;
        }

        public Review(String authorUID, String msg, int rate, String creationTimestamp, ArrayList<String> mediaLinks) {
            this(authorUID, msg, rate, creationTimestamp, mediaLinks, new ArrayList<>());
        }

        public Review(String msg, int rate, ArrayList<Bitmap> images) {
            this(null, msg, rate, null, new ArrayList<>(), images);
        }

        public String getAuthorUID() {
            return authorUID;
        }

        public String getMsg() {
            return msg;
        }

        public int getRate() {
            return rate;
        }

        public String getCreationTimestamp() {
            return creationTimestamp;
        }

        public ArrayList<Bitmap> getImages() {
            return images;
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

        public void downloadImages(Utils.OnTaskCompleted<Void> callback) {
            if (mediaLinks.size() == images.size()) {
                callback.onTaskCompleted(null);
            }

            for (String mediaLink : mediaLinks) {

                (new Utils.httpRequestImage(response -> {
                    images.add(response);
                    if (mediaLinks.size() == images.size()) {
                        callback.onTaskCompleted(null);
                    }
                })).execute(mediaLink);
            }
        }

        public void getJsonAsync(Utils.OnTaskCompleted<JsonObject> callback) {
            JsonObject data = new JsonObject();
            data.addProperty("msg", msg);
            data.addProperty("rate", rate);

            uploadImages(res -> {
                JsonArray jsonMediaLinks = new JsonArray();
                for (String mediaLink : mediaLinks) {
                    jsonMediaLinks.add(mediaLink);
                }
                data.addProperty("media_links", jsonMediaLinks.toString());

                callback.onTaskCompleted(data);
            });


        }

        public static Review fromJson(JsonObject json) {
            ArrayList<String> mediaLinks = new ArrayList<>();

            if (json.has("media_links")) {
                for (JsonElement jsonElement : json.get("media_links").getAsJsonArray()) {
                    mediaLinks.add(jsonElement.getAsString());
                }
            }

            return new Review(
                    json.has("author_uid") ? json.get("author_uid").getAsString() : null,
                    json.has("msg") ? json.get("msg").getAsString() : null,
                    json.has("rate") ? json.get("rate").getAsInt() : null,
                    json.has("creation_timestamp") ? json.get("creation_timestamp").getAsString() : null,
                    mediaLinks
            );
        }
    }

}


