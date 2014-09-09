package org.onlab.timer;

import org.jboss.netty.util.HashedWheelTimer;


public final class Timer {

    private Timer() {}

    private static HashedWheelTimer timer;

    public static HashedWheelTimer getTimer() {
        if (Timer.timer == null) {
            Timer.timer = new HashedWheelTimer();
            Timer.timer.start();
        }
        return Timer.timer;
    }

}
