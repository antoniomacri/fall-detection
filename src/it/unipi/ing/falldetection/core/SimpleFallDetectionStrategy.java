package it.unipi.ing.falldetection.core;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.util.Log;

public class SimpleFallDetectionStrategy implements IFallDetectionStrategy, ISensorDataListener
{
    protected static final double g = 9.80665;
    protected static final long timerDelay = 2500;

    public SimpleFallDetectionStrategy(ISensorDataProvider source)
    {
        source.addListener(this);
    }

    protected Vector<OnFallDetectedListener> listeners = new Vector<OnFallDetectedListener>();

    @Override
    public void addListener(OnFallDetectedListener listener) {
        listeners.addElement(listener);
    }

    @Override
    public void removeListener(OnFallDetectedListener listener) {
        listeners.removeElement(listener);
    }

    protected void fireFallEvent(long timestamp, float reliability) {
        FallDetectionEvent event = new FallDetectionEvent(timestamp, reliability);
        for (OnFallDetectedListener e : listeners) {
            Log.i(Thread.currentThread() + getClass().getName(), "fireFallEvent to "
                    + e.getClass().getName());
            e.onFallDetected(this, event);
        }
    }

    protected class EventFiringTimerTask extends TimerTask
    {
        @Override
        public void run() {
            fireFallEvent(System.currentTimeMillis(), 1.0f);
        }
    };

    protected Timer timer = new Timer();
    protected TimerTask timerTask = new EventFiringTimerTask();

    @Override
    public void onDataAvailable(SensorDataManager sender, SensorData data) {
        float x = data.values[0];
        float y = data.values[1];
        float z = data.values[2];
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        if (magnitude >= 2 * g) {
            timer.cancel();
            timer = new Timer();
            timer.schedule(timerTask, timerDelay);
        }
    }
}
