package my_app.weather.util;

/**
 * Created by Hans on 5/17/2015.
 */
public interface PeriodicUpdatee {
    /**
     * @return number of milliseconds until the next update
     */
    long periodicUpdate();
}
