package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.app.Application;

public class SharedState extends Application {

    // FIXME: remove renting from here and create singleton
    // FIXME: add general stuff here like server URL

    private boolean isRenting = false;

    public boolean isRenting() {
        return isRenting;
    }

    public void setRenting(boolean renting) {
        isRenting = renting;
    }
}
