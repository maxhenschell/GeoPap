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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.debug.TestMock;

import static eu.geopaparazzi.library.util.LibraryConstants.GPS_AVERAGING_SAMPLE_NUMBER;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_GPSAVG_NUMBER_SAMPLES;


/**
 * A class to handle GPS averaging.
 * <p/>
 * <p/>
 *
 * @author Tim Howard (nynhp.org)
 */
@SuppressWarnings("nls")
public class GpsAvgActivity extends Activity {

    /**
     * Intent key to use for double array gps averaged position data [lon, lat, elev].
     */
    public static final String GPS_SERVICE_AVERAGED_POSITION = "GPS_SERVICE_AVERAGED_POSITION";

    /**
     * Intent key to pass the boolean to start gps averaging.
     */
    public static final String START_GPS_AVERAGING = "START_GPS_AVERAGING";
    /**
     * Intent key to pass the boolean to start gps averaging.
     */
    public static final String GPS_AVG_COMPLETE = "GPS_AVG_COMPLETE";
    /**
     * Intent key to pass the boolean to stop averaging.
     */
    public static final String STOP_AVERAGING_NOW = "STOP_AVERAGING_NOW";


    private SharedPreferences preferences;
    private LocationManager locationManager;
    private boolean useNetworkPositions = false;
    private boolean isMockMode = false;

    /**
     * The last taken gps location.
     */
    private GpsLocation lastGpsLocation = null;

    /**
     * The previous gps location or null if no gps location was taken yet.
     * <p/>
     * <p>This changes with every {@link #onLocationChanged(android.location.Location)}.</p>
     */
    private Location previousLoc = null;
    private int currentPointsNum;
    private long lastLocationupdateMillis;

    /**
     * The current total distance of the track from start to the current point.
     */
    private double currentDistance;

    /**
     * GPS time interval.
     */
    private static int WAITSECONDS = 1;

    private GpsStatus mStatus;
    private long currentRecordedLogId = -1;
    private volatile boolean gotFix;
    private boolean isDatabaseLogging = false;
    private boolean isListeningForUpdates = false;
    private boolean isProviderEnabled;
    private Handler toastHandler;

    /**
     * for gps avg
     */
    private boolean isAveraging = false; //original also declared static
    private boolean stopAveragingRequest = false;
    private GpsAvgMeasurements gpsavgmeasurements;
    private NotificationCompat.Builder nBuilder;
    private int numberSamplesUsedInAvg = -1;


    public void onCreate(Bundle icicle, Intent intent) {
        super.onCreate(icicle);
        setContentView(R.layout.note);

        Bundle extras = getIntent().getExtras();

        log("onCreate GpsAvgActivity");

        //this.setVisible(false);

/*        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            useNetworkPositions = preferences.getBoolean(LibraryConstants.PREFS_KEY_GPS_USE_NETWORK_POSITION, false);
            isMockMode = preferences.getBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
            toastHandler = new Handler();
            log("onCreateCommand: Preferences created");
        }
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //locationManager.addGpsStatusListener(this); //think only needed for gps service, not here
            isProviderEnabled = isGpsOn();

            log("onCreateCommand: LocationManager created");
        }

        if (intent != null) {
            if (intent.hasExtra(START_GPS_AVERAGING)){
                log("onStartCommand: Start GPS averaging called");
                stopAveragingRequest = false;
                numberSamplesUsedInAvg = -1;
                gpsavgmeasurements = GpsAvgMeasurements.getInstance();
                boolean doAverage = intent.getBooleanExtra(START_GPS_AVERAGING, false);
                if(!isAveraging && doAverage){
                    startAveraging();
                }
            }
            if (intent.hasExtra(STOP_AVERAGING_NOW)){
                log("onStartCommand: Stop GPS averaging called");
                log("GPSAVG: Stop GPS averaging called");
                stopAveragingRequest = true;
            }

        }*/

    }

    @Override
    public void onDestroy() {
        log("onDestroy GpsAvgActivity");

        if (TestMock.isOn) {
            TestMock.stopMocking(locationManager);
        }
        super.onDestroy();
    }


