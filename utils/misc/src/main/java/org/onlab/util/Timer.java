package org.onlab.util;

import org.jboss.netty.util.HashedWheelTimer;

/**
 * Hashed-wheel timer singleton. Care must be taken to shutdown the timer
 * only when the VM is ready to exit.
 */
public final class Timer {

    private static HashedWheelTimer timer;

    // Ban public construction
    private Timer() {
    }

    /**
     * Returns the singleton hashed-wheel timer.
     *
     * @return hashed-wheel timer
     */
    public static HashedWheelTimer getTimer() {
        if (Timer.timer == null) {
            Timer.timer = new HashedWheelTimer();
            Timer.timer.start();
        }
        return Timer.timer;
    }

}
