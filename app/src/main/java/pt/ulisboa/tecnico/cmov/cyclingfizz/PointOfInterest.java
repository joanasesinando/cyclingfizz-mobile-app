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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PointOfInterest implements Serializable {

    static String SERVER_URL = "https://stations.cfservertest.ga";
    static String TAG = "Cycling_Fizz@POI";


    private final Point coord;
    private String name;
    private String description;
    private ArrayList<Bitmap> images = new ArrayList<>();
    private ArrayList<String> mediaLinks = new ArrayList<>();

    private ArrayList<Comment> comments = new ArrayList<>();

    private boolean alreadyVisited = false;

    public PointOfInterest(Point coord, String name, String description, List<Bitmap> images) {
        this.coord = coord;
        this.name = name;
        this.description = description;
        this.images = new ArrayList<>(images);
    }

    public PointOfInterest(String name, String description, List<String> mediaLinks, Point coord) {
        this.coord = coord;
        this.name = name;
        this.description = description;
        this.mediaLinks = new ArrayList<>(mediaLinks);
    }

    public Point getCoord() {
        return coord;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) { this.name = name; }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) { this.description = description; }

    public ArrayList<Bitmap> getImages() {
        return images;
    }
    public void setImages(List<Bitmap> images) {
        this.images = new ArrayList<>(images);
    }

    public ArrayList<Comment> getComments() {
        return comments;
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
        ArrayList<Comment> comments = new ArrayList<>();

        for (JsonElement jsonElement :  json.get("media_links").getAsJsonArray()) {
            mediaLinks.add(jsonElement.getAsString());
        }

        PointOfInterest poi = new PointOfInterest(
                json.get("title").getAsString(),
                json.get("description").getAsString(),
                mediaLinks,
                (Point) Feature.fromJson(json.get("point").getAsString()).geometry());

        if (json.get("comments") != null && json.has("comments")) {
            JsonArray commentsJson = json.get("comments").getAsJsonArray();
            for (JsonElement commentJson : commentsJson) {
                poi.addCommentFromJson(commentJson.getAsJsonObject());
            }
        }

        return poi;
    }

    public void addCommentFromJson(JsonObject json) {
        this.comments.add(Comment.fromJson(json));
    }

    public void commentPOI(String msg, ArrayList<Bitmap> images, Utils.OnTaskCompleted<Comment> callback) {
        Comment comment = new Comment(msg, images);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                comment.uploadImages(ignored -> {
                    comment.getJsonAsync(data -> {
                        data.addProperty("idToken", idToken);
                        (new Utils.httpPostRequestJson(response -> {
                            addCommentFromJson(response);
                            callback.onTaskCompleted(Comment.fromJson(response));
                        }, data.toString())).execute(SERVER_URL + "/comment-poi");
                    });
                });

            });
        } else {
            callback.onTaskCompleted(null);
            Log.d(TAG, "Null User");
        }
    }




    public static class Comment implements Serializable {

        private final String creationTimestamp;
        private final String authorUID;
        private final String msg;
        private final ArrayList<String> mediaLinks;
        private final ArrayList<Bitmap> images;

        private Comment(String creationTimestamp, String authorUID, String msg, ArrayList<String> mediaLinks, ArrayList<Bitmap> images) {
            this.creationTimestamp = creationTimestamp;
            this.authorUID = authorUID;
            this.msg = msg;
            this.mediaLinks = mediaLinks;
            this.images = images;
        }

        public Comment(String creationTimestamp, String authorUID, String msg, ArrayList<String> mediaLinks) {
            this(creationTimestamp, authorUID, msg, mediaLinks, new ArrayList<>());
        }

        public Comment(String msg, ArrayList<Bitmap> images) {
            this(null, null, msg, new ArrayList<>(), images);

        }

        public String getCreationTimestamp() {
            return creationTimestamp;
        }

        public String getAuthorUID() {
            return authorUID;
        }

        public String getMsg() {
            return msg;
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

            uploadImages(res -> {
                JsonArray jsonMediaLinks = new JsonArray();
                for (String mediaLink : mediaLinks) {
                    jsonMediaLinks.add(mediaLink);
                }
                data.addProperty("media_links", jsonMediaLinks.toString());

                callback.onTaskCompleted(data);
            });


        }

        public static Comment fromJson(JsonObject json) {
            ArrayList<String> mediaLinks = new ArrayList<>();

            if (json.has("media_links")) {
                for (JsonElement jsonElement : json.get("media_links").getAsJsonArray()) {
                    mediaLinks.add(jsonElement.getAsString());
                }
            }

            return new Comment(
                    json.has("creation_timestamp") ? json.get("creation_timestamp").getAsString() : null,
                    json.has("author_uid") ? json.get("author_uid").getAsString() : null,
                    json.has("msg") ? json.get("msg").getAsString() : null,
                    mediaLinks
            );
        }
    }
}
