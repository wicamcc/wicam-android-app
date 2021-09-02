package co.armstart.openwicam;


import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

/**
 * Created by lyf on 2016-09-27.
 */

public class WicamApplication extends Application {
    private static Context context;
    public void onCreate() {
        super.onCreate();
        WicamApplication.context = getApplicationContext();
        Wicam.loadSavedWicams();
    }

    public static Context getAppContext() {
        return WicamApplication.context;
    }
    public static ContextWrapper getAppContextWrapper() {
        return (ContextWrapper) WicamApplication.context;
    }
}
