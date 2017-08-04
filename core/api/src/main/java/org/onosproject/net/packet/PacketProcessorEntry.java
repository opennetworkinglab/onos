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
package org.onosproject.net.packet;

/**
 * Packet processor entry tracking the processor, its priority and
 * time consumption.
 */
public interface PacketProcessorEntry {

    /**
     * Returns the packet processor.
     *
     * @return packet processor
     */
    PacketProcessor processor();

    /**
     * Returns the packet processor priority.
     *
     * @return processor priority
     */
    int priority();

    /**
     * Returns the number of invocations.
     *
     * @return number of invocations
     */
    long invocations();

    /**
     * Returns the total time, in nanoseconds, spent processing packets.
     *
     * @return total time in nanos
     */
    long totalNanos();

    /**
     * Returns the average time, in nanoseconds, spent processing packets.
     *
     * @return average time in nanos
     */
    long averageNanos();
}
