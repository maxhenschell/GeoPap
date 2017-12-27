/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
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
package eu.hydrologis.geopaparazzi.preferences;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Environment;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;

/**
 * A spinner within preferences, modified from
 * Andrea Antonello's custom sdcard path chooser.
 * 
 * 
 * @author Tim Howard
 *
 */
public class FredQuickSets extends DialogPreference {
    private Context context;
    private String quicksetChoice = ""; //$NON-NLS-1$
    private Spinner quicksetChoicesSpinner;

    private static String EXTERNAL_DB = "EXTERNAL_DB";//$NON-NLS-1$
    private static String EXTERNAL_DB_NAME = "EXTERNAL_DB_NAME";//$NON-NLS-1$
    private static String FIRST_LEVEL_TABLE = "FIRST_LEVEL_TABLE";//$NON-NLS-1$
    private static String COLUMN_FIRST_LEVEL_ID = "COLUMN_FIRST_LEVEL_ID";//$NON-NLS-1$
    private static String SECOND_LEVEL_TABLE = "SECOND_LEVEL_TABLE";//$NON-NLS-1$
    private static String COLUMN_SECOND_LEVEL_ID = "COLUMN_SECOND_LEVEL_ID";//$NON-NLS-1$
    private static String TABLES_TWO_LEVELS = "TABLES_TWO_LEVELS";//$NON-NLS-1$
    private static String COLUMN_LAT = "COLUMN_LAT";//$NON-NLS-1$
    private static String COLUMN_LON = "COLUMN_LON";//$NON-NLS-1$
    private static String COLUMN_ACC = "COLUMN_ACC";
    private static String COLUMN_ACC_UNITS = "COLUMN_ACC_UNITS";
    private static String COLUMN_COORD_SOURCE = "COLUMN_COORD_SOURCE";
    private static String COLUMN_ELEV = "COLUMN_ELEV";
    private static String COLUMN_GPS_UNIT = "COLUMN_GPS_UNIT";
    private static String COLUMN_GPS_AVG_TIME_MIN = "COLUMN_GPS_AVG_TIME_MIN";
    private static String COLUMN_GPS_AVG_TIME_SEC = "COLUMN_GPS_AVG_TIME_SEC";
    private static String COLUMN_GPS_AVG_COUNT = "COLUMN_GPS_AVG_COUNT";
    private static String COLUMN_PDOP = "COLUMN_PDOP";
    private static String COLUMN_NOTE = "COLUMN_NOTE";//$NON-NLS-1$
    private static String COLUMN_FIRST_LEVEL_DESCRIPTOR = "COLUMN_FIRST_LEVEL_DESCRIPTOR";//$NON-NLS-1$
    private static String COLUMN_SECOND_LEVEL_DESCRIPTOR = "COLUMN_SECOND_LEVEL_DESCRIPTOR";//$NON-NLS-1$
    private static String COLUMN_FIRST_LEVEL_TIMESTAMP = "COLUMN_FIRST_LEVEL_TIMESTAMP";//$NON-NLS-1$
    private static String COLUMN_SECOND_LEVEL_TIMESTAMP = "COLUMN_SECOND_LEVEL_TIMESTAMP";//$NON-NLS-1$

