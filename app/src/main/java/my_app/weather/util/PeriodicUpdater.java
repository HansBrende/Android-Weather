package my_app.weather.util;

import android.os.Handler;

/**
 * Created by Hans on 5/17/2015.
 */
public class PeriodicUpdater implements Runnable {

    private final Handler handler = new Handler();
    private final PeriodicUpdatee updatee;

    public PeriodicUpdater(PeriodicUpdatee updatee) {
        this.updatee = updatee;
    }

    public final void run() {
        long nextInterval = updatee.periodicUpdate();
        handler.postDelayed(this, nextInterval);
    }

    public final void run(long delay) {
        handler.postDelayed(this, delay);
    }

    public final void stop() {
        handler.removeCallbacks(this);
    }

}
