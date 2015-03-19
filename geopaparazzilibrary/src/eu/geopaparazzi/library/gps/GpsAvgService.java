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

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
 * An intent service to handle and return gps averaging data
 * <p/>
 * <p/>
 *
 * @author Tim Howard (nynhp.org)
 */
@SuppressWarnings("nls")

// info: https://developer.android.com/training/run-background-service/create-service.html

public class GpsAvgService extends IntentService {

    /**
     * Intent key to use for broadcasts.
     */
    public static final String GPS_AVG_SERVICE_BROADCAST_NOTIFICATION = "eu.geopaparazzi.library.gps.GpsAvgService.BROADCAST";

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
    private BroadcastReceiver cReceiver;

    /**
     * The last taken gps location.
     */
    private GpsLocation lastGpsLocation = null;

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

    //public constructor
    public GpsAvgService() {
        super("GpsAvgService");
    }


    // can't usually cancel an intentService, need these overrides to make it possible
    private class CancelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            GPLog.addLogEntry("GPSAVG", "custom receiver onReceive");
            stopAveragingRequest = true;
            stopSelf();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cReceiver = new CancelReceiver();
        IntentFilter filter = new IntentFilter(GpsAvgService.STOP_AVERAGING_NOW);
        registerReceiver(cReceiver, filter);
        GPLog.addLogEntry("GPSAVG","custom receiver registered");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (cReceiver != null)
            unregisterReceiver(cReceiver);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Gets data from the incoming Intent
        String dataString = intent.getDataString();

        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            useNetworkPositions = preferences.getBoolean(LibraryConstants.PREFS_KEY_GPS_USE_NETWORK_POSITION, false);
            isMockMode = preferences.getBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
            GPLog.addLogEntry("GPSAVG", "onHandleIntent: Preferences created");
        }

        if (locationManager == null) {
            GPLog.addLogEntry("GPSAVG", "onHandleIntent: start locationManager");
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        if (intent.hasExtra(START_GPS_AVERAGING)){
            GPLog.addLogEntry("GPSAVG", "onHandleIntent: Start GPS averaging called");
            stopAveragingRequest = false;
            numberSamplesUsedInAvg = -1;
            gpsavgmeasurements = GpsAvgMeasurements.getInstance();
            int doAverage = intent.getIntExtra(START_GPS_AVERAGING, 0);
            if(!isAveraging && doAverage==1){
                startAveraging();
            }
        }

        if (intent.hasExtra(STOP_AVERAGING_NOW)){
            GPLog.addLogEntry("GPSAVG", "onHandleIntent: stop averaging called via intent");
            stopAveragingRequest = true;
            stopSelf();
        }


//        if (intent.hasExtra(STOP_AVERAGING_NOW)){
//            log("onHandleIntent: Stop GPS averaging called");
//            log("GPSAVG: Stop GPS averaging called");
//            stopAveragingRequest = true;
//        }


    }

    private static void log(String msg) {
        try {
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("GPSAVGSERVICE", null, null, msg);

        } catch (Exception e) {
            e.printStackTrace();
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
            GPLog.addLogEntry("GPSAVG","sendBroadcast in Av complete and av=False");
            sendBroadcast(intent);
            stopSelf();
        } else {
            GPLog.addLogEntry("GPSAVG","sendBroadcast av not complete");
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

        // todo: see: http://stackoverflow.com/questions/24785267/pending-intent-from-notification-not-received
        // see: http://android-er.blogspot.com/2013/03/stop-intentservice.html
        // see: http://stackoverflow.com/questions/12112998/stopping-an-intentservice-from-an-activity

        //build the notification intents
        //Intent cancelIntent = new Intent(this, GpsAvgService.class);
        //Intent cancelIntent = new Intent(this, CancelReceiver.class);
        Intent cancelIntent = new Intent();
        cancelIntent.setAction(STOP_AVERAGING_NOW);
        //Intent cancelIntent = new Intent();
        //cancelIntent.setAction(ACTION_CANCEL);
        //intent.putExtra(GPS_SERVICE_STATUS, 1);
        cancelIntent.putExtra(STOP_AVERAGING_NOW, 1);
        //intent.putExtra("stopGPSAveraging","stopGpsAv");


        //final PendingIntent pendingIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        for (int i = 0; i < numSamps; i++) {
            GPLog.addLogEntry("GPSAVG", "In avg loop, i=" + i);
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
                GPLog.addLogEntry("GPSAVG", "stop avg req in avg loop");
                break;
            }
        }
        if(numberSamplesUsedInAvg == -1){
            numberSamplesUsedInAvg = numSamps;
            GPLog.addLogEntry("GPSAVG", "set numberSamples to " + numberSamplesUsedInAvg);
        }
        broadcast("GPS Averaging complete");
        cancelAvgNotify(notifyMgr);
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
        int notificationId = 6789;
        notifyMgr.notify(notificationId, nBuilder.build());
    }

    /**
    * Cancels the notification
    *
    * @param notifyMgr the notification manager
    *
    */
    public void cancelAvgNotify(NotificationManager notifyMgr) {
        GPLog.addLogEntry("GPSAVG", "cancel notification called");
    notifyMgr.cancel(6789);

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
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
    //
//    @Override
//    public boolean stopService( Intent name ) {
//    /*
//    * You stop a service via the stopService() method. No matter how
//    * frequently you called the startService(intent) method, one call
//    * to the stopService() method stops the service.
//    *
//    * A service can terminate itself by calling the stopSelf() method.
//    * This is typically done if the service finishes its work.
//    */
//    return super.stopService(name);
//    }


}
