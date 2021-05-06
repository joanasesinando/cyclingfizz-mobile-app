package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Route {

    private final String id;
    private final String routeJson;
    private final String idToken;
    private String authorUID;
    private final ArrayList<PointOfInterest> POIs;
    private final String title;
    private final String description;
    private ArrayList<Review> reviews = new ArrayList<>();

    static String SERVER_URL = "https://stations.cfservertest.ga";
    static String TAG = "Cycling_Fizz@Route";

    private Route(String routeJson, String idToken, String title, String description, ArrayList<PointOfInterest> POIs, String id, String authorUID) {
        this.routeJson = routeJson;
        this.idToken = idToken;
        this.title = title;
        this.description = description;
        this.POIs = POIs;
        this.id = id;
    }

    private Route(String routeJson, String title, String description, ArrayList<PointOfInterest> POIs, String id, String authorUID) {
        this(routeJson, null, title, description, POIs, id, authorUID);
    }

    public Route(String routeJson, String idToken, String title, String description, ArrayList<PointOfInterest> POIs) {
        this(routeJson, idToken, title, description, POIs, null, null);
    }

    public Feature getRouteFeature() {
        return Feature.fromJson(routeJson);
    }

    public void getJsonAsync(Utils.OnTaskCompleted<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("route", routeJson);
        data.addProperty("id_token", idToken);
        data.addProperty("title", title);
        data.addProperty("description", description);

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
    }

    public ArrayList<PointOfInterest> getAllPOIs() {
        return POIs;
    }

    public String getId() {
        return id;
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
                json.get("author_uid").getAsString()
        );

        if (json.get("reviews") != null && !json.has("reviews")) {
            JsonArray reviewsJson = json.get("reviews").getAsJsonArray();
            for (JsonElement reviewJson : reviewsJson) {
              route.addReviewFromJson(reviewJson.getAsJsonObject());
            }
        }

        return new Route(
                json.get("route").getAsString(),
                json.get("title").getAsString(),
                json.get("description").getAsString(),
                POISs,
                json.get("id").getAsString(),
                json.get("author_uid").getAsString()
                );
    }

    public String getRoute() {
        return routeJson;
    }

    public String getIdToken() {
        return idToken;
    }

    public ArrayList<PointOfInterest> getPOIs() {
        return POIs;
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

    public void addReviews(Review review) {
        this.reviews.add(review);
    }

    public void addReviewFromJson(JsonObject json) {
        this.reviews.add(new Review(
                json.get("author_uid").getAsString(),
                json.get("msg").getAsString(),
                json.get("rate").getAsInt(),
                json.get("creation_timestamp").getAsString()
                ));
    }

    public ArrayList<Integer> getRates() {
        ArrayList<Integer> rates = new ArrayList<>();
        for (Review review : reviews) {
            rates.add(review.getRate());
        }

        return rates;
    }



    private static class Review {

        private final String authorUID;
        private final String msg;
//        private final ArrayList<PointOfInterest> POIs;
        private final int rate;
        private final String creationTimestamp;

        public Review(String authorUID, String msg, int rate, String creationTimestamp) {
            this.authorUID = authorUID;
            this.msg = msg;
            this.rate = rate;
            this.creationTimestamp = creationTimestamp;
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
    }


}


