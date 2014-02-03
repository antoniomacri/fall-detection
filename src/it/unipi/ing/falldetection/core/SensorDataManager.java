package it.unipi.ing.falldetection.core;

import java.util.Vector;

public class SensorDataManager implements ISensorDataProvider
{
    protected Vector<ISensorDataListener> listeners = new Vector<ISensorDataListener>();

    public void addListener(ISensorDataListener listener) {
        listeners.addElement(listener);
    }

    public void removeListener(ISensorDataListener listener) {
        listeners.removeElement(listener);
    }

    protected void fireDataAvailable(SensorData data) {
        for (ISensorDataListener e : listeners) {
            e.onDataAvailable(this, data);
        }
    }

    public void feed(SensorData data) {
        fireDataAvailable(data);
    }
}
