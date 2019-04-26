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

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_GPSAVG_NUMBER_SAMPLES;
import static eu.geopaparazzi.library.util.LibraryConstants.GPS_AVERAGING_SAMPLE_NUMBER;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.app.NotificationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsService;
import eu.geopaparazzi.library.gps.GpsLocation;

/**
 * An intent service to handle and return gps averaging data
 * <p/>
 * <p/>
 *
 * @author Tim Howard (nynhp.org), based on work by Andrea Antonello
 */
@SuppressWarnings("nls")

// info: https://developer.android.com/training/run-background-service/create-service.html

public class GpsAvgService extends IntentService {

    /**
     * Intent key to use for broadcasts.
     */
    public static final String GPS_AVG_SERVICE_BROADCAST_NOTIFICATION = "eu.geopaparazzi.library.gps.GpsAvgService.BROADCAST";

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
    private BroadcastReceiver cReceiver;

    /**
     * The last taken gps location.
     */
    private GpsLocation lastGpsLocation = null;

    /**
     * GPS time interval between averaging samples.
     * 1000 ms = 1 second
     */
    private static long WAIT_MILLISECONDS = 1000L;


    /**
     * for gps avg
     */
    private boolean isAveraging = false;
    private boolean stopAveragingRequest = false;
    private GpsAvgMeasurements gpsavgmeasurements;
    private NotificationCompat.Builder nBuilder;
    private int numberSamplesUsedInAvg = -1;

    public static final String CHANNEL_ID = "GeopaparazziGPSServiceChannel";
    private int notificationId = 6789;
    private NotificationManager notificationManagerNative;
    private String title;
    private String text;
    private String name;
    private String description;

    //public constructor
    public GpsAvgService() {
        super("GpsAvgService");
    }

    // can't usually cancel an intentService, need these overrides to make it possible
    private class CancelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //GPLog.addLogEntry("GPSAVG", "custom receiver onReceive");
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
        //GPLog.addLogEntry("GPSAVG","cancel receiver registered");

