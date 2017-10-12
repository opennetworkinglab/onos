/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.net.flow;

/**
 * Stat fields are supported default by OXS.
 */
public enum StatTriggerField {
    /** Time flow entry has been alive. Unit indicates nanoseconds. */
    DURATION,
    /** Time flow entry has been idle. Unit indicates nanoseconds. */
    IDLE_TIME,
    /** Number of aggregated flow entries. */
    FLOW_COUNT,
    /** Number of packets in flow entry. */
    PACKET_COUNT,
    /** Number of bytes in flow entry. */
    BYTE_COUNT
}