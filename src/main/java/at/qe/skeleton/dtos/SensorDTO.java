package at.qe.skeleton.dtos;

public class SensorDTO {

    private long timestamp;
    private float temperature;
    private float humidity;
    private float pressure;
    private long gasResistance;

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }

    public float getHumidity() { return humidity; }
    public void setHumidity(float humidity) { this.humidity = humidity; }

    public float getPressure() { return pressure; }
    public void setPressure(float pressure) { this.pressure = pressure; }

    public long getGasResistance() { return gasResistance; }
    public void setGasResistance(long gasResistance) { this.gasResistance = gasResistance; }
}
