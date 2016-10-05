package it.unipi.ing.falldetection.core;

public interface IFallDetectionServiceListener extends OnFallDetectedListener, OnFallConfirmedListener
{
    void onFallDetectionStarted();

    void onFallDetectionStopped(boolean error, String errorMessage);
}
