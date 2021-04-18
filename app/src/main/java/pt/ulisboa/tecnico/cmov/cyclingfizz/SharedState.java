package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.app.Application;

public class SharedState extends Application {

    private boolean isRenting = false;

    public boolean isRenting() {
        return isRenting;
    }

    public void setRenting(boolean renting) {
        isRenting = renting;
    }
}
