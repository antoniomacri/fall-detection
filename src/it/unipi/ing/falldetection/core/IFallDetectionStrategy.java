package it.unipi.ing.falldetection.core;

public interface IFallDetectionStrategy
{
	void addListener(IFallDetectionListener listener);

    void removeListener(IFallDetectionListener listener);
}
