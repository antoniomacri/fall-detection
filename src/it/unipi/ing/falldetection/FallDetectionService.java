package it.unipi.ing.falldetection;

import java.util.Vector;

import it.unipi.ing.falldetection.core.*;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

public class FallDetectionService extends Service
{
    public static final String PREFERENCES_STATISTICS = "Stats";

    public static final String FALLDETECTION_START = "FALLDETECTION_START";
    public static final String FALLDETECTION_STOP = "FALLDETECTION_STOP";

    private static final int DELAY = SensorManager.SENSOR_DELAY_GAME;
    private static final int ACTIVATION_NOTIFICATION_ID = R.string.fall_detection_service;

    private volatile Looper serviceLooper;
    private volatile ServiceHandler serviceHandler;

    private SensorManager sensorManager;
    private SensorDataManager dataManager;
    private IFallDetectionStrategy fallDetectionStrategy;
    private SensorListener sensorListener;
    private FallListener fallListener;
    private boolean active = false;

    private final IBinder binder;
    private final Vector<IFallDetectionServiceListener> listeners;

    public FallDetectionService()
    {
        binder = new Binder();
        listeners = new Vector<IFallDetectionServiceListener>();
    }

    @Override
    public void onCreate()
    {
        HandlerThread thread = new HandlerThread(getClass().getName());
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        dataManager = new SensorDataManager();
        sensorListener = new SensorListener(dataManager);
        fallListener = new FallListener(this);
        fallDetectionStrategy = new SimpleFallDetectionStrategy(dataManager);
    }

