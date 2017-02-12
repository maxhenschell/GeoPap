/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 * Fred - integrated field data recording
 * Copyright (C) 2014 New York Natural Heritage Program (nynhp.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.geopaparazzi.core.mapview.overlays;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.core.GeoPapFromDroidDb;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DatabaseManager;
import eu.geopaparazzi.core.mapview.MapviewActivity;

/**
 * Fred activity.
 * 
 * @author Andrea Antonello (hydrologis.eu)
 * @author Tim Howard (nynhp.org)
 */

@TargetApi(11)

public class FredDataDirectActivity extends Activity {

    private static final String USE_MAPCENTER_POSITION = "USE_MAPCENTER_POSITION"; //$NON-NLS-1$
    private double latitude;
    private double longitude;
    private double elevation;
    private double gpsAccuracy;
    private String gpsAccuracyUnits;
    private int numberPointsSampled;
    private String gpsUnit;
    private String coordSource;
    private String recordID;
    //private double[] gpsLocation;
    //private BroadcastReceiver gpsBroadcastReceiver;

    private List<String> secondIDs; // second level information -- e.g. obspoint
    private static String EXTERNAL_DB = "EXTERNAL_DB";//$NON-NLS-1$
    private static String EXTERNAL_DB_NAME = "EXTERNAL_DB_NAME";//$NON-NLS-1$
    private static String SECOND_LEVEL_TABLE = "SECOND_LEVEL_TABLE";//$NON-NLS-1$
    private static String COLUMN_SECOND_LEVEL_ID = "COLUMN_SECOND_LEVEL_ID";//$NON-NLS-1$
    private static String COLUMN_LAT = "COLUMN_LAT";//$NON-NLS-1$
    private static String COLUMN_LON = "COLUMN_LON";//$NON-NLS-1$
    private static String COLUMN_ELEV = "COLUMN_ELEV";//$NON-NLS-1$
    private static String COLUMN_ACC = "COLUMN_ACC";//$NON-NLS-1$
    private static String COLUMN_ACC_UNITS = "COLUMN_ACC_UNITS";//$NON-NLS-1$
    private static String COLUMN_GPS_AVG_COUNT = "COLUMN_GPS_AVG_COUNT";
    private static String COLUMN_GPS_UNIT = "COLUMN_GPS_UNIT";
    private static String COLUMN_COORD_SOURCE = "COLUMN_COORD_SOURCE";//$NON-NLS-1$
    private static String COLUMN_SECOND_LEVEL_DESCRIPTOR = "COLUMN_SECOND_LEVEL_DESCRIPTOR";//$NON-NLS-1$
    private static String COLUMN_SECOND_LEVEL_TIMESTAMP = "COLUMN_SECOND_LEVEL_TIMESTAMP";//$NON-NLS-1$
    private static String PREFS_KEY_FRED_QUICK_SET = "PREFS_KEY_FRED_QUICK_SET";//$NON-NLS-1$


    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // get preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final String externalDB = preferences.getString(EXTERNAL_DB, "default"); //$NON-NLS-1$
        final String externalDBname = preferences.getString(EXTERNAL_DB_NAME, "default12"); //$NON-NLS-1$
        final String childTable = preferences.getString(SECOND_LEVEL_TABLE, "default3"); //$NON-NLS-1$
        final String childID = preferences.getString(COLUMN_SECOND_LEVEL_ID, "default4"); //$NON-NLS-1$
        final String colLat = preferences.getString(COLUMN_LAT, "default5"); //$NON-NLS-1$  
        final String colLon = preferences.getString(COLUMN_LON, "default6"); //$NON-NLS-1$
        final String colElev = preferences.getString(COLUMN_ELEV, "default7"); //$NON-NLS-1$
        final String colAcc = preferences.getString(COLUMN_ACC, "default8"); //$NON-NLS-1$
        final String colAccUnits = preferences.getString(COLUMN_ACC_UNITS, "default9"); //$NON-NLS-1$
        final String colGpsAvgCount = preferences.getString(COLUMN_GPS_AVG_COUNT, "default14");
        final String colGpsUnit = preferences.getString(COLUMN_GPS_UNIT, "default15");
        final String colCoordSo = preferences.getString(COLUMN_COORD_SOURCE, "default10"); //$NON-NLS-1$
        final String childDescriptorField = preferences.getString(COLUMN_SECOND_LEVEL_DESCRIPTOR, "default11"); //$NON-NLS-1$
        final String childTimeStamp = preferences.getString(COLUMN_SECOND_LEVEL_TIMESTAMP, "default12"); //$NON-NLS-1$
        final String quicksetChoice = preferences.getString(PREFS_KEY_FRED_QUICK_SET, "default13"); //$NON-NLS-1$

        // debug some of the defaults in case of problems
//        if (GPLog.LOG_HEAVY) {
//            GPLog.addLogEntry("fred", "prefs DB val: " + externalDB); //$NON-NLS-1$
//            GPLog.addLogEntry("fred", "prefs child table: " + childTable); //$NON-NLS-1$
//            GPLog.addLogEntry("fred", "prefs child ID: " + childID); //$NON-NLS-1$
//        }

        // Get intent, action
        Intent intent = getIntent();
        String intentType = intent.getStringExtra("type");

        recordID = GeoPapFromDroidDb.idKey;
        coordSource = intent.getStringExtra("coordSource");

        //GPLog.addLogEntry("fred","extra, type = " + intentType);
        //GPLog.addLogEntry("fred", "recordID is " + recordID); //$NON-NLS-1$

        // check if the location already has coordinates in Fred
        if (intentType.equals("checkForExistingLocation")){
            //GPLog.addLogEntry("fred", "checking if location already has gps data");
            boolean hasLocData = false;
            try {
                final SQLiteDatabase sqlDB;
                sqlDB = DatabaseManager.getInstance().getDatabase(FredDataDirectActivity.this)
                        .openDatabase(externalDB, null, 2);
                hasLocData = checkExistingGpsLoc(childTable, colLat,
                        childID, recordID, sqlDB);
                sqlDB.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Intent resultIntent = new Intent();

            if (hasLocData) {
                resultIntent.putExtra("hasLocData", true);
                resultIntent.putExtra("coordSource", coordSource);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                resultIntent.putExtra("hasLocData", false);
                resultIntent.putExtra("coordSource", coordSource);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }

        } else {
            GPLog.addLogEntry("fred", "writing location data");
            latitude = intent.getDoubleExtra(LibraryConstants.LATITUDE, 0.0);
            longitude = intent.getDoubleExtra(LibraryConstants.LONGITUDE, 0.0);
            elevation = intent.getDoubleExtra(LibraryConstants.ELEVATION, 0.0);
            gpsAccuracy = intent.getDoubleExtra("gpsAccuracy",-1.0);
            gpsAccuracyUnits = intent.getStringExtra("gpsAccuracyUnits");
            numberPointsSampled = intent.getIntExtra("numberPointsSampled",0);
            gpsUnit = "Tablet: " + Build.MODEL;

//        if (GPLog.LOG_HEAVY){
//            GPLog.addLogEntry(this, "Received intent action " + action); //$NON-NLS-1$
//            GPLog.addLogEntry(this, "Received intent type " + type); //$NON-NLS-1$
//        }

            // first off, check to see if dB exists
            final boolean dbExists = doesDatabaseExist(this, externalDB);
            if (!dbExists) {
                Toast.makeText(getApplicationContext(),
                        "Database does not exist", Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
                finish();
            } else if (recordID == null) {
                Toast.makeText(getApplicationContext(),
                        "No record ID, can't write point data", Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
                finish();
            } else {
                secondIDs = new ArrayList<String>();
                try {
                    final SQLiteDatabase sqlDB = DatabaseManager.getInstance().getDatabase(this)
                            .openDatabase(externalDB, null, 2);
                    boolean tableExists = doesTableExist(childTable, sqlDB);
                    if (tableExists) {
                        secondIDs = getTableIDs(sqlDB, childTable, childID, childDescriptorField, childTimeStamp, null, null, quicksetChoice);
                        // don't open form if no records
                        if (secondIDs.size() == 0) {
                            GPLog.addLogEntry(this, "Fred DB: no records 1 " + quicksetChoice);
                            GPDialogs.toast(this, "No records in DB - add one first", Toast.LENGTH_SHORT);
                            finish();
                        }
                    } else {
                        GPLog.addLogEntry(this, "Fred DB: no Table 2 " + childTable);
                        GPDialogs.toast(this, "Missing table in DB - check settings", Toast.LENGTH_SHORT);
                        finish();
                    }
                    sqlDB.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            boolean IsWritten = false;
            try {
                final SQLiteDatabase sqlDB;
                sqlDB = DatabaseManager.getInstance().getDatabase(FredDataDirectActivity.this)
                        .openDatabase(externalDB, null, 2);
                IsWritten = writeGpsData(childTable,childID,colLat, colLon, colElev, colAcc, colAccUnits, colCoordSo,
                        colGpsUnit, colGpsAvgCount, recordID, latitude, longitude, elevation, gpsAccuracy, gpsAccuracyUnits,
                        coordSource, gpsUnit, numberPointsSampled,
                        sqlDB);
                sqlDB.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (IsWritten) {
                Intent resultIntent = new Intent();
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        }
    }

    // TODO need an onStart() for this activity!!!
    // TODO need an onResume() for this activity!!!

    @Override
    public void onPause() {
        //maintain this boolean
        MapviewActivity.created = true;
        //GPLog.addLogEntry(this, "Pausing Fred ... MapsActivity.created =  " + MapviewActivity.created); //$NON-NLS-1$

        super.onPause();
    }

    @Override
    public void onStop() {
        //maintain this boolean
        MapviewActivity.created = true;
        //GPLog.addLogEntry(this, "Stopping Fred ... MapsActivity.created =  " + MapviewActivity.created); //$NON-NLS-1$

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //if (gpsBroadcastReceiver != null)
          //  GpsServiceUtilities.unregisterFromBroadcasts(this, gpsBroadcastReceiver);

        //maintain this boolean
        MapviewActivity.created = true;
        //GPLog.addLogEntry(this, "Destroying Fred ... MapsActivity.created =  " + MapviewActivity.created); //$NON-NLS-1$

        super.onDestroy();
    }

    /**
     * Gets a list of values from a table. This sorts descending
     * by time so the most recently created record appears first
     *
     * @param sqliteDatabase the DB to query
     * @param tableName      the table to query
     * @param IdCol          the ID column to grab
     * @param NameCol        the name column to grab (site name, or other text field)
     * @param tsCol          the time/date column
     * @param filterID       the column name on which to filter the list (parent table ID) using strWhere
     * @param strWhere       if selecting a row, which ID value to select on
     * @throws IOException if a problem
     */
    private static List<String> getTableIDs(SQLiteDatabase sqliteDatabase, String tableName, String IdCol, String NameCol,
                                            String tsCol, String filterID, String strWhere, String quickSet) throws IOException {

        //GPLog.addLogEntry("in getTableIDs, tableName= " + tableName);
        //GPLog.addLogEntry("in getTableIDs, IdCol= " + IdCol);
        //GPLog.addLogEntry("in getTableIDs, NameCol= " + NameCol);

        String asColumnsToReturn[] = {NameCol, IdCol, tsCol};
        String strSortOrder = tsCol + " DESC"; //$NON-NLS-1$
        if (strWhere != null) {
            strWhere = filterID + "=" + strWhere; //$NON-NLS-1$
        }

        Cursor c = null;
        try {
            c = sqliteDatabase.query(tableName, asColumnsToReturn, strWhere, null, null, null, strSortOrder);
            int count = c.getCount();

            c.moveToFirst();
            List<String> NmIdTsList = new ArrayList<String>(count);
            String row = "";
            while (!c.isAfterLast()) {
                String fID = c.getString(1); // to handle non-int IDs
                String fName = c.getString(0);
                String tStamp = c.getString(2);
                try {
                    //todo: customize for each type - does not work yet
                    if(quickSet == "Fred-Bot,Zool" && tableName == "IPAQ_SppSurvUtmList") {
                        row = "GPS ID = " + fName + " (created on " + tStamp + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    } else {
                        //String row = fName + " (" + String.valueOf(fID) + ", " + tStamp + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        row = fName + " (" + fID + ", " + tStamp + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    }
                    NmIdTsList.add(row);
                } catch (Exception e) {
                    // ignore invalid rows
                }
                c.moveToNext();

                if (c.isAfterLast()) {
                    break;
                }
            }
            return NmIdTsList;
        } catch (Exception e) {
            GPLog.error("FredReadQuery", e.getLocalizedMessage(), e); //$NON-NLS-1$
            throw new IOException(e.getLocalizedMessage());
        } finally {
            if (c != null)
                c.close();
        }
    }

    /**
     * Checks GPS data at the external database to see if location data (just latitude for simplicity) exists already
     *
     * @param tbl         is the name of the table to write to
     * @param colLat      is the column name for Latitude
     * @param colSecondID is the column name for the child ID column
     * @param lvlTwoID    the child ID for the record to update
     * @param sqlDB       the DB to write to
     * @throws IOException if a problem
     */

    private static boolean checkExistingGpsLoc(String tbl, String colLat, String colSecondID,
                                               String lvlTwoID, SQLiteDatabase sqlDB) throws IOException {

        GPLog.addLogEntry("fred", "checking for existing data at point");
        try {
            String query = "SELECT " + colLat + " FROM " + tbl + " WHERE " + colSecondID + "= '" + lvlTwoID + "'";
            Cursor cursor = sqlDB.rawQuery(query, null);
            //GPLog.addLogEntry("fred", "query: " + query);
            if(cursor!=null) {
                //GPLog.addLogEntry("fred", "cursor not null");
                //GPLog.addLogEntry("fred", "cursor count = " + cursor.getCount());
                //GPLog.addLogEntry("fred", "cursor cols = " + cursor.getColumnCount());
                //GPLog.addLogEntry("fred", "cursor cols = " + cursor.getColumnName(0));
                if(cursor.getCount()>0) {
                    cursor.moveToFirst();
                    cursor.moveToLast();
                    Double lat = cursor.getDouble(0);
                    //Double lat = cursor.getDouble(cursor.getColumnIndex(colLat));
                    //GPLog.addLogEntry("fred", "lat = " + lat);
                    if(lat > 0){
                        //GPLog.addLogEntry("fred", "coords existing for this point");
                        cursor.close();
                        return true;
                    }
                }
                cursor.close();
            }

        } catch (Exception e) {
            GPLog.error("FredSelectQuery", e.getLocalizedMessage(),e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqlDB.close();
        }
    return false;
    }

    /**
     * Writes GPS data to an external database
     *
     * @param tbl         is the name of the table to write to
     * @param colSecondID is the column name for the child ID column
     * @param colLat      is the column name for Latitude
     * @param colLon      is the column name for Longitude
     * @param colElev     is the column name for elevation
     * @param colAcc      is the column name for point accuracy
     * @param colAccUnits is the column name for accuracy units
     * @param colCoordSo  is the column name for how coordinates were obtained (gps, map, gps avg)
     * @param colGpsUnit  is the column name for gps unit
     * @param colAvgCount is the column name for gps averaging count
     * @param lvlTwoID    the child ID for the record to update
     * @param ddLon       decimal degrees longitude
     * @param ddLat       decimal degrees latitude
     * @param elev          elevation
     * @param gpsAcc        gps accuracy as reported by gps
     * @param gpsAccUnits   gps accuracy units
     * @param coordSo       coordinates source, manual from crosshairs on map or gps or gps average?
     * @param gpsUnit       the name of the gps unit used
     * @param numPtsSampled number of points sampled when gps averaging
     * @param sqlDB       the DB to write to
     * @throws IOException if a problem
     */
    private static boolean writeGpsData(String tbl, String colSecondID,
                                        String colLat, String colLon,String colElev, String colAcc, String colAccUnits, String colCoordSo,
                                        String colGpsUnit, String colAvgCount,
                                        String lvlTwoID,
                                        double ddLat, double ddLon, double elev, double gpsAcc, String gpsAccUnits, String coordSo,
                                        String gpsUnit, int numPtsSampled,
                                        SQLiteDatabase sqlDB) throws IOException {

        try {
            sqlDB.beginTransaction();
            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE "); //$NON-NLS-1$
            sb.append(tbl);
            sb.append(" SET "); //$NON-NLS-1$
            sb.append(colLat).append("=").append(ddLat).append(", "); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(colLon).append("=").append(ddLon).append(", "); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(colElev).append("=").append(elev).append(", "); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(colAcc).append("=").append(gpsAcc).append(", "); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(colAccUnits).append("= '").append(gpsAccUnits).append("', "); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(colCoordSo).append("= '").append(coordSo).append("', "); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(colGpsUnit).append("= '").append(gpsUnit).append("', "); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(colAvgCount).append("= '").append(numPtsSampled).append("' "); //$NON-NLS-1$ //$NON-NLS-2$

            sb.append("WHERE ").append(colSecondID).append("= '").append(lvlTwoID).append("'"); //$NON-NLS-1$ //$NON-NLS-2$

            String query = sb.toString();
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("FredWriteQuery", query); //$NON-NLS-1$
            SQLiteStatement sqlUpdate = sqlDB.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();
            sqlDB.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("FredWriteQuery", e.getLocalizedMessage(), e); //$NON-NLS-1$
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqlDB.endTransaction();
            sqlDB.close();
        }
        return true;
    }

    /**
     * Check to see if DB exists
     *
     * @param context is the context
     * @param dbName  is the name of the database to check
     */
    private static boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

    /**
     * Check to see if a table in the DB exists
     *
     * @param tableName is the name of the table
     * @param sqlDb  is the name of the database
     */
    public boolean doesTableExist(String tableName, SQLiteDatabase sqlDb) {
        Cursor cursor = sqlDb.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
        if(cursor!=null) {
            if(cursor.getCount()>0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }
}