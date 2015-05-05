package eu.hydrologis.geopaparazzi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.util.LinkedHashMap;
import java.util.Map;

import eu.geopaparazzi.library.database.GPLog;
import eu.hydrologis.geopaparazzi.maps.MapsActivity;

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

    private static String whichFredDb = null;
    private static String idKey = null;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        // Get intent, action
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        String dataString = intent.getDataString();
        //int intentFlags = intent.getFlags();
        String fullIntent = intent.toString();
        String intentPackage = intent.getPackage();

        String extraParam = intent.getStringExtra("parameter");
        // parameter should map as "key: value; key: value; key: value" with or without spaces

        GPLog.addLogEntry(this, "GPFDDB " + extraParam);



        if (extraParam != null) {
            String[] extraParams = extraParam.split(";");

            for (int i = 0; i < extraParams.length; i++)
                extraParams[i] = extraParams[i].trim();

            Map<String, String> extraParsMap = new LinkedHashMap<String, String>();
            for (String keyValue : extraParams) {
                String[] pairs = keyValue.split(" *: *", 2);
                extraParsMap.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
            }
            // currently mapped items are DDB, Form, ID
            GPLog.addLogEntry(this, "GPFDDB " + extraParsMap);

//        for (String p:extraParams){
//            GPLog.addLogEntry(this, "GPFDDB extras " + p);
//        }
//
//        if (GPLog.LOG_HEAVY){
//            GPLog.addLogEntry(this, "GPFDDB maps boolean " + MapsActivity.created); //$NON-NLS-1$
//            GPLog.addLogEntry(this, "GPFDDB intent type " + type); //$NON-NLS-1$
//            GPLog.addLogEntry(this, "GPFDDB intent dataString " + dataString); //$NON-NLS-1$
//            GPLog.addLogEntry(this, "GPFDDB intent fullIntent " + fullIntent); //$NON-NLS-1$
//            GPLog.addLogEntry(this, "GPFDDB intent package " + intentPackage); //$NON-NLS-1$
//            GPLog.addLogEntry(this, "GPFDDB extra " + extraParam); //$NON-NLS-1$
//        }

            //set base preferences for fred

            if (extraParsMap.containsKey("DDB")) {
                whichFredDb = extraParsMap.get("DDB");
                setFredPrefs(whichFredDb);
            }

            if (extraParsMap.containsKey("ID")){
                idKey = extraParsMap.get("ID");
            }
        }

        // finally see what's open and send user to the correct spot
        checkMapsActivity();
    }

    private void checkMapsActivity() {
        /*
         * check to see if maps activity is running
         */

        if (MapsActivity.created) {
            //run maps activity here
            Intent intent = new Intent(this, MapsActivity.class);

            if (GPLog.LOG_HEAVY){
                GPLog.addLogEntry(this, "GPFDDB maps boolean " + MapsActivity.created); //$NON-NLS-1$
                GPLog.addLogEntry(this, "GPFDDB starting maps"); //$NON-NLS-1$
            }

            if (idKey != null){
                intent.putExtra("uid",idKey);
            }

            intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
        } else {
            Intent intent = new Intent(this, GeoPaparazziActivity.class);
            //Intent intent = new Intent(".GeoPaparazziActivity");
            if (GPLog.LOG_HEAVY){
                GPLog.addLogEntry(this, "GPFDDB maps boolean " + MapsActivity.created); //$NON-NLS-1$
                GPLog.addLogEntry(this, "GPFDDB starting main"); //$NON-NLS-1$
            }

            if (idKey != null){
                intent.putExtra("uid",idKey);
            }

            intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
        }

    }

    private void setFredPrefs(String ddbName){
    /*
     *    if info was shipped with the intent, set the prefs accordingly
     *
     *    ddbName is name of droid db database. Options: iMapField, fredEcol, fredBotZool
     */

        GPLog.addLogEntry(this, "GPFDDB ddb is " + ddbName);

        changeSettings(ddbName, this);

    }

    private void changeSettings(String quicksetChoice, Context context){
        // could not call FredQuickSets.java directly so repeating here, yuk.

        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String externalDB = baseDir + context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_external_db_path);
        String externalDBname = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_external_db_name);
        Boolean haveParentTable = Boolean.valueOf(context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_two_levels));
        String parentTable = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_first_level_table);
        String parentID = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_first_level_ID);
        String parentDescriptorField = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_first_level_descriptor);
        String parentTimeStamp = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_first_level_timestamp);
        String childTable = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_second_level_table);
        String childID = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_second_level_ID);
        String colLat = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_column_Lat);
        String colLon = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_column_Lon);
        String colNote = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_column_note);
        String childDescriptorField = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_second_level_descriptor);
        String childTimeStamp = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_second_level_timestamp);

        if (quicksetChoice.equals("iMapField")) { //$NON-NLS-1$
            externalDB = baseDir + context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_external_db_path);
            externalDBname = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_external_db_name);
            haveParentTable = Boolean.valueOf(context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_two_levels));
            parentTable = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_first_level_table);
            parentID = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_first_level_ID);
            parentDescriptorField = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_first_level_descriptor);
            parentTimeStamp = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_first_level_timestamp);
            childTable = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_second_level_table);
            childID = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_second_level_ID);
            colLat = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_column_Lat);
            colLon = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_column_Lon);
            colNote = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_column_note);
            childDescriptorField = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_second_level_descriptor);
            childTimeStamp = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_iMap_second_level_timestamp);
        } else if (quicksetChoice.equals("Fred-Ecology")) { //$NON-NLS-1$ //should be "Fred-Ecology"
            externalDB = baseDir + context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_external_db_path);
            externalDBname = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_external_db_name);
            haveParentTable = Boolean.valueOf(context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_two_levels));
            parentTable = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_first_level_table);
            parentID = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_first_level_ID);
            parentDescriptorField = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_first_level_descriptor);
            parentTimeStamp = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_first_level_timestamp);
            childTable = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_second_level_table);
            childID = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_second_level_ID);
            colLat = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_column_Lat);
            colLon = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_column_Lon);
            colNote = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_column_note);
            childDescriptorField = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_second_level_descriptor);
            childTimeStamp = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_second_level_timestamp);
        } else if (quicksetChoice.equals("Fred-Bot_Zool")) { //$NON-NLS-1$
            externalDB = baseDir + context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_external_db_path);
            externalDBname = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_external_db_name);
            haveParentTable = Boolean.valueOf(context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_two_levels));
            parentTable = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_first_level_table);
            parentID = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_first_level_ID);
            parentDescriptorField = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_first_level_descriptor);
            parentTimeStamp = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_first_level_timestamp);
            childTable = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_second_level_table);
            childID = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_second_level_ID);
            colLat = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_column_Lat);
            colLon = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_column_Lon);
            colNote = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_column_note);
            childDescriptorField = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_second_level_descriptor);
            childTimeStamp = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_second_level_timestamp);
        } else {
            // don't change anything
        }

        GPLog.addLogEntry(this, "GPFDDB external db is " + externalDB);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(EXTERNAL_DB, externalDB);
        editor.putString(EXTERNAL_DB_NAME, externalDBname);
        editor.putBoolean(TABLES_TWO_LEVELS, haveParentTable);
        editor.putString(FIRST_LEVEL_TABLE, parentTable);
        editor.putString(COLUMN_FIRST_LEVEL_ID, parentID);
        editor.putString(SECOND_LEVEL_TABLE, childTable);
        editor.putString(COLUMN_SECOND_LEVEL_ID, childID);
        editor.putString(COLUMN_LAT, colLat);
        editor.putString(COLUMN_LON, colLon);
        editor.putString(COLUMN_NOTE, colNote);
        editor.putString(COLUMN_FIRST_LEVEL_DESCRIPTOR, parentDescriptorField);
        editor.putString(COLUMN_FIRST_LEVEL_TIMESTAMP, parentTimeStamp);
        editor.putString(COLUMN_SECOND_LEVEL_DESCRIPTOR, childDescriptorField);
        editor.putString(COLUMN_SECOND_LEVEL_TIMESTAMP, childTimeStamp);
        editor.commit();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() { super.onResume(); }

    @Override
    protected void onStop() { super.onStop(); }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
