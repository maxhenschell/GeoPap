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

import static eu.geopaparazzi.library.util.LibraryConstants.GPS_LOGGING_DISTANCE;
import static eu.geopaparazzi.library.util.LibraryConstants.GPS_LOGGING_INTERVAL;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_GPSLOGGINGDISTANCE;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_GPSLOGGINGINTERVAL;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_GPSAVG_NUMBER_SAMPLES;
import static eu.geopaparazzi.library.util.LibraryConstants.GPS_AVERAGING_SAMPLE_NUMBER;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.Toast;

import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IGpsLogDbHelper;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.debug.TestMock;


/**
 * A service to handle the GPS data.
 * <p/>
 * <p/>
 * use this to start and trigger a service</br>
 * <code>Intent i= new Intent(context, GpsService.class)</code>;</br>
 * add data to the intent</br>
 * <code>i.putExtra("KEY1", "Value to be used by the service");</br>
 * context.startService(i);</code>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class GpsAvgService extends Service implements LocationListener, Listener {

    /**
     * Intent key to use for broadcasts.
     */
    public static final String GPS_AVG_SERVICE_BROADCAST_NOTIFICATION = "eu.geopaparazzi.library.gps.GpsAvgService";

    /**
     * Intent key to use for int gps status.
     * <p/>
     * <p>Status can be:
     * <ul>
     * <li>gps off = 0</li>
     * <li>gps on but not listening for updates = 1</li>
     * <li>gps on and listening for updates but no fix= 2</li>
     * <li>gps has fix = 3</li>
     * </ul>
     */
    public static final String GPS_SERVICE_STATUS = "GPS_SERVICE_STATUS";

    /**
     * Intent key to use for double array position data [lon, lat, elev].
     */
    public static final String GPS_SERVICE_POSITION = "GPS_SERVICE_POSITION";
    /**
     * Intent key to use for double array gps averaged position data [lon, lat, elev].
     */
    public static final String GPS_SERVICE_AVERAGED_POSITION = "GPS_SERVICE_AVERAGED_POSITION";
    /**
     * Intent key to use for double array position extra data [accuracy, speed, bearing].
     */
    public static final String GPS_SERVICE_POSITION_EXTRAS = "GPS_SERVICE_POSITION_EXTRAS";
    /**
     * Intent key to use for long time.
     */
    public static final String GPS_SERVICE_POSITION_TIME = "GPS_SERVICE_POSITION_TIME";
    /**
     * Intent key to use for current recorded log id.
     */
    public static final String GPS_SERVICE_CURRENT_LOG_ID = "GPS_SERVICE_CURRENT_LOG_ID";
    /**
     * Intent key to use for int array gps extra data [maxSatellites, satCount, satUsedInFixCount].
     */
    public static final String GPS_SERVICE_GPSSTATUS_EXTRAS = "GPS_SERVICE_GPSSTATUS_EXTRAS";
    /**
     * Intent key to use to trigger a broadcast.
     */
    public static final String GPS_SERVICE_DO_BROADCAST = "GPS_SERVICE_DO_BROADCAST";
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
     * <p>This changes with every {@link #onLocationChanged(Location)}.</p>
     */
    private Location previousLoc = null;

    private long lastLocationupdateMillis;
    private int currentPointsNum;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // GPLog.addLogEntry(this, "onStartCommand called with intent: " + intent);

        /*
         * If startService(intent) is called while the service is running, 
         * its onStartCommand() is also called. Therefore your service needs 
         * to be prepared that onStartCommand() can be called several times.
         */
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            useNetworkPositions = preferences.getBoolean(LibraryConstants.PREFS_KEY_GPS_USE_NETWORK_POSITION, false);
            isMockMode = preferences.getBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
            toastHandler = new Handler();
            log("onStartCommand: Preferences created");
        }
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.addGpsStatusListener(this);
            isProviderEnabled = isGpsOn();

            log("onStartCommand: LocationManager created + GpsService started");
        }
        if (!isListeningForUpdates) {
            registerForLocationUpdates();
            log("onStartCommand: Registered for location updates");
        }

        if (intent != null) {

            if (intent.hasExtra(GPS_SERVICE_DO_BROADCAST)) {
                log("onStartCommand: broadcast trigger");
                boolean doBroadcast = intent.getBooleanExtra(GPS_SERVICE_DO_BROADCAST, false);
                if (doBroadcast) {
                    broadcast("triggered by onStartCommand Intent");
                }
            }
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

        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        log("onDestroy Gpsservice.");

        if (locationManager != null && isListeningForUpdates) {
            locationManager.removeUpdates(this);
            locationManager.removeGpsStatusListener(this);
            isListeningForUpdates = false;
        }
        if (TestMock.isOn) {
            TestMock.stopMocking(locationManager);
        }
        super.onDestroy();
    }

    /**
     * Starts listening to the gps provider.
     */
    private void registerForLocationUpdates() {
        if (isMockMode) {
            log("Gps Avg service started using Mock locations");
            TestMock.startMocking(locationManager, this);
            isListeningForUpdates = true;
        } else {
            float minDistance = 0.2f;
            long waitForSecs = WAITSECONDS;

            if (useNetworkPositions) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, waitForSecs * 1000l, minDistance, this);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, waitForSecs * 1000l, minDistance, this);
            }
            isListeningForUpdates = true;
            log("registered for updates.");
        }
        broadcast("triggered by registerForLocationUpdates");
    }

    private static void log(String msg) {
        try {
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("GPSAVGSERVICE", null, null, msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void logABS(String msg) {
        try {
            if (GPLog.LOG_ABSURD)
                GPLog.addLogEntry("GPSAVGSERVICE", null, null, msg);
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

            broadcast("triggered by onLocationChanged");
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // for( GpsManagerListener activity : listeners ) {
        // activity.onStatusChanged(provider, status, extras);
        // }
    }

    public void onProviderEnabled(String provider) {
        isProviderEnabled = true;
        if (!isListeningForUpdates) {
            registerForLocationUpdates();
        }
        broadcast("triggered by onProviderEnabled");
    }

    public void onProviderDisabled(String provider) {
        isProviderEnabled = false;
        broadcast("triggered by onProviderDisabled");
    }

    public void onGpsStatusChanged(int event) {
        mStatus = locationManager.getGpsStatus(mStatus);

        // check fix
        boolean tmpGotFix = GpsStatusInfo.checkFix(gotFix, lastLocationupdateMillis, event);
        if (!tmpGotFix) {
            // check if it is just standing still
            GpsStatusInfo info = new GpsStatusInfo(mStatus);
            int satForFixCount = info.getSatUsedInFixCount();
            if (satForFixCount > 2) {
                tmpGotFix = true;
                // updating loc update, assuming the still filter is giving troubles
                lastLocationupdateMillis = SystemClock.elapsedRealtime();
            }
        }

        // if (DOLOGPOSITION) {
        // StringBuilder sb = new StringBuilder();
        // sb.append("gotFix: ").append(gotFix).append(" tmpGotFix: ").append(tmpGotFix).append("\n");
        // GPLog.addLogEntry("GPSSERVICE", sb.toString());
        // }

        if (tmpGotFix != gotFix) {
            gotFix = tmpGotFix;
            broadcast("triggered by onGpsStatusChanged on fix change: " + gotFix);
        } else {
            gotFix = tmpGotFix;
            if (!tmpGotFix && isProviderEnabled) {
                broadcast("triggered by onGpsStatusChanged on fix change: " + gotFix);
            }
        }

        if (!gotFix) {
            lastGpsLocation = null;
        }
    }

    /**
     * @param message a message for sending data back.
     */
    private void broadcast(String message) {
        Intent intent = new Intent(GPS_AVG_SERVICE_BROADCAST_NOTIFICATION);

        int status = 0; // gps off
        if (isProviderEnabled) {
            status = 1; // gps on
        }
        if (isProviderEnabled && isListeningForUpdates && !gotFix) {
            status = 2; // listening for updates but has no fix
        }
        if ((isProviderEnabled && isListeningForUpdates && gotFix && lastGpsLocation != null) || isMockMode) {
            status = 3; // listening for updates and has fix
        }
        intent.putExtra(GPS_SERVICE_STATUS, status);

        double lon = -1;
        double lat = -1;
        double elev = -1;
        float accuracy = -1;
        long time = -1;
        if (lastGpsLocation != null) {
            lon = lastGpsLocation.getLongitude();
            lat = lastGpsLocation.getLatitude();
            elev = lastGpsLocation.getAltitude();
            double[] lastPositionArray = new double[]{lon, lat, elev};
            intent.putExtra(GPS_SERVICE_POSITION, lastPositionArray);
            accuracy = lastGpsLocation.getAccuracy();
            float[] lastPositionExtrasArray = new float[]{accuracy};
            intent.putExtra(GPS_SERVICE_POSITION_EXTRAS, lastPositionExtrasArray);
            time = lastGpsLocation.getTime();
            intent.putExtra(GPS_SERVICE_POSITION_TIME, time);
        }
        int maxSatellites = -1;
        int satCount = -1;
        int satUsedInFixCount = -1;
        if (mStatus != null) {
            GpsStatusInfo info = new GpsStatusInfo(mStatus);
            maxSatellites = info.getMaxSatellites();
            satCount = info.getSatCount();
            satUsedInFixCount = info.getSatUsedInFixCount();
            intent.putExtra(GPS_SERVICE_GPSSTATUS_EXTRAS, new int[]{maxSatellites, satCount, satUsedInFixCount});
        }

//        if (DOLOGPOSITION) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("GPS SERVICE INFO: ").append(message).append("\n");
//            sb.append("---------------------------\n");
//            sb.append("gps status=").append(GpsServiceStatus.getStatusForCode(status)).append("(" + status).append(")\n");
//            sb.append("lon=").append(lon).append("\n");
//            sb.append("lat=").append(lat).append("\n");
//            sb.append("elev=").append(elev).append("\n");
//            sb.append("accuracy=").append(accuracy).append("\n");
//            sb.append("speed=").append(speed).append("\n");
//            sb.append("bearing=").append(bearing).append("\n");
//            sb.append("time=").append(time).append("\n");
//            sb.append("maxSatellites=").append(maxSatellites).append("\n");
//            sb.append("satCount=").append(satCount).append("\n");
//            sb.append("satUsedInFix=").append(satUsedInFixCount).append("\n");
//            GPLog.addLogEntry("GPSAVGSERVICE", sb.toString());
//        }

        if (isAveraging) {
            Location loc = gpsavgmeasurements.getAveragedLocation();
            lon = loc.getLongitude();
            lat = loc.getLatitude();
            elev = loc.getAltitude();
            int numSamples = numberSamplesUsedInAvg;
            double[] GpsAvgPositionArray = new double[]{lon, lat, elev, numSamples};
            intent.putExtra(GPS_SERVICE_AVERAGED_POSITION, GpsAvgPositionArray);
            if(message == "GPS Averaging complete") {
                intent.putExtra(GPS_AVG_COMPLETE, 1);
                GPLog.addLogEntry("GPSAVG", "put extra AVGCOMPLETE");
                isAveraging = false;
            } else {
                intent.putExtra(GPS_AVG_COMPLETE,0);
            }
            GPLog.addLogEntry("GPSAVG","put extra AVERAGED POSITION");
        }

        if (message == "GPS Averaging complete" & isAveraging == false){
            sendBroadcast(intent);
            stopSelf();
        } else {
            sendBroadcast(intent);
        }


    }

    private class ToastRunnable implements Runnable {
        String mText;

        public ToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_LONG).show();
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
        Intent intent = new Intent(this, GpsAvgService.class);
        intent.setAction("stopGpsAv");
        //intent.putExtra(GPS_SERVICE_STATUS, 1);
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
                        broadcast("GPS Averaging complete");
                        cancelAvgNotify(notifyMgr);
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

    // /////////////////////////////////////////////
    // UNUSED METHODS
    // /////////////////////////////////////////////
    // @Override
    // public void onCreate() {
    // super.onCreate();
    // /*
    // * If the startService(intent) method is called and the service is not
    // * yet running, the service object is created and the onCreate()
    // * method of the service is called.
    // */
    // }
    //
    // @Override
    // public ComponentName startService( Intent service ) {
    // /*
    // * Once the service is started, the startService(intent) method in the
    // * service is called. It passes in the Intent object from the
    // * startService(intent) call.
    // */
    // return super.startService(service);
    // }
    //
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //
    @Override
    public boolean stopService( Intent name ) {
    /*
    * You stop a service via the stopService() method. No matter how
    * frequently you called the startService(intent) method, one call
    * to the stopService() method stops the service.
    *
    * A service can terminate itself by calling the stopSelf() method.
    * This is typically done if the service finishes its work.
    */
    return super.stopService(name);
    }
}
