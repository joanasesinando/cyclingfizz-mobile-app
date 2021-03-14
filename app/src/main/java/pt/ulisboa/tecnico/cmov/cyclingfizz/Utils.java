package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.app.Activity;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public final class Utils {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static void setStatusBarColor(Activity activity, int color) {
        Window window = activity.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(activity,color));
    }
}
