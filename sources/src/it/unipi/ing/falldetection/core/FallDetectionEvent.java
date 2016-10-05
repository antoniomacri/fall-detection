package it.unipi.ing.falldetection.core;

public class FallDetectionEvent
{
    public final long timestamp;
    public final float reliability;
    public boolean confirmed;
    public String notes;
    public SensorDataBuffer snapshot;

    public FallDetectionEvent(long timestamp, float reliability, SensorDataBuffer snapshot)
    {
        this.timestamp = timestamp;
        this.reliability = reliability;
        this.snapshot = snapshot;
    }

    public void validate(boolean confirmed, String info) {
        this.confirmed = confirmed;
        this.notes = info;
    }
}
