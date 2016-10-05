package it.unipi.ing.falldetection.core;

public final class SensorDataBuffer
{
    public final String[] descriptions;
    public final float[][] values;

    public SensorDataBuffer(String[] descriptions, float[][] values) {
        this.descriptions = descriptions;
        this.values = values;
    }
}
