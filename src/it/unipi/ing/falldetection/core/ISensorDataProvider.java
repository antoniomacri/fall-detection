package it.unipi.ing.falldetection.core;

public interface ISensorDataProvider
{
	public void addListener(ISensorDataListener listener);

	public void removeListener(ISensorDataListener listener);

    public void feed(SensorData data);
}