    /**
     * @param ctxt  the context to use.
     * @param attrs attributes.
     */
    public FredQuickSets( Context ctxt, AttributeSet attrs ) {
        super(ctxt, attrs);
        this.context = ctxt;
        setPositiveButtonText(ctxt.getString(android.R.string.ok));
        setNegativeButtonText(ctxt.getString(android.R.string.cancel));
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout mainLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        mainLayout.setLayoutParams(layoutParams);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        TextView comboLabelView = new TextView(context);
        comboLabelView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        comboLabelView.setPadding(2, 2, 2, 2);
        comboLabelView.setText(R.string.fred_choose_settings);
        mainLayout.addView(comboLabelView);

        quicksetChoicesSpinner = new Spinner(context);
        quicksetChoicesSpinner
                .setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        quicksetChoicesSpinner.setPadding(15, 5, 15, 5);
        mainLayout.addView(quicksetChoicesSpinner);

        ArrayList<String> quicksetChoicesList = new ArrayList<String>();

        quicksetChoicesList.add("Fred-Surveysite"); //$NON-NLS-1$
        quicksetChoicesList.add("Fred-Ecology"); //$NON-NLS-1$
        quicksetChoicesList.add("Fred-Bot_Zool"); //$NON-NLS-1$

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
                quicksetChoicesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        quicksetChoicesSpinner.setAdapter(adapter);
        if (quicksetChoice != null) {
            for( int i = 0; i < quicksetChoicesList.size(); i++ ) {
                if (quicksetChoicesList.get(i).equals(quicksetChoice.trim())) {
                    quicksetChoicesSpinner.setSelection(i);
                    break;
                }
            }
        }

        return mainLayout;
    }
    /*
    @Override
    protected void onBindDialogView( View v ) {
        super.onBindDialogView(v);

        editView.setText(quicksetChoice);
        if (quicksetChoice != null) {
            for( int i = 0; i < quicksetChoicesList.size(); i++ ) {
                if (quicksetChoicesList.get(i).equals(quicksetChoice.trim())) {
                    quicksetChoicesSpinner.setSelection(i);
                    break;
                }
            }
        }
    }
    */
    @Override
    protected void onDialogClosed( boolean positiveResult ) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            quicksetChoice = quicksetChoicesSpinner.getSelectedItem().toString();
        }
        if (callChangeListener(quicksetChoice)) {
            persistString(quicksetChoice);
        }

        changeSettings(quicksetChoice, context);

    }
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index ) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue( boolean restoreValue, Object defaultValue ) {

        if (restoreValue) {
            if (defaultValue == null) {
                quicksetChoice = getPersistedString(""); //$NON-NLS-1$
            } else {
                quicksetChoice = getPersistedString(defaultValue.toString());
            }
        } else {
            quicksetChoice = defaultValue.toString();
        }
    }

    private void changeSettings(String quicksetChoice, Context context){

        // get the base path
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        // set defaults to something real
        String externalDB = baseDir + context.getString(R.string.fred_defval_external_db_path);
        String externalDBname = context.getString(R.string.fred_defval_external_db_name);
        Boolean haveParentTable = Boolean.valueOf(context.getString(R.string.fred_defval_two_levels));
        String parentTable = context.getString(R.string.fred_defval_first_level_table);
        String parentID = context.getString(R.string.fred_defval_first_level_ID);
        String parentDescriptorField = context.getString(R.string.fred_defval_first_level_descriptor);
        String parentTimeStamp = context.getString(R.string.fred_defval_first_level_timestamp);
        String childTable = context.getString(R.string.fred_defval_second_level_table);
        String childID = context.getString(R.string.fred_defval_second_level_ID);
        String colLat = context.getString(R.string.fred_defval_column_Lat);
        String colLon = context.getString(R.string.fred_defval_column_Lon);
        String colAcc = context.getString(R.string.fred_defval_column_acc);
        String colAccUnits = context.getString(R.string.fred_defval_column_acc_units);
        String colCoordSo = context.getString(R.string.fred_defval_column_coord_source);
        String colElev = context.getString(R.string.fred_defval_column_elev);
        String colGpsUnit = context.getString(R.string.fred_defval_column_gps_unit);
        String colGpsAvgTimeMinutes = context.getString(R.string.fred_defval_column_gps_average_time_min);
        String colGpsAvgTimeSeconds = context.getString(R.string.fred_defval_column_gps_average_time_sec);
        String colGpsAvgCount = context.getString(R.string.fred_defval_column_gps_avg_count);
        String colPdop = context.getString(R.string.fred_defval_column_Pdop);
        String colNote = context.getString(R.string.fred_defval_column_note);
        String childDescriptorField = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_second_level_descriptor);
        String childTimeStamp = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_defval_second_level_timestamp);

        if (quicksetChoice.equals("Fred-Surveysite")) { //$NON-NLS-1$
            externalDB = baseDir + context.getString(eu.geopaparazzi.core.R.string.fred_SS_external_db_path);
            externalDBname = context.getString(eu.geopaparazzi.core.R.string.fred_SS_external_db_name);
            haveParentTable = Boolean.valueOf(context.getString(eu.geopaparazzi.core.R.string.fred_SS_two_levels));
            parentTable = context.getString(eu.geopaparazzi.core.R.string.fred_SS_first_level_table);
            parentID = context.getString(eu.geopaparazzi.core.R.string.fred_SS_first_level_ID);
            parentDescriptorField = context.getString(eu.geopaparazzi.core.R.string.fred_SS_first_level_descriptor);
            parentTimeStamp = context.getString(eu.geopaparazzi.core.R.string.fred_SS_first_level_timestamp);
            childTable = context.getString(eu.geopaparazzi.core.R.string.fred_SS_second_level_table);
            childID = context.getString(eu.geopaparazzi.core.R.string.fred_SS_second_level_ID);
            colLat = context.getString(eu.geopaparazzi.core.R.string.fred_SS_column_Lat);
            colLon = context.getString(eu.geopaparazzi.core.R.string.fred_SS_column_Lon);
            colNote = context.getString(eu.geopaparazzi.core.R.string.fred_SS_column_note);
            childDescriptorField = context.getString(eu.geopaparazzi.core.R.string.fred_SS_second_level_descriptor);
            childTimeStamp = context.getString(eu.geopaparazzi.core.R.string.fred_SS_second_level_timestamp);
        } else if (quicksetChoice.equals("Fred-Ecology")) { //$NON-NLS-1$
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
            colAcc = context.getString(R.string.fred_defval_column_acc);
            colAccUnits = context.getString(R.string.fred_defval_column_acc_units);
            colCoordSo = context.getString(R.string.fred_defval_column_coord_source);
            colElev = context.getString(R.string.fred_defval_column_elev);
            colGpsUnit = context.getString(R.string.fred_defval_column_gps_unit);
            colGpsAvgTimeMinutes = context.getString(R.string.fred_defval_column_gps_average_time_min);
            colGpsAvgTimeSeconds = context.getString(R.string.fred_defval_column_gps_average_time_sec);
            colGpsAvgCount = context.getString(R.string.fred_defval_column_gps_avg_count);
            colPdop = context.getString(R.string.fred_defval_column_Pdop);
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
            colAcc = context.getString(R.string.fred_BotZoo_column_acc);
            colAccUnits = context.getString(R.string.fred_BotZoo_column_acc_units);
            colCoordSo = context.getString(R.string.fred_BotZoo_column_coord_source);
            colElev = context.getString(R.string.fred_BotZoo_column_elev);
            colGpsUnit = context.getString(R.string.fred_BotZoo_column_gps_unit);
            colGpsAvgTimeMinutes = context.getString(R.string.fred_BotZoo_column_gps_average_time_min);
            colGpsAvgTimeSeconds = context.getString(R.string.fred_BotZoo_column_gps_average_time_sec);
            colGpsAvgCount = context.getString(R.string.fred_BotZoo_column_gps_avg_count);
            colPdop = context.getString(R.string.fred_BotZoo_column_Pdop);
            colNote = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_column_note);
            childDescriptorField = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_second_level_descriptor);
            childTimeStamp = context.getString(eu.hydrologis.geopaparazzi.R.string.fred_BotZoo_second_level_timestamp);
        } else {
            // don't change anything
        }

        SharedPreferences prefs = this.getSharedPreferences();
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
        editor.putString(COLUMN_ACC, colAcc);
        editor.putString(COLUMN_ACC_UNITS,colAccUnits);
        editor.putString(COLUMN_COORD_SOURCE,colCoordSo);
        editor.putString(COLUMN_ELEV,colElev);
        editor.putString(COLUMN_GPS_UNIT,colGpsUnit);
        editor.putString(COLUMN_GPS_AVG_TIME_MIN,colGpsAvgTimeMinutes);
        editor.putString(COLUMN_GPS_AVG_TIME_SEC,colGpsAvgTimeSeconds);
        editor.putString(COLUMN_GPS_AVG_COUNT,colGpsAvgCount);
        editor.putString(COLUMN_PDOP,colPdop);
        editor.putString(COLUMN_NOTE, colNote);
        editor.putString(COLUMN_FIRST_LEVEL_DESCRIPTOR, parentDescriptorField);
        editor.putString(COLUMN_FIRST_LEVEL_TIMESTAMP, parentTimeStamp);
        editor.putString(COLUMN_SECOND_LEVEL_DESCRIPTOR, childDescriptorField);
        editor.putString(COLUMN_SECOND_LEVEL_TIMESTAMP, childTimeStamp);
        editor.commit();

    }
}