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
package eu.geopaparazzi.library.gps;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import eu.geopaparazzi.library.database.GPLog;

//import static eu.geopaparazzi.library.gps.GpsService.GPS_AVG_COMPLETE;
//import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_AVERAGED_POSITION;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_BROADCAST_NOTIFICATION;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_DO_BROADCAST;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_GPSSTATUS_EXTRAS;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_POSITION;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_POSITION_EXTRAS;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_POSITION_TIME;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_STATUS;
//import static eu.geopaparazzi.library.gps.GpsService.START_GPS_AVERAGING;
import static eu.geopaparazzi.library.gps.GpsService.START_GPS_CONTINUE_LOG;
import static eu.geopaparazzi.library.gps.GpsService.START_GPS_LOGGING;
import static eu.geopaparazzi.library.gps.GpsService.START_GPS_LOG_HELPER_CLASS;
import static eu.geopaparazzi.library.gps.GpsService.START_GPS_LOG_NAME;
import static eu.geopaparazzi.library.gps.GpsService.STOP_GPS_LOGGING;

//import static eu.geopaparazzi.library.gps.GpsService.*;

/**
 * 
 * A gps averaging utils class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsAvgUtilities {

    /**
     * Start the service.
     *
     * @param activity the activity to use.
     */
    public static void startGpsAvgService( Activity activity ) {
        GPLog.addLogEntry("GPSAVG","In GpsAvgUtilities start service");
        Intent intent = new Intent(activity, GpsAvgService.class);
        activity.startService(intent);
    }

    /**
     * Utility to get the position/gps average from an intent.
     *
     * @param intent the intent.
     * @return the position as lon, lat, elev.
     */
    public static double[] getPositionAverage( Intent intent ) {
        GPLog.addLogEntry("GPSAVG","In GpsAvgUtilities getPositionAvg");
        if (intent == null) {
            return null;
        }
        double[] position = intent.getDoubleArrayExtra("GPS_SERVICE_AVERAGED_POSITION");
        return position;
    }


//    /**
//     * Utility to get the {@link getGpsAveragingStatus} from an intent.
//     *
//     * @param intent the intent.
//     * @return the status.
//     */
//    public static GpsAvgStatus getGpsAveragingStatus( Intent intent ) {
//        if (intent == null) {
//            return null;
//        }
//        int getGpsAveragingStatusCode = intent.getIntExtra(GPS_AVG_COMPLETE, 0);
//        return GpsAvgStatus.getStatusForCode(getGpsAveragingStatusCode);
//    }


    /**
     * Start position averaging.
     *
     * @param context the context to use.
     */
    public static void startGpsAveraging( Context context) {
        GPLog.addLogEntry("GPSAVG","In gpsserviceutilities startGPSAvg");
        Intent intent = new Intent(context, GpsAvgActivity.class);
        //startActivityForResult(intent, AVGRESULT);


    }
}
