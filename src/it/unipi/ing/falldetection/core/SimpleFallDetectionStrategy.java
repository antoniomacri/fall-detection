package it.unipi.ing.falldetection.core;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class SimpleFallDetectionStrategy implements IFallDetectionStrategy, ISensorDataListener
{
    protected static final double g = 9.80665;
    protected static final long timerDelay = 2500;

    private SensorDataManager dataManager;

    public SimpleFallDetectionStrategy(SensorDataManager dataManager)
    {
        this.dataManager = dataManager;
        dataManager.addListener(this);
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

    protected void fireFallEvent(long timestamp, float reliability, SensorDataBuffer snapshot) {
        FallDetectionEvent event = new FallDetectionEvent(timestamp, reliability, snapshot);
        for (OnFallDetectedListener e : listeners) {
            e.onFallDetected(this, event);
        }
    }

    protected class EventFiringTimerTask extends TimerTask
    {
        @Override
        public void run() {
            fireFallEvent(System.currentTimeMillis(), 1.0f, dataManager.takeSnapshot());
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
