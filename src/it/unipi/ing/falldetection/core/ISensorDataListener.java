package it.unipi.ing.falldetection.core;

public interface ISensorDataListener
{
    void onDataAvailable(SensorDataManager sender, SensorData data);
}
