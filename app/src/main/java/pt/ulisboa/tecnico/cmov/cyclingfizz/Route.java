package pt.ulisboa.tecnico.cmov.cyclingfizz;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

public class Route {

    private final String routeJson;
    private final String idToken;
    private final ArrayList<PointOfInterest> POIs;
    private final String title;
    private final String description;

    static String SERVER_URL = "https://stations.cfservertest.ga";
    static String TAG = "Cycling_Fizz@Route";

    public Route(String routeJson, String idToken, String title, String description, ArrayList<PointOfInterest> POIs) {
        this.routeJson = routeJson;
        this.idToken = idToken;
        this.title = title;
        this.description = description;
        this.POIs = POIs;
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

    public static Route fromJson(JsonObject json) {

        ArrayList<PointOfInterest> POISs = new ArrayList<>();

        for (JsonElement jsonElement : json.get("POIs").getAsJsonArray()) {
            POISs.add(PointOfInterest.fromJson(jsonElement.getAsJsonObject()));
        }

        return new Route(
                json.get("route").getAsString(),
                json.get("author_uid").getAsString(),
                json.get("title").getAsString(),
                json.get("description").getAsString(),
                POISs
                );
    }


}

