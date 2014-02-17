package it.unipi.ing.falldetection.core;

import java.util.Vector;

public class SensorDataManager implements ISensorDataProvider
{
    protected Vector<ISensorDataListener> listeners = new Vector<ISensorDataListener>();
    protected int history;
    protected int cardinality;
    protected String[] descriptions;
    protected float[][] pool;

    protected int position = 0;

    public SensorDataManager(int history, int cardinality, String[] descriptions) {
        this.history = history;
        this.cardinality = cardinality;
        this.descriptions = descriptions;
        this.pool = new float[history][cardinality];
    }

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
        synchronized (this) {
            pool[position][0] = data.timestamp;
            System.arraycopy(data.values, 0, pool[position], 1, data.values.length);
            position = (position + 1) % pool.length;
        }
        fireDataAvailable(data);
    }

    public SensorDataBuffer takeSnapshot() {
        float[][] ordered = new float[pool.length][cardinality];
        synchronized (this) {
            for (int i = 0; i < pool.length - position; i++) {
                System.arraycopy(pool[position + i], 0, ordered[i], 0, cardinality);
            }
            for (int i = 0; i < position; i++) {
                System.arraycopy(pool[i], 0, ordered[pool.length - position + i], 0, cardinality);
            }
        }
        return new SensorDataBuffer(descriptions, ordered);
    }
}
