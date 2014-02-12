package it.unipi.ing.falldetection.core;

public class FallDetectionEvent
{
    public final long timestamp;
    public final float reliability;
    public String notes;

    public FallDetectionEvent(long timestamp, float reliability)
    {
        this.timestamp = timestamp;
        this.reliability = reliability;
    }

    public FallDetectionEvent(long timestamp, float reliability, String notes)
    {
        this.timestamp = timestamp;
        this.reliability = reliability;
        this.notes = notes;
    }
}
