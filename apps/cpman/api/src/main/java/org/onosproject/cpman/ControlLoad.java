/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cpman;

import org.onosproject.net.statistic.Load;

import java.util.concurrent.TimeUnit;

/**
 * Data repository for control plane load information.
 */
public interface ControlLoad extends Load {

    /**
     * Obtains the average of the specified time duration.
     *
     * @param duration time duration
     * @param unit     time unit
     * @return average control plane metric value
     */
    long average(int duration, TimeUnit unit);

    /**
     * Obtains the average of all time duration.
     *
     * @return average control plane metric value
     */
    long average();

    /**
     * Obtains the most recent metric values of the specified time duration.
     *
     * @param duration time duration
     * @param unit time unit
     * @return a collection of the most recent metric values
     */
    long[] recent(int duration, TimeUnit unit);

    /**
     * Obtains all metrics.
     *
     * @return a collection of the all metric values
     */
    long[] all();
}
