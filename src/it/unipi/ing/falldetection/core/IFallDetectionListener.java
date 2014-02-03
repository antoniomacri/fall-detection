package it.unipi.ing.falldetection.core;

public interface IFallDetectionListener
{
    void onFallDetected(IFallDetectionStrategy sender, FallDetectionEvent event);

    void onFallConfirmed(IFallDetectionStrategy sender, FallDetectionEvent event);
}
