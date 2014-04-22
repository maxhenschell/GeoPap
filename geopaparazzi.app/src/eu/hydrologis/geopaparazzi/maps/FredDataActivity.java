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
package eu.hydrologis.geopaparazzi.maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsManager;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DatabaseManager;

/**
 * Fred activity.
 * 
 * @author Andrea Antonello (hydrologis.eu)
 * @author Tim Howard (nynhp.org)
 */
public class FredDataActivity extends Activity {

    private static final String USE_MAPCENTER_POSITION = "USE_MAPCENTER_POSITION";
    private double latitude;
    private double longitude;
    private double elevation;
    private double mapCenterLatitude;
    private double mapCenterLongitude;
    private double mapCenterElevation;
    private double[] gpsLocation;

    private ToggleButton togglePositionTypeButtonGps;
    private ToggleButton writeDataButton;
    private Spinner lvlOneSpinner;
    private Spinner lvlTwoSpinner;

    private List<String> firstIDs; // first level information -- e.g. surveysite
    private List<String> secondIDs; // second level information -- e.g. obspoint
    private static String EXTERNAL_DB = "/mnt/sdcard/FRED/FRED.db";
    // private static String EXTERNAL_DB = "/FRED/FRED.db";
    private static String FIRST_LEVEL_TABLE = "IPAQ_surveysite";
    private static String COLUMN_FIRST_LEVEL_ID = "Site_ID";
    private static String SECOND_LEVEL_TABLE = "IPAQ_Obspts";
    private static String COLUMN_SECOND_LEVEL_ID = "Obspts_ID";
    private static String COLUMN_LAT = "latitude";
    private static String COLUMN_LON = "longitude";
    // private static String COLUMN_NOTE = "Comments";

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.fred_writecoords);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        togglePositionTypeButtonGps = (ToggleButton) findViewById(R.id.togglePositionTypeGps);
        togglePositionTypeButtonGps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                Editor edit = preferences.edit();
                edit.putBoolean(USE_MAPCENTER_POSITION, !isChecked);
                edit.commit();
            }
        });

        boolean useMapCenterPosition = preferences.getBoolean(USE_MAPCENTER_POSITION, false);

        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "position button set to: " + useMapCenterPosition); //$NON-NLS-1$

        double[] mapCenter = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
        mapCenterLatitude = mapCenter[1];
        mapCenterLongitude = mapCenter[0];
        // mapCenterElevation = 0.0;
        if (GpsManager.getInstance(this).hasFix()) {
            gpsLocation = PositionUtilities.getGpsLocationFromPreferences(preferences);
            // if (GPLog.LOG_HEAVY)
            //    GPLog.addLogEntry(this, "gpsLoc_lat: " + gpsLocation[1]); //$NON-NLS-1$
        }
        if (gpsLocation == null) {
            // no gps, can use only map center
            togglePositionTypeButtonGps.setChecked(false);
            togglePositionTypeButtonGps.setEnabled(false);
            Editor edit = preferences.edit();
            edit.putBoolean(USE_MAPCENTER_POSITION, false);
            edit.commit();
        } else {
            if (useMapCenterPosition) {
                togglePositionTypeButtonGps.setChecked(false);
            } else {
                togglePositionTypeButtonGps.setChecked(true);
            }
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // get the data to populate the spinners
        // EXTERNAL_DB = "/mnt/sdcard/FRED/FRED.db";

        final String fullDBPath = Environment.getExternalStorageDirectory().getPath() + "__" + EXTERNAL_DB;

        firstIDs = new ArrayList<String>();
        try {
            final SQLiteDatabase sqlDB = DatabaseManager.getInstance().getDatabase().openDatabase(EXTERNAL_DB, null, 2);
            firstIDs = getTableIDs(sqlDB, FIRST_LEVEL_TABLE, COLUMN_FIRST_LEVEL_ID, "Surveysite", "TIMESTAMP", null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        secondIDs = new ArrayList<String>();
        try {
            String firstIDsArrayFirstRow = firstIDs.get(0);
            int start = firstIDsArrayFirstRow.indexOf("(") + 1; // the ID should be the second
            String firstIDsID = firstIDsArrayFirstRow.substring(start, firstIDsArrayFirstRow.indexOf(", "));
            final SQLiteDatabase sqlDB = DatabaseManager.getInstance().getDatabase().openDatabase(EXTERNAL_DB, null, 2);
            secondIDs = getTableIDs(sqlDB, SECOND_LEVEL_TABLE, COLUMN_SECOND_LEVEL_ID, "Obspt", "TIMESTAMP", firstIDsID);
            // secondIDs = getTableIDs(sqlDB, SECOND_LEVEL_TABLE, COLUMN_SECOND_LEVEL_ID, "Obspt",
            // "TIMESTAMP",
            // null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Bundle extras = getIntent().getExtras();
        // String levelOne = extras.get(Constants.FRED_KEY_FIELD_LEVEL_ONE);
        // String levelTwo = extras.get(Constants.FRED_KEY_FIELD_LEVEL_TWO);
        // String levelThree = extras.get(Constants.FRED_KEY_FIELD_LEVEL_THREE);

        final TextView fredLocTextView = (TextView) findViewById(R.id.fredfrm_location);
        String locText = fredLocTextView.getText().toString();
        checkPositionCoordinates();
        locText = "Lat:" + latitude + ", Lon:" + longitude; //$NON-NLS-1$ //$NON-NLS-2$
        fredLocTextView.setText(locText);

        final TextView fredElevTextView = (TextView) findViewById(R.id.fredfrm_elevation);
        String elevText = fredElevTextView.getText().toString();
        elevText = "Elev:" + String.format("%.2f", elevation) + "m"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        fredElevTextView.setText(elevText);

        final EditText fredDestinationDB = (EditText) findViewById(R.id.fredfrm_destinationDB);
        EXTERNAL_DB = fredDestinationDB.getText().toString();

        // need to query IPAQ_surveysite table and fields Site_ID and Surveysite
        // need to query IPAQ_obspoints table and fields Site_ID, Obspts_ID, Obspt

        Button refreshButton = (Button) findViewById(R.id.refreshPosition);
        refreshButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                checkPositionCoordinates();
                String loc = "Lat:" + latitude + ", Lon:" + longitude; //$NON-NLS-1$ //$NON-NLS-2$
                fredLocTextView.setText(loc);
                String elev = "Elev:" + String.format("%.2f", elevation) + "m"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                fredElevTextView.setText(elev);
            }
        });

        Button avgButton = (Button) findViewById(R.id.fredfrm_avgbutton);
        avgButton.setEnabled(false);

        Button avgStopButton = (Button) findViewById(R.id.fredfrm_avgstopbutton);
        avgStopButton.setEnabled(false);

        writeDataButton = (ToggleButton) findViewById(R.id.fredfrm_writedataToggle);
        writeDataButton.setOnClickListener(new ToggleButton.OnClickListener(){
            public void onClick( View v ) {

                boolean IsWritten = false;
                writeDataButton.setChecked(IsWritten);

                final EditText edittextNote = (EditText) findViewById(R.id.fredfrm_notes);
                String note = edittextNote.getText().toString();
                edittextNote.setText(fullDBPath);

                String spinDat = (String) lvlOneSpinner.getSelectedItem();
                int start = spinDat.indexOf("(") + 1; // the ID should be the second
                String firstIDsID = spinDat.substring(start, spinDat.indexOf(", "));

                spinDat = (String) lvlTwoSpinner.getSelectedItem();
                start = spinDat.indexOf("(") + 1; // the ID should be the second
                String SecondIDsID = spinDat.substring(start, spinDat.indexOf(", "));

                String lat = String.format(Locale.getDefault(), "%.6f", latitude);
                String lon = String.format(Locale.getDefault(), "%.6f", longitude);

                final SQLiteDatabase sqlDB;

                try {
                    sqlDB = DatabaseManager.getInstance().getDatabase().openDatabase(EXTERNAL_DB, null, 2);
                    IsWritten = writeGpsData(firstIDsID, SecondIDsID, lat, lon, note, sqlDB);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                writeDataButton.setChecked(IsWritten);
            }
        });
        // TODO FIX THIS BUTTON
        Button returnButton = (Button) findViewById(R.id.fredfrm_returntofred);
        returnButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Intent intent = new Intent("com.syware.droiddb");
                intent.addFlags(intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(intent.FLAG_ACTIVITY_SINGLE_TOP);
                // intent.addFlags(intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("parameter", "FRED"); // this needs work, see description
                startActivity(intent);
            }
        });

        lvlOneSpinner = (Spinner) findViewById(R.id.fredfrm_levelonespinner);

        ArrayAdapter<String> lvlOneAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, firstIDs);
        lvlOneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lvlOneSpinner.setAdapter(lvlOneAdapter);
        lvlOneSpinner.setSelection(0);

        lvlTwoSpinner = (Spinner) findViewById(R.id.fredfrm_leveltwospinner);
        // try {

        ArrayAdapter<String> lvlTwoAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, secondIDs);
        lvlTwoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lvlTwoSpinner.setAdapter(lvlTwoAdapter);
        lvlTwoSpinner.setSelection(0);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

        lvlOneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            public void onItemSelected( AdapterView< ? > adapterView, View view, int i, long l ) {
                try {
                    // refresh data for second level spinner
                    String firstIDsArrayFirstRow = firstIDs.get(0);
                    int start = firstIDsArrayFirstRow.indexOf("(") + 1;
                    String firstIDsID = firstIDsArrayFirstRow.substring(start, firstIDsArrayFirstRow.indexOf(", "));
                    final SQLiteDatabase sqlDB = DatabaseManager.getInstance().getDatabase().openDatabase(EXTERNAL_DB, null, 2);
                    secondIDs = getTableIDs(sqlDB, SECOND_LEVEL_TABLE, COLUMN_SECOND_LEVEL_ID, "Obspt", "TIMESTAMP", firstIDsID);

                    ArrayAdapter<String> lvlTwoAdapter = new ArrayAdapter<String>(FredDataActivity.this,
                            android.R.layout.simple_spinner_item, secondIDs);
                    lvlTwoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    lvlTwoSpinner.setAdapter(lvlTwoAdapter);
                    lvlTwoSpinner.setSelection(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public void onNothingSelected( AdapterView< ? > adapterView ) {
                return;
            }
        });

    }

    // TODO need an onStart() for this activity!!!
    // TODO need an onPause() for this activity!!!
    // TODO need an onStop() for this activity!!!
    // TODO need an onResume() for this activity!!!

    /**
     * Checks to see whether to draw position from map or from GPS
     * 
     */
    private void checkPositionCoordinates() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useMapCenterPosition = preferences.getBoolean(USE_MAPCENTER_POSITION, false);
        if (useMapCenterPosition || gpsLocation == null) {
            latitude = mapCenterLatitude;
            longitude = mapCenterLongitude;
            elevation = mapCenterElevation;
        } else {
            latitude = gpsLocation[1];
            longitude = gpsLocation[0];
            elevation = gpsLocation[2];
        }
    }

    /**
     * Gets a list of values from a table. This sorts descending
     * by time so the most recently created record appears first
     * 
     * @param sqliteDatabase the DB to query
     * @param tableName the table to query
     * @param IdCol the ID column to grab
     * @param NameCol the name column to grab (site name, or other text field)
     * @param tsCol the time/date column 
     * @param strWhere if selecting a row, which ID value to select on
     * 
     * @throws IOException if a problem
     */
    private static List<String> getTableIDs( SQLiteDatabase sqliteDatabase, String tableName, String IdCol, String NameCol,
            String tsCol, String strWhere ) throws IOException {

        String asColumnsToReturn[] = {NameCol, IdCol, tsCol};
        String strSortOrder = tsCol + " DESC";
        if (strWhere != null) {
            strWhere = COLUMN_FIRST_LEVEL_ID + "=" + strWhere;
        }

        Cursor c = null;
        try {
            c = sqliteDatabase.query(tableName, asColumnsToReturn, strWhere, null, null, null, strSortOrder);
            int count = c.getCount();

            c.moveToFirst();
            List<String> NmIdTsList = new ArrayList<String>(count);
            while( !c.isAfterLast() ) {
                int fID = c.getInt(1);
                String fName = c.getString(0);
                String tStamp = c.getString(2);
                try {
                    String row = fName + " (" + String.valueOf(fID) + ", " + tStamp + ")";
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
        } finally {
            if (c != null)
                c.close();
        }
    }

    /**
     * Writes GPS data to an external database
     * 
     * @param lvlOneID the first level ID (surveysite)
     * @param lvlTwoID the second level ID (obspoint)
     * @param ddLon  decimal degrees longitude
     * @param ddLat decimal degrees latitude
     * @param note comments field to write to DB
     * @param sqlDB the DB to write to
     * @throws IOException if a problem
     */
    private static boolean writeGpsData( String lvlOneID, String lvlTwoID, String ddLat, String ddLon, String note,
            SQLiteDatabase sqlDB ) throws IOException {
        try {
            // final SQLiteDatabase sqlDB =
            // DatabaseManager.getInstance().getDatabase().openDatabase(sqlDatB, null, 2);
            sqlDB.beginTransaction();

            StringBuilder sb = new StringBuilder();
            sb = new StringBuilder();
            sb.append("UPDATE ");
            sb.append(SECOND_LEVEL_TABLE);
            sb.append(" SET ");
            sb.append(COLUMN_LAT).append("=").append(ddLat).append(", ");
            sb.append(COLUMN_LON).append("=").append(ddLon).append(" ");
            sb.append("WHERE ").append(COLUMN_FIRST_LEVEL_ID).append("=").append(lvlOneID).append(" ");
            sb.append("AND ").append(COLUMN_SECOND_LEVEL_ID).append("=").append(lvlTwoID);

            String query = sb.toString();
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("FredWriteQuery", query);
            SQLiteStatement sqlUpdate = sqlDB.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqlDB.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("FredWriteQuery", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqlDB.endTransaction();
        }
        return true;
    }

    /**
     * Opens the DroidDB app.
     * 
     * 
     */
    /*  public static void openDroidDB() {
          String droidDBAction = "com.syware.droiddb.VIEW";
          String droidDBPackage = "com.syware.droiddb";
          boolean hasDroidDB = false;
          List<PackageInfo> installedPackages = new ArrayList<PackageInfo>();

          if (GPLog.LOG_ABSURD)
              GPLog.addLogEntry("DroidDB", "Attempt app open ");
    */
    // { // try to get the installed packages list. Seems to have troubles over different
    // versions, so trying them all
    /*
      try {
          installedPackages = context.getPackageManager().getInstalledPackages(0);
      } catch (Exception e) {
          // ignore
      }
      if (installedPackages.size() == 0)
          try {
              installedPackages = context.getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES);
          } catch (Exception e) {
              // ignore
          }
    }

    if (installedPackages.size() > 0) {
      // if a list is available, check if the status gps is installed
      for( PackageInfo packageInfo : installedPackages ) {
          String packageName = packageInfo.packageName;
          if (GPLog.LOG_ABSURD)
              GPLog.addLogEntry("FRED", packageName);
          if (packageName.startsWith(droidDBPackage)) {
              hasDroidDB = true;
              if (GPLog.LOG_ABSURD)
                  GPLog.addLogEntry("ACTIONBAR", "Found package: " + packageName);
              break;
          }
      }
    } else {
    */
    /*
      * if no package list is available, for now try to fire it up anyways.
      * This has been a problem for a user on droidx with android 2.2.1.
      */
    /*     hasDroidDB = true;
     }

     if (hasDroidDB) {
         Intent intent = new Intent(droidDBAction);
         startActivity(intent);
     } else {
         new AlertDialog.Builder(context).setTitle(context.getString(R.string.installgpsstatus_title))
                 .setMessage("DroidDb is not installed?").setIcon(android.R.drawable.ic_dialog_info)
                 .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
                     public void onClick( DialogInterface dialog, int whichButton ) {
                         // ignore
                     }
                 }).setPositiveButton(android.R.string.ok, null).show();
     }

    }
    */
    // @Override
    // public void finish() {
    // updateWithNewValues();
    // super.finish();
    // }

    // private void updateWithNewValues() {
    // try {
    // DaoGpsLog.updateLogProperties(item.getId(), newColor, newWidth, item.isVisible(),
    // newText);
    // } catch (IOException e) {
    // GPLog.error(this, e.getLocalizedMessage(), e);
    // e.printStackTrace();
    // }
    // }

    // }
}