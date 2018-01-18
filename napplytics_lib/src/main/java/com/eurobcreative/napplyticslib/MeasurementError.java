package com.eurobcreative.napplyticslib;

/**
 * Error raised when a measurement fails.
 */
public class MeasurementError extends Exception {
    public MeasurementError(String reason) {
        super(reason);
    }
}