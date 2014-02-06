package it.unipi.ing.falldetection.core;

public interface OnFallDetectedListener
{
    void onFallDetected(IFallDetectionStrategy sender, FallDetectionEvent event);
}