        title = "Geopap GPS Average Service";
        text = "Geopapaparazzi is position averaging.";
        name = "Geopap Avg Channel";
        description = "Geopaparazzi GPS Avg Service Channel";
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (cReceiver != null)
            unregisterReceiver(cReceiver);
    }

    //TODO: When gps signal is lost, geopap switches to location on map center, messing things up.
    //todo: need to handle gps signal coming in and out of connection

    @Override
    protected void onHandleIntent(Intent intent) {

        // Gets data from the incoming Intent
        String dataString = intent.getDataString();

        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            //GPLog.addLogEntry("GPSAVG", "onHandleIntent: Preferences created");
        }

        if (locationManager == null) {
            //GPLog.addLogEntry("GPSAVG", "onHandleIntent: start locationManager");
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        if (intent.hasExtra(START_GPS_AVERAGING)){
            //GPLog.addLogEntry("GPSAVG", "onHandleIntent: Start GPS averaging called");
            stopAveragingRequest = false;
            numberSamplesUsedInAvg = -1;
            gpsavgmeasurements = GpsAvgMeasurements.getInstance();
            if(!isAveraging){
                startAveraging();
            }
        }

        if (intent.hasExtra(STOP_AVERAGING_NOW)){
            //GPLog.addLogEntry("GPSAVG", "onHandleIntent: stop averaging called via intent");
            stopAveragingRequest = true;
            stopSelf();
        }
    }

    /**
     * @param message a message for sending data back.
     */
    private void broadcast(String message) {
        Intent intent = new Intent(GPS_AVG_SERVICE_BROADCAST_NOTIFICATION);

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

        if (isAveraging) {
            Location loc = gpsavgmeasurements.getAveragedLocation();
            lon = loc.getLongitude();
            lat = loc.getLatitude();
            elev = loc.getAltitude();
            accuracy = loc.getAccuracy();
            int numSamples = numberSamplesUsedInAvg;
            double[] GpsAvgPositionArray = new double[]{lon, lat, elev, numSamples, accuracy};
            intent.putExtra(GPS_SERVICE_AVERAGED_POSITION, GpsAvgPositionArray);
            if(message == "GPS Averaging complete") {
                intent.putExtra(GPS_AVG_COMPLETE, 1);
                //GPLog.addLogEntry("GPSAVG", "put extra AVGCOMPLETE");
                isAveraging = false;
            } else {
                intent.putExtra(GPS_AVG_COMPLETE,0);
            }
            //GPLog.addLogEntry("GPSAVG","put extra AVERAGED POSITION");
        }

        if (message == "GPS Averaging complete" & isAveraging == false){
            //GPLog.addLogEntry("GPSAVG","sendBroadcast in Av complete and av=False");
            sendBroadcast(intent);
            stopSelf();
        } else {
            //GPLog.addLogEntry("GPSAVG","sendBroadcast av not complete");
            sendBroadcast(intent);
        }


    }

    /**
     * Starts active averaging.
     */
    public void startAveraging() {
        isAveraging = true;
        boolean hasSignal = true;
        int signalLostSeconds = 0;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        gpsavgmeasurements.clean();
        //final int averagingDelaySeconds = 1;

        final String numSamples = preferences.getString(PREFS_KEY_GPSAVG_NUMBER_SAMPLES,
                String.valueOf(GPS_AVERAGING_SAMPLE_NUMBER));
        final Integer numSamps = Integer.parseInt(numSamples);
        //build the notification intents
        Intent cancelIntent = new Intent();
        cancelIntent.setAction(STOP_AVERAGING_NOW);
        cancelIntent.putExtra(STOP_AVERAGING_NOW, 1);

        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //run the averaging
        for (int i = 0; i < numSamps; i++) {
            //GPLog.addLogEntry("GPSAVG", "In avg loop, i=" + i);
            Location location = locationManager.getLastKnownLocation("gps");
            //TODO: check properly if gps signal is lost!!
            // method is probably to use same method as GpsInfoDialogFragment and set up a reciever and use all data from geopap's service for all of this
            if (location != null) {
                hasSignal = true;
                signalLostSeconds = 0;
                gpsavgmeasurements.add(location);
                notifyAboutAveraging(pendingIntent, notifyMgr, hasSignal, i, numSamps, signalLostSeconds);
                numberSamplesUsedInAvg++;

            } else {
                //if signal is lost, wait.
                // this actually happens when gps is turned off, but may not (does not?) happen when signal is lost but gps remains on
                //todo need a way to really check if signal is lost
                //GPLog.addLogEntry("GPSAVG", "gps signal lost i= " + i);
                hasSignal = false;
                i--;
                signalLostSeconds++;
                notifyAboutAveraging(pendingIntent, notifyMgr, hasSignal, i, numSamps, signalLostSeconds);
                if(signalLostSeconds > 20){break;}
            }

            try {
                Thread.sleep(WAIT_MILLISECONDS);
            } catch (InterruptedException e) {
                break;
            }

            if(stopAveragingRequest){
                numberSamplesUsedInAvg++;
                //GPLog.addLogEntry("GPSAVG", "stop avg req in avg loop. samps is " + i);
                break;
            }
        }
        if(numberSamplesUsedInAvg == -1){
            numberSamplesUsedInAvg = numSamps;
            //GPLog.addLogEntry("GPSAVG", "set numberSamples to " + numberSamplesUsedInAvg);
        }
        broadcast("GPS Averaging complete");
        cancelAvgNotify(notifyMgr);

    }


    /**
     * Creates a notification for users to track (and stop early, if desired) GPS position averaging
     *
     * @param pendingIntent the pending intent for the notification
     * @param notifyMgr the notification manager retrieved from the system
     * @param haveSignal whether or not the unit currently has GPS signal
     * @param sampsAcquired the current number of gps samples sent to gpsavgmeasurements for averaging
     * @param sampsTargeted the total number of gps samples requested for averaging
     * @param noSignalDuration the total time without GPS signal, seconds
     */
    public void notifyAboutAveraging(PendingIntent pendingIntent, NotificationManager notifyMgr, Boolean haveSignal, Integer sampsAcquired, Integer sampsTargeted, Integer noSignalDuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String msg = "Averaging " + String.valueOf(sampsAcquired) + " of " + String.valueOf(sampsTargeted) + ". Tap to cancel.";
            if (notificationManagerNative == null) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library

                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);
                // Register the channel with the system
                notificationManagerNative = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManagerNative != null) {
                    notificationManagerNative.createNotificationChannel(channel);

                    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setContentTitle("GPS Position Averaging")
                            .setContentText(msg)
                            .setSmallIcon(R.drawable.ic_stat_geopaparazzi_notification_icon)
                            .setContentIntent(pendingIntent)
                            .setOnlyAlertOnce(true)
                            .setWhen(System.currentTimeMillis())
                            .setProgress(sampsTargeted, sampsAcquired, false)
                            .build();
                    notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;

                    startForeground(notificationId, notification);
                }
            } else {
                if (haveSignal) {
                    Notification update = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setContentTitle("GPS Position Averaging")
                            .setContentText(msg)
                            .setSmallIcon(R.drawable.ic_stat_geopaparazzi_notification_icon)
                            .setContentIntent(pendingIntent)
                            .setOnlyAlertOnce(true)
                            .setWhen(System.currentTimeMillis())
                            .setProgress(sampsTargeted, sampsAcquired, false)
                            .build();

                    notificationManagerNative.notify(notificationId, update);
                } else {
                    msg = String.valueOf(sampsAcquired) + " points sampled. GPS signal lost for " + noSignalDuration + ", waiting. Tap to stop NOW.";
                    Notification update = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setContentTitle("GPS Position Averaging")
                            .setContentText(msg)
                            .setSmallIcon(R.drawable.ic_stat_geopaparazzi_notification_icon)
                            .setContentIntent(pendingIntent)
                            .setOnlyAlertOnce(true)
                            .setWhen(System.currentTimeMillis())
                            .setProgress(sampsTargeted, sampsAcquired, false)
                            .build();

                    notificationManagerNative.notify(notificationId, update);
                }
            }
        } else {
                if (nBuilder == null) {
                    String msg = "Averaging " + String.valueOf(sampsAcquired) + " of " + String.valueOf(sampsTargeted) + ". Tap to cancel.";
                    nBuilder = new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.map_marker_circle)
                            .setContentTitle("GPS Position Averaging")
                            .setContentText(msg)
                            .setContentIntent(pendingIntent)
                            .setProgress(sampsTargeted, sampsAcquired, false);

                } else {
                    if (haveSignal) {
                        String msg = String.valueOf(sampsAcquired) + " of " + String.valueOf(sampsTargeted) + " points sampled. Tap to stop NOW.";
                        nBuilder.setContentText(msg)
                                .setProgress(sampsTargeted, sampsAcquired, false);
                    } else {
                        String msg = String.valueOf(sampsAcquired) + " points sampled. GPS signal lost for " + noSignalDuration + ", waiting. Tap to stop NOW.";
                        nBuilder.setContentText(msg)
                                .setProgress(sampsTargeted, sampsAcquired, false);

                    }
                }
                // Issue notification
                notifyMgr.notify(notificationId, nBuilder.build());
            }
        }

    /**
    * Cancels the notification
    *
    * @param notifyMgr the notification manager
    *
    */
    public void cancelAvgNotify(NotificationManager notifyMgr) {
        //GPLog.addLogEntry("GPSAVG", "cancel notification called");
    notifyMgr.cancel(notificationId);

    }


}
