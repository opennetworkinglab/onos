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
package org.onosproject.net;

/**
 * Represents ODU (Optical channel Data Unit) signal type.
 *
 * <p>
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)" and
 * Open Networking Foundation "Optical Transport Protocol Extensions Version 1.0".
 * </p>
 */
public enum OduSignalType {
    // bit rate in Mbps
    ODU0(1_250),
    ODU1(2_500),
    ODU2(10_000),
    ODU2e(10_000),
    ODU3(40_000),
    ODU4(100_000),
    ODUC1(100_000),
    ODUC2(200_000),
    ODUC3(300_000),
    ODUC4(400_000),
    ODUC5(500_000),
    ODUC6(600_000),
    ODUC7(700_000),
    ODUC8(800_000),
    ODUC9(900_000);

    private final long bitRate;

    OduSignalType(long bitRate) {
        this.bitRate = bitRate;
    }

    /**
     * Return the bit rate in Mbps of the port.
     * @return bit rate
     */
    public long bitRate() {
        return this.bitRate;
    }

    /**
     * Returns the number of tributary slots of the OduSignalType.
     * Each TributarySlot is 1.25Gbps.
     * @return number of tributary slots
     */
    public int tributarySlots() {
        return (int) (this.bitRate() / OduSignalType.ODU0.bitRate());
    }

}
