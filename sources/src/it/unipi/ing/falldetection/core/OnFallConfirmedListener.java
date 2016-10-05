package it.unipi.ing.falldetection.core;

public interface OnFallConfirmedListener
{
    void onFallConfirmed(IFallDetectionStrategy sender, FallDetectionEvent event);
}
