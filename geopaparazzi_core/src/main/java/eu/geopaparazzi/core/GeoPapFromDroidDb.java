package eu.geopaparazzi.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.LinkedHashMap;
import java.util.Map;

import eu.geopaparazzi.core.preferences.FredPreferences;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.core.mapview.MapviewActivity;


/**
 * A way to handle inconsistent activity opening in the activity stack
 * If intent comes from DroidDB, send it here and then choose which activity to begin
 * depending on the status of the stack.
 *
 * @author Tim Howard  12/22/2014
 */

public class GeoPapFromDroidDb extends Activity{

    private static boolean mapsActivityRunning = false;

    private static String EXTERNAL_DB = "EXTERNAL_DB";//$NON-NLS-1$
    private static String EXTERNAL_DB_NAME = "EXTERNAL_DB_NAME";//$NON-NLS-1$
    private static String FIRST_LEVEL_TABLE = "FIRST_LEVEL_TABLE";//$NON-NLS-1$
    private static String COLUMN_FIRST_LEVEL_ID = "COLUMN_FIRST_LEVEL_ID";//$NON-NLS-1$
    private static String SECOND_LEVEL_TABLE = "SECOND_LEVEL_TABLE";//$NON-NLS-1$
    private static String COLUMN_SECOND_LEVEL_ID = "COLUMN_SECOND_LEVEL_ID";//$NON-NLS-1$
    private static String TABLES_TWO_LEVELS = "TABLES_TWO_LEVELS";//$NON-NLS-1$
    private static String COLUMN_LAT = "COLUMN_LAT";//$NON-NLS-1$
    private static String COLUMN_LON = "COLUMN_LON";//$NON-NLS-1$
    private static String COLUMN_NOTE = "COLUMN_NOTE";//$NON-NLS-1$
    private static String COLUMN_FIRST_LEVEL_DESCRIPTOR = "COLUMN_FIRST_LEVEL_DESCRIPTOR";//$NON-NLS-1$
    private static String COLUMN_SECOND_LEVEL_DESCRIPTOR = "COLUMN_SECOND_LEVEL_DESCRIPTOR";//$NON-NLS-1$
    private static String COLUMN_FIRST_LEVEL_TIMESTAMP = "COLUMN_FIRST_LEVEL_TIMESTAMP";//$NON-NLS-1$
    private static String COLUMN_SECOND_LEVEL_TIMESTAMP = "COLUMN_SECOND_LEVEL_TIMESTAMP";//$NON-NLS-1$

    public static String whichFredDb = null;
    public static String idKey = null;
    public static String whichFredForm = null;
    public static String whichFredCallingActivity = null;
    public static String whichFredClass = null;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        // Get intent, action
        Intent intent = getIntent();
        String extraParam = intent.getStringExtra("parameter");
        // parameter should map as "key: value; key: value; key: value" with or without spaces

        GPLog.addLogEntry(this, "GPFDDB onCreate extra string " + extraParam);

        if (extraParam != null) {
            setFredMappings(extraParam, this);
        }

        // finally see what's open and send user to the correct spot
        checkMapsActivity();
    }

    private void checkMapsActivity() {
        /*
         * check to see if maps activity is running
         */

        if (MapviewActivity.created) {
            //run maps activity here
            Intent intent = new Intent(this, MapviewActivity.class);

            if (GPLog.LOG_HEAVY){
                GPLog.addLogEntry(this, "GPFDDB maps boolean " + MapviewActivity.created); //$NON-NLS-1$
                GPLog.addLogEntry(this, "GPFDDB starting maps"); //$NON-NLS-1$
            }
            intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
        } else {
            Intent intent = new Intent(this, GeopaparazziCoreActivity.class);
            if (GPLog.LOG_HEAVY){
                GPLog.addLogEntry(this, "GPFDDB maps boolean " + MapviewActivity.created); //$NON-NLS-1$
                GPLog.addLogEntry(this, "GPFDDB starting main"); //$NON-NLS-1$
            }
            intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
        }

    }

    private void setFredPrefs(String ddbName, Context context){
    /*
     *    if info was shipped with the intent, set the prefs accordingly
     *
     *    ddbName is name of droid db database. Options: fredEcol, fredBotZool, fredSurveysite
     */

        GPLog.addLogEntry(this, "GPFDDB ddb is " + ddbName);

        FredPreferences fredP = new FredPreferences();
        fredP.changeSettings(ddbName, context);

        Intent intent = new Intent(context,FredPreferences.class);
        intent.addFlags(intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
    }


    private void setFredMappings(String param, Context context){
    /*
     *    extract the extra parameters and assign them to constants.
     *    set preference
     *
     *    ddbName is name of droid db database. Options: fredEcol, fredBotZool, fredSurveysite
     */
        String[] extraParams = param.split(";");

        for (int i = 0; i < extraParams.length; i++)
            extraParams[i] = extraParams[i].trim();

        Map<String, String> extraParsMap = new LinkedHashMap<String, String>();
        for (String keyValue : extraParams) {
            String[] pairs = keyValue.split(" *: *", 2);
            extraParsMap.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
        }
        // currently mapped items are DDB, Form, ID
        GPLog.addLogEntry(context, "GPFDDB extraParsMap " + extraParsMap);


        if (extraParsMap.containsKey("DDB")) {
            whichFredDb = extraParsMap.get("DDB");
            setFredPrefs(whichFredDb, context);
        }

        GPLog.addLogEntry(context, "GPFDDB db is " + whichFredDb);


        if (extraParsMap.containsKey("ID")){
            idKey = extraParsMap.get("ID");
        }

        GPLog.addLogEntry(context, "GPFDDB idkey is " + idKey);

        if (extraParsMap.containsKey("Form")){
            whichFredForm = extraParsMap.get("Form");
        }

        GPLog.addLogEntry(context, "GPFDDB Form is " + whichFredForm);
    }



    @Override
    protected void onPause() {

        super.onPause();
        GPLog.addLogEntry(this, "GPFDDB onPause");
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        GPLog.addLogEntry(this, "GPFDDB onRestart");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String extraParam = intent.getStringExtra("parameter");
        // parameter should map as "key: value; key: value; key: value" with or without spaces
        GPLog.addLogEntry(this, "GPFDDB onNewIntent extra string " + extraParam);

        if (extraParam != null) {
            setFredMappings(extraParam, this);
        }

        // finally see what's open and send user to the correct spot
        checkMapsActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GPLog.addLogEntry(this, "GPFDDB onResume");
        Intent intent = getIntent();
        String extraParam = intent.getStringExtra("parameter");

        if (extraParam != null) {
            setFredMappings(extraParam, this);
        }
    }

    @Override
    protected void onStop() { super.onStop(); }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