    @Override
    public void onDestroy()
    {
        serviceLooper.quit();

        fallDetectionStrategy = null;
        fallListener = null;
        sensorListener = null;
        dataManager = null;
        sensorManager = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = serviceHandler.obtainMessage();
        msg.obj = intent;
        serviceHandler.sendMessage(msg);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public boolean isActive() {
        return active;
    }

    public void startFallDetection() {
        onStartCommand(new Intent(FALLDETECTION_START, null, this, getClass()), 0, 1);
    }

    public void stopFallDetection() {
        onStartCommand(new Intent(FALLDETECTION_STOP, null, this, getClass()), 0, 1);
    }

    public int getFallDetectedCount() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_STATISTICS, 0);
        return preferences.getInt("stats_falls_detected", 0);
    }

    public int getFallConfirmedCount() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_STATISTICS, 0);
        return preferences.getInt("stats_falls_confirmed", 0);
    }

    private void onHandleIntent(Intent intent)
    {
        if (FALLDETECTION_START.equals(intent.getAction())) {
            startFallDetectionImpl();
        }
        else if (FALLDETECTION_STOP.equals(intent.getAction())) {
            stopFallDetectionImpl();
        }
    }

    private void startFallDetectionImpl()
    {
        if (active) {
            return;
        }

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (!sensorManager.registerListener(sensorListener, accelerometer, DELAY, serviceHandler)) {
            String errorMessage = getResources().getString(R.string.no_accelerometer_available);
            fireFallDetectionStopped(true, errorMessage);
            return;
        }
        fallDetectionStrategy.addListener(fallListener);
        active = true;

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent intentPause = new Intent(this, FallDetectionService.class);
        intentPause.setAction(FALLDETECTION_STOP);
        PendingIntent pIntentPause = PendingIntent.getService(this, 0, intentPause, 0);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.status_active))
                .setContentText(getResources().getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(false);
        b.addAction(android.R.drawable.ic_media_pause,
                getResources().getString(R.string.tap_to_deactivate), pIntentPause);

        startService(new Intent(this, FallDetectionService.class));
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(ACTIVATION_NOTIFICATION_ID, b.build());
        fireFallDetectionStarted();
    }

    private void stopFallDetectionImpl()
    {
        if (!active) {
            return;
        }

        fallDetectionStrategy.removeListener(fallListener);
        sensorManager.unregisterListener(sensorListener);
        active = false;

        stopSelf();
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(ACTIVATION_NOTIFICATION_ID);
        fireFallDetectionStopped(false, null);
    }

    private final class ServiceHandler extends Handler
    {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
        }
    }

    /**
     * Class used for the client Binder. Because we know this service always runs in the same
     * process as its clients, we don't need to deal with IPC.
     */
    public class Binder extends android.os.Binder
    {
        public FallDetectionService getService() {
            return FallDetectionService.this;
        }
    }

    private static class SensorListener implements SensorEventListener
    {
        private SensorDataManager dataManager;

        public SensorListener(SensorDataManager dataManager) {
            this.dataManager = dataManager;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Nothing to do
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // Track only accelerometer events
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float[] values = new float[event.values.length];
                System.arraycopy(event.values, 0, values, 0, values.length);
                SensorData sd = new SensorData(event.sensor, values, event.accuracy,
                        event.timestamp);
                dataManager.feed(sd);
            }
        }
    };

    private static class FallListener implements IFallDetectionListener
    {
        private FallDetectionService service;

        public FallListener(FallDetectionService service) {
            this.service = service;
        }

        @Override
        public void onFallDetected(IFallDetectionStrategy sender, FallDetectionEvent event) {
            SharedPreferences preferences = service.getSharedPreferences(PREFERENCES_STATISTICS, 0);
            int stats_falls_detected = preferences.getInt("stats_falls_detected", 0);
            stats_falls_detected++;
            preferences.edit().putInt("stats_falls_detected", stats_falls_detected);
            // dataManager.save

            service.fireFallDetected(sender, event);

            Intent intent = new Intent(service, FallDetectedActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            service.startActivity(intent);
        }

        @Override
        public void onFallConfirmed(IFallDetectionStrategy sender, FallDetectionEvent event) {
            SharedPreferences preferences = service.getSharedPreferences("", 0);
            String sms_content_default = service.getString(R.string.sms_content_default);
            String sms_content = preferences.getString("sms_content", sms_content_default);
            boolean add_location = preferences.getBoolean("user_alerts_add_location", true);
            if (add_location) {
                if (!sms_content.matches("\\s$"))
                    sms_content += " ";
                sms_content += service.getString(R.string.sms_content_my_location) + " ";
                String longitude = "", latitude = "";
                sms_content += "http://www.google.com/maps?q=" + latitude + "," + longitude;
            }
            int id = R.string.fall_detected;

            int stats_falls_confirmed = preferences.getInt("stats_falls_confirmed", 0);
            stats_falls_confirmed++;
            preferences.edit().putInt("stats_falls_confirmed", stats_falls_confirmed);

            service.fireFallConfirmed(sender, event);
        }
    };

    public void addListener(IFallDetectionServiceListener listener) {
        synchronized (listeners) {
            listeners.addElement(listener);
        }
    }

    public void removeListener(IFallDetectionServiceListener listener) {
        synchronized (listeners) {
            listeners.removeElement(listener);
        }
    }

    private void fireFallDetectionStarted() {
        synchronized (listeners) {
            for (IFallDetectionServiceListener e : listeners) {
                e.onFallDetectionStarted();
            }
        }
    }

    private void fireFallDetectionStopped(boolean error, String errorMessage) {
        synchronized (listeners) {
            for (IFallDetectionServiceListener e : listeners) {
                e.onFallDetectionStopped(error, errorMessage);
            }
        }
    }

    private void fireFallDetected(IFallDetectionStrategy sender, FallDetectionEvent event) {
        synchronized (listeners) {
            for (IFallDetectionServiceListener e : listeners) {
                e.onFallDetected(sender, event);
            }
        }
    }

    private void fireFallConfirmed(IFallDetectionStrategy sender, FallDetectionEvent event) {
        synchronized (listeners) {
            for (IFallDetectionServiceListener e : listeners) {
                e.onFallConfirmed(sender, event);
            }
        }
    }
}
