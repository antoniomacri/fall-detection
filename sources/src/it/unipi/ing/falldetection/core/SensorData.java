package it.unipi.ing.falldetection.core;

import android.hardware.Sensor;

public class SensorData
{
    /**
     * The values acquired from the sensor. See {@link android.hardware.Sensor#values Sensor.values}
     * for details.
     */
    public final float[] values;

    /**
     * The sensor that produced the values. See {@link android.hardware.SensorManager SensorManager}
     * for details.
     */
    public Sensor sensor;

    /**
     * The accuracy of this sensor reading. See {@link android.hardware.SensorManager SensorManager}
     * for details.
     */
    public int accuracy;

    /**
     * The time in nanosecond at which the event happened.
     */
    public long timestamp;

    public SensorData(Sensor sensor, float[] values, int accuracy, long timestamp)
    {
        this.sensor = sensor;
        this.values = values;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
    }
}
