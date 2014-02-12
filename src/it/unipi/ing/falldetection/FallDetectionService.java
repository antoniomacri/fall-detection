package it.unipi.ing.falldetection;

import java.util.Vector;

import it.unipi.ing.falldetection.ContactListPreference.Contact;
import it.unipi.ing.falldetection.core.*;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class FallDetectionService extends Service
{
    public static final String FALLDETECTION_START = "FALLDETECTION_START";
    public static final String FALLDETECTION_STOP = "FALLDETECTION_STOP";
    public static final String FALLDETECTION_CONFIRM_FALL = "FALLDETECTION_CONFIRM_FALL";

    private static final int DELAY = SensorManager.SENSOR_DELAY_GAME;
    private static final int ACTIVATION_NOTIFICATION_ID = R.string.fall_detection_service;
    private static final String FALLDETECTION_SMS_SENT = "FALLDETECTION_SMS_SENT";

    private volatile Looper serviceLooper;
    private volatile ServiceHandler serviceHandler;

    private SensorManager sensorManager;
    private SensorDataManager dataManager;
    private IFallDetectionStrategy fallDetectionStrategy;
    private SensorListener sensorListener;
    private FallListener fallListener;
    private boolean active = false;

    private FallDetectionEvent lastEvent;

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
        fallListener = new FallListener();
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

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
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

    public boolean isActive() {
        return active;
    }

    public void startFallDetection() {
        onStartCommand(new Intent(FALLDETECTION_START, null, this, getClass()), 0, 1);
    }

    public void stopFallDetection() {
        onStartCommand(new Intent(FALLDETECTION_STOP, null, this, getClass()), 0, 1);
    }

    public void confirmFall(int token, boolean confirmed, String info) {
        Intent intent = new Intent(FALLDETECTION_CONFIRM_FALL, null, this, getClass());
        intent.putExtra("token", token);
        intent.putExtra("confirmed", confirmed);
        intent.putExtra("info", info);
        onStartCommand(intent, 0, 1);
    }

    private void onHandleIntent(Intent intent)
    {
        String action = intent.getAction();
        if (FALLDETECTION_START.equals(action)) {
            doStartFallDetection();
        }
        else if (FALLDETECTION_STOP.equals(action)) {
            doStopFallDetection();
        }
        else if (FALLDETECTION_CONFIRM_FALL.equals(action)) {
            boolean confirmed = intent.getBooleanExtra("confirmed", true);
            String info = intent.getStringExtra("info");
            int token = intent.getIntExtra("token", 0);
            doConfirmFall(token, confirmed, info);
        }
    }

    private void doStartFallDetection()
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

        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setContentTitle(getResources().getString(R.string.status_active));
        b.setContentText(getResources().getString(R.string.app_name));
        b.setSmallIcon(R.drawable.ic_launcher);
        b.setContentIntent(pIntent);
        b.setAutoCancel(false);
        b.addAction(android.R.drawable.ic_media_pause,
                getResources().getString(R.string.tap_to_deactivate), pIntentPause);

        startService(new Intent(this, FallDetectionService.class));

        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(ACTIVATION_NOTIFICATION_ID, b.build());

        fireFallDetectionStarted();
    }

    private void doStopFallDetection()
    {
        if (!active) {
            return;
        }

        fallDetectionStrategy.removeListener(fallListener);
        sensorManager.unregisterListener(sensorListener);
        active = false;

        // Stop the service
        stopSelf();
        // Now the service can be destroyed by the system
        // (whenever no more clients are bound to it).

        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(ACTIVATION_NOTIFICATION_ID);

        fireFallDetectionStopped(false, null);
    }

    private void doConfirmFall(int token, boolean confirmed, String info)
    {
        if (lastEvent == null || token != lastEvent.hashCode()) {
            Log.w(getClass().getName(), "Attempt to confirm or deny an old event.");
            return;
        }

        if (confirmed) {
            if (UserPreferencesHelper.isSmsAlertEnabled(this)) {
                sendSms();
            }
            if (UserPreferencesHelper.isPhoneCallAlertEnabled(this)) {
                makeCall();
            }

            StatisticsHelper.stepFallConfirmedCount(this);
            lastEvent.notes = info;
            fireFallConfirmed(fallDetectionStrategy, lastEvent);
        }
        lastEvent = null;
    }

    private void sendSms()
    {
        String smsContent = UserPreferencesHelper.getSmsContent(this);

        if (UserPreferencesHelper.isAddLocationEnabled(this)) {
            if (!smsContent.matches("\\s$")) {
                smsContent += " ";
            }
            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                String link = getString(R.string.sms_content_my_location_url_format, latitude, longitude);
                smsContent += getString(R.string.sms_content_my_location, link);
            }
            else {
                // TODO: obtain location deferred
            }
        }

        SmsManager smsManager = SmsManager.getDefault();
        Contact[] recipients = UserPreferencesHelper.getSmsRecipients(this);
        for (Contact c : recipients) {
            // We need to create a new BroadcastReceiver for each SMS, and each of them must
            // receive intents with different actions (we distinguish intents by using the phone
            // number). Each instance of the SmsSentBroadcastReceiver will unregister itself after
            // being called.
            SmsSentBroadcastReceiver receiver = new SmsSentBroadcastReceiver();
            registerReceiver(receiver, new IntentFilter(FALLDETECTION_SMS_SENT + "/" + c.value));
            Intent intent = new Intent(FALLDETECTION_SMS_SENT + "/" + c.value);
            intent.putExtra("contact_name", c.title);
            intent.putExtra("contact_number", c.value);
            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, intent, 0);
            smsManager.sendTextMessage(c.value, null, smsContent, sentPI, null);
        }
    }

    private void makeCall()
    {
        Contact recipient = UserPreferencesHelper.getPhoneCallRecipient(this);
        if (recipient == null) {
            return;
        }

        // To set the speakers on we need to listen for changes of the phone state.
        // The two alternatives are: a BroadcastReceiver for the intent ACTION_NEW_OUTGOING_CALL and
        // a PhoneStateListener. The former choice seems to be better, since it allows to retrieve
        // the number of the processing call and match it against the expected recipient's number.
        // (A PhoneStateListener provides the phone number only in case of an incoming call.)
        SpeakerPhoneBroadcastReceiver receiver = new SpeakerPhoneBroadcastReceiver(recipient.value);
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL));
        registerReceiver(receiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("tel:" + recipient.value));
        startActivity(intent);
    }

    private static class SensorListener implements SensorEventListener
    {
        // Make explicit the dependence on dataManager
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
                SensorData sd = new SensorData(event.sensor, values, event.accuracy, event.timestamp);
                dataManager.feed(sd);
            }
        }
    };

    private class FallListener implements OnFallDetectedListener
    {
        @Override
        public void onFallDetected(IFallDetectionStrategy sender, FallDetectionEvent event) {
            lastEvent = event;

            // dataManager.save

            StatisticsHelper.stepFallDetectedCount(FallDetectionService.this);
            fireFallDetected(sender, event);

            if (UserPreferencesHelper.getAlertTimeout(FallDetectionService.this) == 0) {
                doConfirmFall(event.hashCode(), true, "");
            }
            else {
                Intent intent = new Intent(FallDetectionService.this, FallDetectedActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // Use a "token" to match the event signaled to the FallDetectedActivity with the
                // one that will be confirmed or denied.
                intent.putExtra("token", lastEvent.hashCode());
                startActivity(intent);
            }
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
