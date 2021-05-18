package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.FileUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Route implements Serializable {

    static String TAG = "Cycling_Fizz@Route";
    static String SERVER_URL = Utils.STATIONS_SERVER_URL;

    private final String id;
    private final String creationTimestamp;
    private final String routeJson;
    private final String idToken;
    private final String authorUID;
    private final ArrayList<PointOfInterest> POIs;
    private final String title;
    private final String description;
    private ArrayList<Review> reviews = new ArrayList<>();
    private Bitmap image;
    private String mediaLink;
    private final int flags;

    private String videoLink;
    public File videoFile;


    private Route(String routeJson, String idToken, String title, String creationTimestamp, String description, ArrayList<PointOfInterest> POIs, String id, String authorUID, Bitmap bitmap, String mediaLink, int flags, File videoFile, String videoLink) {
        this.routeJson = routeJson;
        this.idToken = idToken;
        this.creationTimestamp = creationTimestamp;
        this.title = title;
        this.description = description;
        this.POIs = POIs;
        this.id = id;
        this.authorUID = authorUID;
        this.image = bitmap;
        this.mediaLink = mediaLink;
        this.flags = flags;
        this.videoLink = videoLink;
        this.videoFile = videoFile;
    }

    private Route(String routeJson, String title, String creationTimestamp, String description, ArrayList<PointOfInterest> POIs, String id, String authorUID, String mediaLink, int flags, String videoLink) {
        // from server
        this(routeJson, null, title, creationTimestamp, description, POIs, id, authorUID, null, mediaLink, flags, null, videoLink);
    }

    public Route(String routeJson, String idToken, String title, String description, ArrayList<PointOfInterest> POIs, Bitmap bitmap, File videoFile) {
        // from android
        this(routeJson, idToken, title, null, description, POIs, null, null, bitmap, null, 0, videoFile, null);
    }

    public Feature getRouteFeature() {
        return Feature.fromJson(routeJson);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void getJsonAsync(Utils.OnTaskCompleted<JsonObject> callback) {

        uploadImage(ignored -> uploadVideo(ignored2 -> {
            JsonObject data = new JsonObject();
            data.addProperty("route", routeJson);
            data.addProperty("id_token", idToken);
            data.addProperty("title", title);
            data.addProperty("description", description);
            data.addProperty("media_link", mediaLink);
            data.addProperty("video_link", videoLink);

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
        }));
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

    public File getVideoFile() {
        return videoFile;
    }

    public String getVideoLink() {
        return videoLink;
    }

    public static Route fromJson(JsonObject json) {

        ArrayList<PointOfInterest> POISs = new ArrayList<>();


        for (JsonElement jsonElement : json.get("POIs").getAsJsonArray()) {
            POISs.add(PointOfInterest.fromJson(jsonElement.getAsJsonObject()));
        }

        Route route = new Route(
                json.get("route").getAsString(),
                json.get("title").getAsString(),
                json.get("creation_timestamp").getAsString(),
                json.get("description").getAsString(),
                POISs,
                json.get("id").getAsString(),
                json.get("author_uid").getAsString(),
                json.has("media_link") && !json.get("media_link").isJsonNull() ? json.get("media_link").getAsString() : null,
                json.has("flags") && !json.get("flags").isJsonNull() ? json.get("flags").getAsInt() : 0,
                json.has("video_link") && !json.get("video_link").isJsonNull() ? json.get("video_link").getAsString() : null

        );

        if (json.get("reviews") != null && json.has("reviews")) {
            JsonObject reviewsJson = json.get("reviews").getAsJsonObject();

            for (String reviewID : reviewsJson.keySet()) {
              route.addReviewFromJson(reviewsJson.get(reviewID).getAsJsonObject(), reviewID);
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

    public String getMediaLink() {
        return mediaLink;
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

    public ArrayList<Review> getReviewsNotFlagged() {
        ArrayList<Review> result = new ArrayList<>();
        for (Review review : getReviews()) {
            if (!review.isFlagged()) result.add(review);
        }
        return result;
    }

    public void setReviews(ArrayList<Review> reviews) {
        this.reviews = reviews;
    }

    public void addReviewFromJson(JsonObject json, String id) {
        this.reviews.add(Review.fromJson(json, id));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void uploadVideo(Utils.OnTaskCompleted<Void> callback) {
        if (videoFile == null) {
            callback.onTaskCompleted(null);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                try {
                    String idToken = result.getToken();
                    byte[] byteArray = Files.readAllBytes(videoFile.toPath());
                    String mediaBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

                    JsonObject data = new JsonObject();
                    data.addProperty("media_base64", mediaBase64);
                    data.addProperty("id_token", idToken);

                    (new Utils.httpPostRequestJson(response -> {
                        if (response.get("status").getAsString().equals("success")) {
                            videoLink = response.get("media_link").getAsString();
                        }
                        callback.onTaskCompleted(null);

                    }, data.toString())).execute(SERVER_URL + "/upload-media");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
        } else {
            Log.e(TAG, "Null User");
            callback.onTaskCompleted(null);
        }
    }

    public void downloadVideo(Context context, Utils.OnTaskCompleted<Void> callback) {
        if (videoLink == null) {
            callback.onTaskCompleted(null);
            return;
        }

        (new Utils.httpRequestVideoFile(response -> {
            videoFile = response;
            callback.onTaskCompleted(null);

        }, context)).execute(videoLink);
    }

    public void uploadImage(Utils.OnTaskCompleted<Void> callback) {

        if (image == null) {
            callback.onTaskCompleted(null);
            return;
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
            Log.e(TAG, "Null User");
        }
    }

    public void downloadImage(Utils.OnTaskCompleted<Void> callback) {
        if (mediaLink == null) {
            callback.onTaskCompleted(null);
            return;
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

    public ArrayList<Integer> getRatesNotFlagged() {
        ArrayList<Integer> rates = new ArrayList<>();
        for (Review review : getReviewsNotFlagged()) {
            rates.add(review.getRate());
        }
        return rates;
    }

    public int getAvgRateNotFlagged() {
        int rateAvg = 0;
        int count = getRatesNotFlagged().size();

        for (int rate : getRatesNotFlagged()) {
            rateAvg += rate/count;
        }
        return rateAvg;
    }


    public ArrayList<Point> getPath() {
        LineString line = (LineString) getRouteFeature().geometry();
        assert line != null;
        return new ArrayList<>(line.coordinates());
    }

    public void getReviewOfCurrentUser(Utils.OnTaskCompleted<Review> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                (new Utils.httpRequestJson(response -> {
                    JsonElement reviewElement = response.get("review_id");
                    if (!response.get("status").getAsString().equals("success") || reviewElement.isJsonNull()) {
                        callback.onTaskCompleted(null);
                        return;
                    }

                    String reviewID = reviewElement.getAsString();
                    for (Review review : reviews) {
                        if (review.getId().equals(reviewID)) {
                            callback.onTaskCompleted(review);
                            return;
                        }
                    }
                })).execute(SERVER_URL + "/get-review-by-user-and-route?idToken=" + idToken + "&route_id=" + id);
            });
            return;

        } else {
            Log.e(TAG, "Null User");
        }
        callback.onTaskCompleted(null);
    }

    public void checkIfUserPlayedRoute(Utils.OnTaskCompleted<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                (new Utils.httpRequestJson(response -> {
                    if (!response.get("status").getAsString().equals("success") || response.get("has_played").isJsonNull()) {
                        callback.onTaskCompleted(false);
                        return;
                    }

                    callback.onTaskCompleted(response.get("has_played").getAsBoolean());
                })).execute(SERVER_URL + "/check-if-user-played-route?idToken=" + idToken + "&route_id=" + id);
            });
            return;

        } else {
            Log.e(TAG, "Null User");
        }
        callback.onTaskCompleted(false);
    }

    public void setRouteAsPlayedInServer(Utils.OnTaskCompleted<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                (new Utils.httpRequestJson(response -> {
                    if (!response.get("status").getAsString().equals("success")) {
                        callback.onTaskCompleted(false);
                        return;
                    }

                    callback.onTaskCompleted(true);
                })).execute(SERVER_URL + "/play-route?idToken=" + idToken + "&route_id=" + id);
            });
            return;

        } else {
            Log.e(TAG, "Null User");
        }
        callback.onTaskCompleted(false);
    }

    public void addReview(String msg, int rate, ArrayList<Bitmap> images, Utils.OnTaskCompleted<Review> callback) {
        Review review = new Review(msg, rate, images);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                review.uploadImages(ignored -> review.getJsonAsync(data -> {
                    data.addProperty("id_token", idToken);
                    data.addProperty("route_id", id);

                    (new Utils.httpPostRequestJson(response -> {
                        JsonObject reviewJson = response.get("review").getAsJsonObject();
                        addReviewFromJson(reviewJson.get("review").getAsJsonObject(), reviewJson.get("id").getAsString());
                        callback.onTaskCompleted(Review.fromJson(reviewJson.get("review").getAsJsonObject(), reviewJson.get("id").getAsString()));
                    }, data.toString())).execute(SERVER_URL + "/review-route");
                }));

            });
        } else {
            callback.onTaskCompleted(null);
            Log.e(TAG, "Null User");
        }
    }

    private void removeReviewById(String reviewID) {
        for (Review review : reviews) {
            if (review.getId().equals(reviewID)) {
                reviews.remove(review);
                return;
            }
        }
    }

    public void removeReview(String reviewID, Utils.OnTaskCompleted<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                JsonObject data = new JsonObject();
                data.addProperty("id_token", idToken);
                data.addProperty("route_id", id);
                data.addProperty("review_id", reviewID);

                (new Utils.httpPostRequestJson(response -> {
                    removeReviewById(reviewID);
                    callback.onTaskCompleted(true);
                }, data.toString())).execute(SERVER_URL + "/delete-review");
            });
        } else {
            callback.onTaskCompleted(false);
            Log.e(TAG, "Null User");
        }
    }


    public void preload(Utils.OnTaskCompleted<Boolean> callback) {
        downloadImage(ignored -> {
            AtomicInteger poisPreloaded = new AtomicInteger();

            if (getAllPOIs().size() == 0) {
                callback.onTaskCompleted(true);
            }

            for (PointOfInterest poi : getAllPOIs()) {
                poi.preload(ignored_poi -> {
                    poisPreloaded.getAndIncrement();
                    if (poisPreloaded.get() == getAllPOIs().size()) {
                        callback.onTaskCompleted(true);
                    }
                });
            }
        });
    }

    public void flag(Utils.OnTaskCompleted<Void> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();
                JsonObject data = new JsonObject();
                data.addProperty("idToken", idToken);
                data.addProperty("route_id", id);

                new Utils.httpPostRequestJson(response -> {
                    callback.onTaskCompleted(null);
                }, data.toString()).execute(SERVER_URL + "/flag-route");

            });
        } else {
            callback.onTaskCompleted(null);
            Log.e(TAG, "Null User");
        }
    }


    public boolean isFlagged() {
        return flags >= Utils.MAX_FLAGS_FROM_BAN;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    static class SortByBestRate implements Comparator<Route> {

        @Override
        public int compare(Route r1, Route r2) {
            if (r1.getAvgRateNotFlagged() == r2.getAvgRateNotFlagged()) {
                return r1.flags - r2.flags;
            } else {
                return r2.getAvgRateNotFlagged() - r1.getAvgRateNotFlagged();
            }
        }
    }

    static class SortByWorstRate implements Comparator<Route> {

        @Override
        public int compare(Route r1, Route r2) {
            if (r1.getAvgRateNotFlagged() == r2.getAvgRateNotFlagged()) {
                return r1.flags - r2.flags;
            } else {
                return r1.getAvgRateNotFlagged() - r2.getAvgRateNotFlagged();
            }
        }
    }

    static class SortByMostRecent implements Comparator<Route> {

        @Override
        public int compare(Route r1, Route r2) {
            if (r1.getCreationTimestamp().equals(r2.getCreationTimestamp())) {
                return r1.flags - r2.flags;
            } else {
                return (int) (Long.parseLong(r2.getCreationTimestamp()) - Long.parseLong(r1.getCreationTimestamp()));
            }
        }
    }
    static class SortByLeastRecent implements Comparator<Route> {

        @Override
        public int compare(Route r1, Route r2) {
            if (r1.getCreationTimestamp().equals(r2.getCreationTimestamp())) {
                return r1.flags - r2.flags;
            } else {
                return (int) (Long.parseLong(r1.getCreationTimestamp()) - Long.parseLong(r2.getCreationTimestamp()));
            }
        }
    }

    public static class Review implements Serializable {

        private final String id;
        private final String creationTimestamp;
        private final String authorUID;
        private final String msg;
        private final int rate;

        private final int flags;

        private final ArrayList<Bitmap> images;

        private final ArrayList<String> mediaLinks;

        private String userName;

        private Bitmap userAvatar;

        private final HashMap<String, Boolean> mediaLinksDownloaded = new HashMap<>();


        private Review(String id, String authorUID, String msg, int rate, String creationTimestamp, ArrayList<String> mediaLinks, ArrayList<Bitmap> images, int flags) {
            this.id = id;
            this.creationTimestamp = creationTimestamp;
            this.authorUID = authorUID;
            this.msg = msg;
            this.rate = rate;
            this.mediaLinks = mediaLinks;
            this.images = images;
            this.flags = flags;
        }

        public Review(String id, String authorUID, String msg, int rate, String creationTimestamp, ArrayList<String> mediaLinks, int flags) {
            // from server
            this(id, authorUID, msg, rate, creationTimestamp, mediaLinks, new ArrayList<>(), flags);
        }

        public Review(String msg, int rate, ArrayList<Bitmap> images) {
            // from android
            this(null, null, msg, rate, null, new ArrayList<>(), images, 0);
        }

        public String getId() {
            return id;
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

        public ArrayList<String> getMediaLinks() {
            return mediaLinks;
        }

        public ArrayList<Bitmap> getImages() {
            return images;
        }

        public String getUserName() {
            return userName;
        }

        public Bitmap getUserAvatar() {
            return userAvatar;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public void setUserAvatar(Bitmap userAvatar) {
            this.userAvatar = userAvatar;
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
                    Log.e(TAG, "Null User");
                }

            }
        }

        public void downloadImages(Utils.OnTaskCompleted<Void> callback) {
            if (mediaLinks.size() == images.size() && Utils.areAllTrue(mediaLinksDownloaded.values())) {
                callback.onTaskCompleted(null);
                return;
            }

            for (String mediaLink : mediaLinks) {
                if (mediaLinksDownloaded.containsKey(mediaLink) && mediaLinksDownloaded.get(mediaLink)) continue;

                (new Utils.httpRequestImage(response -> {
                    images.add(response);
                    mediaLinksDownloaded.put(mediaLink, true);
                    if (mediaLinks.size() == images.size() && Utils.areAllTrue(mediaLinksDownloaded.values())) {
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

        public static Review fromJson(JsonObject json, String id) {
            ArrayList<String> mediaLinks = new ArrayList<>();

            if (json.has("media_links") && !json.get("media_links").isJsonNull()) {
                for (JsonElement jsonElement : json.get("media_links").getAsJsonArray()) {
                    mediaLinks.add(jsonElement.getAsString());
                }
            }

            return new Review(
                    id,
                    json.has("author_uid") ? json.get("author_uid").getAsString() : null,
                    json.has("msg") ? json.get("msg").getAsString() : null,
                    json.has("rate") ? json.get("rate").getAsInt() : null,
                    json.has("creation_timestamp") ? json.get("creation_timestamp").getAsString() : null,
                    mediaLinks,
                    json.has("flags") ? json.get("flags").getAsInt() : 0
                    );
        }

        public void flag(String route_id, Utils.OnTaskCompleted<Void> callback) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                user.getIdToken(true).addOnSuccessListener(result -> {
                    String idToken = result.getToken();
                    JsonObject data = new JsonObject();
                    data.addProperty("idToken", idToken);
                    data.addProperty("route_id", route_id);
                    data.addProperty("review_id", id);

                    new Utils.httpPostRequestJson(response -> {
                        callback.onTaskCompleted(null);
                    }, data.toString()).execute(SERVER_URL + "/flag-review");

                });
            } else {
                callback.onTaskCompleted(null);
                Log.e(TAG, "Null User");
            }
        }

        public boolean isFlagged() {
            return flags >= Utils.MAX_FLAGS_FROM_BAN;
        }

        static class SortByBestRate implements Comparator<Review> {

            @Override
            public int compare(Review r1, Review r2) {
                if (r1.getRate() == r2.getRate()) {
                    return r1.flags - r2.flags;
                } else {
                    return r2.getRate() - r1.getRate();
                }
            }
        }

        static class SortByWorstRate implements Comparator<Review> {

            @Override
            public int compare(Review r1, Review r2) {
                if (r1.getRate() == r2.getRate()) {
                    return r1.flags - r2.flags;
                } else {
                    return r1.getRate() - r2.getRate();
                }
            }
        }

        static class SortByMostRecent implements Comparator<Review> {

            @Override
            public int compare(Review r1, Review r2) {
                if (r1.getCreationTimestamp().equals(r2.getCreationTimestamp())) {
                    return r1.flags - r2.flags;
                } else {
                    return (int) (Long.parseLong(r2.getCreationTimestamp()) - Long.parseLong(r1.getCreationTimestamp()));
                }
            }
        }
        static class SortByLeastRecent implements Comparator<Review> {

            @Override
            public int compare(Review r1, Review r2) {
                if (r1.getCreationTimestamp().equals(r2.getCreationTimestamp())) {
                    return r1.flags - r2.flags;
                } else {
                    return (int) (Long.parseLong(r1.getCreationTimestamp()) - Long.parseLong(r2.getCreationTimestamp()));
                }
            }
        }
    }


}