    private static void log(String msg) {
        try {
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("GPSAVG", null, null, msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void logABS(String msg) {
        try {
            if (GPLog.LOG_ABSURD)
                GPLog.addLogEntry("GPSAVG", null, null, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the GPS is switched on.
     * <p/>
     * <p>Does not say if the GPS is supplying valid data.</p>
     *
     * @return <code>true</code> if the GPS is switched on.
     */
    private boolean isGpsOn() {
        if (locationManager == null) {
            return false;
        }
        boolean gpsIsEnabled;
        if (useNetworkPositions) {
            gpsIsEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } else {
            gpsIsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        logABS("Gps is enabled: " + gpsIsEnabled);
        return gpsIsEnabled;
    }

    public void onLocationChanged(Location loc) {
        if (loc == null) {
            lastGpsLocation = null;
            return;
        }
        lastGpsLocation = new GpsLocation(loc);
        synchronized (lastGpsLocation) {
            lastLocationupdateMillis = SystemClock.elapsedRealtime();
            lastGpsLocation.setPreviousLoc(previousLoc);
            // save last known location
            double recLon = lastGpsLocation.getLongitude();
            double recLat = lastGpsLocation.getLatitude();
            double recAlt = lastGpsLocation.getAltitude();
            PositionUtilities.putGpsLocationInPreferences(preferences, recLon, recLat, recAlt);
            previousLoc = loc;

        }
    }


    /**
     * Starts active averaging.
     */
    public void startAveraging() {
        isAveraging = true;
        //Toast.makeText(GpsService.this, "Starting GPS Averaging", Toast.LENGTH_SHORT).show();
        gpsavgmeasurements.clean();
        final int averagingDelaySeconds = 1;

        final String numSamples = preferences.getString(PREFS_KEY_GPSAVG_NUMBER_SAMPLES,
                String.valueOf(GPS_AVERAGING_SAMPLE_NUMBER));
        final Integer numSamps = Integer.parseInt(numSamples);

        //build the notification intents
        Intent intent = new Intent(this, GpsAvgActivity.class);
        intent.setAction("stopGpsAv");
        intent.putExtra(STOP_AVERAGING_NOW, 1);
        //intent.putExtra("stopGPSAveraging","stopGpsAv");

        final PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < numSamps; i++) {
                            GPLog.addLogEntry("GPSAVG", "In avg loop");
                            // can't figure out how to use lastGpsLocation from this class
                            // need to sample immediate gps location, not delayed or stored
                            Location location = locationManager.getLastKnownLocation("gps");
                            if (location != null) {
                                gpsavgmeasurements.add(location);
                            }
                            try {
                                for (int j = 0; j < averagingDelaySeconds; j++) {
                                    Thread.sleep(1000L);
                                }
                            } catch (InterruptedException e) {
                                break;
                            }
                            notifyAboutAveraging(pendingIntent, notifyMgr, i, numSamps);
                            if(stopAveragingRequest){
                                numberSamplesUsedInAvg = i + 1;
                                break;
                            }
                        }
                        if(numberSamplesUsedInAvg == -1){
                            numberSamplesUsedInAvg = numSamps;
                        }
                        cancelAvgNotify(notifyMgr);
                        finishAndReturn();
                    }
                }
        ).start();

    }

    /**
     * Creates a notification for users to track (and stop early, if desired) GPS position averaging
     *
     * @param pendingIntent the pending intent for the notification
     * @param notifyMgr the notification manager retrieved from the system
     * @param sampsAcquired the current number of gps samples sent to gpsavgmeasurements for averaging
     * @param sampsTargeted the total number of gps samples requested for averaging
     *
     */
    public void notifyAboutAveraging(PendingIntent pendingIntent, NotificationManager notifyMgr, Integer sampsAcquired, Integer sampsTargeted) {

        if (nBuilder == null) {
            String msg = "Averaging " + String.valueOf(sampsAcquired) + " of " + String.valueOf(sampsTargeted) + ".";
            nBuilder =  new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.action_bar_logo)
                            .setContentTitle("GPS Position Averaging")
                            .setContentText(msg)
                            .setContentIntent(pendingIntent)
                            //.addAction(R.drawable.goto_position,"Finish now", stopAvgPendingIntent) //button capabilities only for android 5?
                            .setProgress(sampsTargeted,sampsAcquired,false);

        } else {
            String msg = String.valueOf(sampsAcquired) + " of " + String.valueOf(sampsTargeted) + " points sampled. Press to stop NOW.";
            nBuilder.setContentText(msg)
                    .setProgress(sampsTargeted,sampsAcquired,false);
        }

        // Issue notification
        int notificationId = 6;
        notifyMgr.notify(notificationId, nBuilder.build());
    }

    /**
    * Cancels the notification
    *
    * @param notifyMgr the notification manager
    *
    */
    public void cancelAvgNotify(NotificationManager notifyMgr) {

    notifyMgr.cancel(6);

    }

    /**
     * sends the return intent.
     */
    private void finishAndReturn(){
        Intent intent = getIntent();
        double lon = -1;
        double lat = -1;
        double elev = -1;

        Location loc = gpsavgmeasurements.getAveragedLocation();
        lon = loc.getLongitude();
        lat = loc.getLatitude();
        elev = loc.getAltitude();
        int numSamples = numberSamplesUsedInAvg;
        double[] GpsAvgPositionArray = new double[]{lon, lat, elev, numSamples};
        intent.putExtra(GPS_SERVICE_AVERAGED_POSITION, GpsAvgPositionArray);

        GPLog.addLogEntry("GPSAVG", "put extra AVGCOMPLETE");
        isAveraging = false;

        setResult(RESULT_OK,intent);
        finish();

    }

}
