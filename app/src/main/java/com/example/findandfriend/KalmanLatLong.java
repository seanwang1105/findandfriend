package com.example.findandfriend;

public class KalmanLatLong {
    private final float Q_metres_per_second; // Process noise
    private long timeStamp_milliseconds; // Last timestamp
    private double lat; // Latitude estimate
    private double lng; // Longitude estimate
    private float variance; // Position estimate variance

    public KalmanLatLong(float Q_metres_per_second) {
        this.Q_metres_per_second = Q_metres_per_second;
        this.variance = -1; // P matrix. Negative means object uninitialized.
    }

    public void process(double lat_measurement, double lng_measurement, float accuracy, long timeStamp_milliseconds) {
        if (variance < 0) {
            // Initialization
            this.lat = lat_measurement;
            this.lng = lng_measurement;
            this.variance = accuracy * accuracy;
            this.timeStamp_milliseconds = timeStamp_milliseconds;
        } else {
            // Time step
            long time_inc_milliseconds = timeStamp_milliseconds - this.timeStamp_milliseconds;
            if (time_inc_milliseconds > 0) {
                // Predict to new time step
                float variance_increase = time_inc_milliseconds / 1000.0f * Q_metres_per_second;
                variance += variance_increase;
                this.timeStamp_milliseconds = timeStamp_milliseconds;
            }

            // Kalman gain
            float K = variance / (variance + accuracy * accuracy);
            this.lat += K * (lat_measurement - this.lat);
            this.lng += K * (lng_measurement - this.lng);
            this.variance = (1 - K) * variance;
        }
    }

    public double getLat() {
        return this.lat;
    }

    public double getLng() {
        return this.lng;
    }
}

