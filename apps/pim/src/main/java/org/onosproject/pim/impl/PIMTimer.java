/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.pim.impl;

import org.jboss.netty.util.HashedWheelTimer;

/**
 * PIM Timer used for PIM Neighbors.
 */
public final class PIMTimer {

    private static volatile HashedWheelTimer timer;

    // Ban public construction
    private PIMTimer() {
    }

    /**
     * Returns the singleton hashed-wheel timer.
     *
     * @return hashed-wheel timer
     */
    public static HashedWheelTimer getTimer() {
        if (PIMTimer.timer == null) {
            initTimer();
        }
        return PIMTimer.timer;
    }

    // Start the PIM timer.
    private static synchronized  void initTimer() {
        if (PIMTimer.timer == null) {

            // Create and start a new hashed wheel timer, if it does not exist.
            HashedWheelTimer hwTimer = new HashedWheelTimer();
            hwTimer.start();
            PIMTimer.timer = hwTimer;
        }
    }
}
