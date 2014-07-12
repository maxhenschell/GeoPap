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
package eu.hydrologis.geopaparazzi.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.GeoPoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import eu.geopaparazzi.library.database.GPLog;
//import android.content.BroadcastReceiver;
//import android.database.sqlite.SQLiteStatement;
//import eu.hydrologis.geopaparazzi.GeopaparazziApplication;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoFredPts {

    private static String EXTERNAL_DB = "EXTERNAL_DB";//$NON-NLS-1$
    private static String SECOND_LEVEL_TABLE = "SECOND_LEVEL_TABLE";//$NON-NLS-1$
    private static String COLUMN_LAT = "COLUMN_LAT";//$NON-NLS-1$
    private static String COLUMN_LON = "COLUMN_LON";//$NON-NLS-1$
    private static String COLUMN_NOTE = "COLUMN_NOTE";//$NON-NLS-1$

    /**
     * Get the collected notes from the database inside a given bound.
     * 
     * @param n north 
     * @param s south
     * @param w west 
     * @param e east
     * 
     * @return the list of notes inside the bounds.
     * @throws IOException  if something goes wrong.
     */
    /**
     public List<Bookmark> getBookmarksInWorldBounds( float n, float s, float w, float e ) throws IOException {

         SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
         String query = "SELECT _id, lon, lat, text FROM XXX WHERE (lon BETWEEN XXX AND XXX) AND (lat BETWEEN XXX AND XXX)";
         // String[] args = new String[]{TABLE_NOTES, String.valueOf(w), String.valueOf(e),
         // String.valueOf(s), String.valueOf(n)};

         query = query.replaceFirst("XXX", childTable);
         query = query.replaceFirst("XXX", String.valueOf(w));
         query = query.replaceFirst("XXX", String.valueOf(e));
         query = query.replaceFirst("XXX", String.valueOf(s));
         query = query.replaceFirst("XXX", String.valueOf(n));

         // Logger.i("DAOBOOKMARKS", "Query: " + query);

         Cursor c = sqliteDatabase.rawQuery(query, null);
         List<Bookmark> bookmarks = new ArrayList<Bookmark>();
         c.moveToFirst();
         while( !c.isAfterLast() ) {
             long id = c.getLong(0);
             double lon = c.getDouble(1);
             double lat = c.getDouble(2);
             String text = c.getString(3);

             Bookmark note = new Bookmark(id, text, lon, lat);
             bookmarks.add(note);
             c.moveToNext();
         }
         c.close();
         return bookmarks;
     }
    */
    /**
     * Get a list of points, for placing on the map, from the Fred DB and table currently in use
     * 
     * @param context the context
     * @param marker the marker to use.
     * @return the list of {@link OverlayItem}s
     * @throws IOException  if something goes wrong.
     */
    // public static List<OverlayItem> getBookmarksOverlays( Drawable marker ) throws IOException {
    public static List<OverlayItem> getFredPtsOverlays( Context context, Drawable marker ) throws IOException {
        // need to get prefs within context of call
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String extDB = prefs.getString(EXTERNAL_DB, "default");//$NON-NLS-1$
        final String childTable = prefs.getString(SECOND_LEVEL_TABLE, "default3"); //$NON-NLS-1$  
        final String colLat = prefs.getString(COLUMN_LAT, "default5"); //$NON-NLS-1$  
        final String colLon = prefs.getString(COLUMN_LON, "default6"); //$NON-NLS-1$  
        final String colNote = prefs.getString(COLUMN_NOTE, "default7"); //$NON-NLS-1$  

        SQLiteDatabase sqlDB = DatabaseManager.getInstance().getDatabase(context).openDatabase(extDB, null, 2);

        String query = "SELECT " + colLat + ", " + colLon + ", " + colNote + " FROM " + childTable;

        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(context, "FredPts Query is " + query); //$NON-NLS-1$

        Cursor c = null;
        try {
            c = sqlDB.rawQuery(query, null);
            List<OverlayItem> fredPts = new ArrayList<OverlayItem>();
            c.moveToFirst();
            while( !c.isAfterLast() ) {
                double lon = c.getDouble(0);
                double lat = c.getDouble(1);
                String text = c.getString(2);
                text = text + "\n";
                OverlayItem pt = new OverlayItem(new GeoPoint(lat, lon), null, text, marker);
                fredPts.add(pt);
                c.moveToNext();
            }
            return fredPts;
        } finally {
            if (c != null)
                c.close();
        }
    }
}
