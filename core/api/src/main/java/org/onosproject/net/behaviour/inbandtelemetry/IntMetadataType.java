/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.net.behaviour.inbandtelemetry;

/**
 * Represents a type of INT metadata.
 */
public enum IntMetadataType {
    /**
     * ID of a switch, unique in the scope of the whole network.
     */
    SWITCH_ID,
    /**
     * The port on which the INT packet was received and sent out.
     */
    L1_PORT_ID,
    /**
     * Time taken for the INT packet to be switched within the device.
     */
    HOP_LATENCY,
    /**
     * The build-up of traffic in the queue that the INT packet observes
     * in the device while being forwarded.
     */
    QUEUE_OCCUPANCY,
    /**
     * The device local time when the INT packet was received on the ingress port.
     */
    INGRESS_TIMESTAMP,
    /**
     * The device local time when the INT packet was processed by the egress port.
     */
    EGRESS_TIMESTAMP,
    /**
     * The logical ports on which the INT packet was received and sent out.
     */
    L2_PORT_ID,
    /**
     * Current utilization of the egress port via witch the INT packet was sent out.
     */
    EGRESS_TX_UTIL
}
