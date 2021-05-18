package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.app.Application;
import android.graphics.Bitmap;

import java.util.ArrayList;

public class SharedState extends Application {

    PointOfInterest viewingPOI = null;
    Route.Review editingReview = null;
    Route reviewingRoute = null;
    ArrayList<Bitmap> slideshowImages;

    private boolean isRenting = false;

    public boolean isRenting() {
        return isRenting;
    }

    public void setRenting(boolean renting) {
        isRenting = renting;
    }
}
