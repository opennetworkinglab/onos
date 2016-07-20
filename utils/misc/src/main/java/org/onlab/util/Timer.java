/*
 * Copyright 2014-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.util;

import org.jboss.netty.util.HashedWheelTimer;

/**
 * Hashed-wheel timer singleton. Care must be taken to shutdown the timer
 * only when the VM is ready to exit.
 */
public final class Timer {

    private static volatile HashedWheelTimer timer;

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
            initTimer();
        }
        return Timer.timer;
    }

    private static synchronized  void initTimer() {
        if (Timer.timer == null) {
            HashedWheelTimer hwTimer = new HashedWheelTimer();
            hwTimer.start();
            Timer.timer = hwTimer;
        }
    }

}
