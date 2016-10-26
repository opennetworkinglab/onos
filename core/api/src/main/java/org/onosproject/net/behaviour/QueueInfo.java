/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import com.google.common.primitives.UnsignedInteger;

/**
 * @deprecated in Junco Release (1.9.1), Use QueueDescription instead
 * {@link org.onosproject.net.behaviour.QueueDescription}
 */
@Deprecated
public class QueueInfo {

    public enum Type {
        /**
         * Supports burst and priority as well as min and max rates.
         */
        FULL,

        /**
         * Only support min and max rates.
         */
        MINMAX
    }

    private final UnsignedInteger queueId;
    private final Type type;
    private final long minRate;
    private final long maxRate;
    private final long burst;
    private final long priority;

    public QueueInfo(UnsignedInteger queueId, Type type, long minRate,
                     long maxRate, long burst, long priority) {
        this.queueId = queueId;
        this.type = type;
        this.minRate = minRate;
        this.maxRate = maxRate;
        this.burst = burst;
        this.priority = priority;
    }

    //TODO builder
    // public static QueueInfoBuilder builder() {}
}
