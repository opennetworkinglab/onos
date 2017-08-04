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

package org.onosproject.net;

/**
 * Represents ODU (Optical channel Data Unit) client port signal type.
 *
 * <p>
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)" and
 * Open Networking Foundation "Optical Transport Protocol Extensions Version 1.0".
 * </p>
 */
public enum CltSignalType {
    /** bit rate in Mbps. */
    CLT_1GBE(1_000),
    CLT_10GBE(10_000),
    CLT_40GBE(40_000),
    CLT_100GBE(100_000);

    private final long bitRate;

    CltSignalType(long bitRate) {
        this.bitRate = bitRate;
    }

    /**
     * Return the bit rate in Mbps of the port.
     * @return bit rate
     */
    public long bitRate() {
        return this.bitRate;
    }
}