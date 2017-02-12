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
package eu.geopaparazzi.core.database;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.GeoPoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.widget.Toast;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.core.database.objects.NoteOverlayItem;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 * modified by Tim Howard, NYNHP
 */
@SuppressWarnings("nls")
public class DaoFredPts {

    private static String EXTERNAL_DB = "EXTERNAL_DB";//$NON-NLS-1$
    private static String SECOND_LEVEL_TABLE = "SECOND_LEVEL_TABLE";//$NON-NLS-1$
    private static String COLUMN_LAT = "COLUMN_LAT";//$NON-NLS-1$
    private static String COLUMN_LON = "COLUMN_LON";//$NON-NLS-1$
    private static String COLUMN_NOTE = "COLUMN_NOTE";//$NON-NLS-1$

    /**
     * Get a list of points, for placing on the map, from the Fred DB and table currently in use
     *
     * @param context the context
     * @param marker  the marker to use.
     * @return the list of {@link OverlayItem}s
     * @throws IOException if something goes wrong.
     */
    // public static List<OverlayItem> getBookmarksOverlays( Drawable marker ) throws IOException {
    public static List<OverlayItem> getFredPtsOverlays(Context context, Drawable marker) throws IOException {
        // need to get prefs within context of call
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String extDB = prefs.getString(EXTERNAL_DB, "default");//$NON-NLS-1$
        final String childTable = prefs.getString(SECOND_LEVEL_TABLE, "default3"); //$NON-NLS-1$  
        final String colLat = prefs.getString(COLUMN_LAT, "default5"); //$NON-NLS-1$  
        final String colLon = prefs.getString(COLUMN_LON, "default6"); //$NON-NLS-1$  
        final String colNote = prefs.getString(COLUMN_NOTE, "default7"); //$NON-NLS-1$  

        List<OverlayItem> fredPts = new ArrayList<OverlayItem>();

        File file = new File(extDB);
        if (file.exists() && !file.isDirectory()) {
            SQLiteDatabase sqlDB = DatabaseManager.getInstance().getDatabase(context).openDatabase(extDB, null, 2);

            String query = "SELECT " + colLat + ", " + colLon + ", " + colNote + " FROM " + childTable;

            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry(context, "FredPts Query is " + query); //$NON-NLS-1$

            Cursor c = null;

            try {
                c = sqlDB.rawQuery(query, null);
                if (c != null) {
                    try {
                        c.moveToFirst();
                        while (!c.isAfterLast()) {
                            double lon = c.getDouble(1);
                            double lat = c.getDouble(0);
                            String text = c.getString(2);
                            text = text + "\n";
                            //GPLog.addLogEntry("fredPts","coords are lon lat: " + String.valueOf(lon) + " " + String.valueOf(lat));
                            try {
                                GeoPoint gp = new GeoPoint(lat, lon);
                                //label fredPt is used in GeopaparazziOverlay.java:483
                                OverlayItem pt = new OverlayItem(gp, "fredPt", text, marker);
                                //todo if we want labeled points, change to noteoverlayitem, get label via query, attach label
                                //todo as second item in NoteOverlayItem, below
                                //NoteOverlayItem pt = new NoteOverlayItem(gp, "", text, marker);
                                fredPts.add(pt);
                            } catch (IllegalArgumentException e) {
                                GPLog.addLogEntry(context, "Exception during Fred geopoints query and display");
                                GPDialogs.toast(context, "At least one Fred point out of possible range", Toast.LENGTH_SHORT);
                            }

                            c.moveToNext();
                        }

                    } finally {
                        c.close();
                    }
                } else {
                    GPDialogs.toast(context, "no Fred points to display", Toast.LENGTH_SHORT);
                }
            } catch (SQLiteException e) {
                //print and catch the exception
                GPLog.addLogEntry(context, "Exception during Fred points query and display");
                GPDialogs.toast(context, "Fred settings wrong", Toast.LENGTH_SHORT);
            }
            sqlDB.close();
        } else {
            GPDialogs.toast(context, "Fred DB settings wrong", Toast.LENGTH_SHORT);
            GPDialogs.toast(context, extDB + " does not exist", Toast.LENGTH_SHORT);
        }

        return fredPts;  // can be empty .. ok?
    }
}

