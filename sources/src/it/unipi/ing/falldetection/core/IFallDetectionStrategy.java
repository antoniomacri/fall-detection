package it.unipi.ing.falldetection.core;

public interface IFallDetectionStrategy
{
    void addListener(OnFallDetectedListener listener);

    void removeListener(OnFallDetectedListener listener);
}
