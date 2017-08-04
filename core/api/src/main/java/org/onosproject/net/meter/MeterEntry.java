/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.meter;

/**
 * Represents a stored meter.
 */
public interface MeterEntry extends Meter {

    /**
     * Updates the state of this meter.
     *
     * @param state a meter state
     */
    void setState(MeterState state);

    /**
     * Set the amount of time the meter has existed in seconds.
     *
     * @param life number of seconds
     */
    void setLife(long life);

    /**
     * Sets the number of flows which are using this meter.
     *
     * @param count a reference count.
     */
    void setReferenceCount(long count);

    /**
     * Updates the number of packets seen by this meter.
     *
     * @param packets a packet count.
     */
    void setProcessedPackets(long packets);

    /**
     * Updates the number of bytes seen by the meter.
     *
     * @param bytes a byte counter.
     */
    void setProcessedBytes(long bytes);
}
