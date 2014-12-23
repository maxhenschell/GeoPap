package eu.hydrologis.geopaparazzi;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import eu.geopaparazzi.library.database.GPLog;
import eu.hydrologis.geopaparazzi.maps.MapsActivity;

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_DATABASE_TO_LOAD;

/**
 * A way to handle inconsistent activity opening in the activity stack
 * If intent comes from DroidDB, send it here and then choose which activity to begin
 * depending on the status of the stack.
 *
 * @author Tim Howard  12/22/2014
 */

public class GeoPapFromDroidDb extends Activity{

    private static boolean mapsActivityRunning = false;

    public void onCreate(Bundle savedInstanceState) {



        // Get intent, action
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        String dataString = intent.getDataString();
        //int intentFlags = intent.getFlags();
        String fullIntent = intent.toString();
        String intentPackage = intent.getPackage();

        if (GPLog.LOG_HEAVY){
            GPLog.addLogEntry(this, "GPFDDB maps boolean " + MapsActivity.created); //$NON-NLS-1$
            GPLog.addLogEntry(this, "GPFDDB intent type " + type); //$NON-NLS-1$
            GPLog.addLogEntry(this, "GPFDDB intent dataString " + dataString); //$NON-NLS-1$
            GPLog.addLogEntry(this, "GPFDDB intent fullIntent " + fullIntent); //$NON-NLS-1$
            GPLog.addLogEntry(this, "GPFDDB intent package " + intentPackage); //$NON-NLS-1$
        }

        checkMapsActivity();
    }

    private void checkMapsActivity() {
        /*
         * check to see if maps activity is running
         */

        if (MapsActivity.created) {
            //run maps activity here
            Intent intent = new Intent("eu.hydrologis.geopaparazzi.maps.MAP_VIEW");
            this.startActivity(intent);
        } else {
            Intent intent = new Intent("eu.hydrologis.geopaparazzi.GeoPaparazziActivity");
            this.startActivity(intent);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // avoid oncreate call when rotating device
        super.onConfigurationChanged(newConfig);
    }

}
