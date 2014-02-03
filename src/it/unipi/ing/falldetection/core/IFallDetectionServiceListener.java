package it.unipi.ing.falldetection.core;

public interface IFallDetectionServiceListener extends IFallDetectionListener
{
    void onFallDetectionStarted();

    void onFallDetectionStopped(boolean error, String errorMessage);
}
