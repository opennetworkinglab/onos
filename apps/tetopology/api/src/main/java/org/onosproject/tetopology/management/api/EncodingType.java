/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api;

/**
 * LSP encoding type.
 * See RFC 3471 for details.
 */
public enum EncodingType {

    /**
     * Designates Packet LSP encoding.
     */
    LSP_ENCODING_PACKET(1),

    /**
     * Designates Ethernet LSP encoding.
     */
    LSP_ENCODING_ETHERNET(2),

    /**
     * Designates ANSI/ETSI PDH encoding.
     */
    LSP_ENCODING_PDH(3),

    /**
     * Designates SDH ITU-T G.707 / SONET ANSI T1.105 LSP encoding.
     */
    LSP_ENCODING_SDH(5),

    /**
     * Designates Digital Wrapper LSP encoding.
     */
    LSP_ENCODING_DIGITAL_WRAPPER(7),

    /**
     * Designates Lambda (photonic) LSP encoding.
     */
    LSP_ENCODING_LAMBDA(8),

    /**
     * Designates Fiber LSP encoding.
     */
    LSP_ENCODING_FIBER(9),

    /**
     * Designates Fiber Channel LSP encoding.
     */
    LSP_ENCODING_FIBER_CHANNEL(11),

    /**
     * Designates G.709 ODUk (Digital Path)LSP encoding.
     */
    LSP_ENCODING_ODUK(12);

    private int value;

    /**
     * Creates an instance of EncodingType.
     *
     * @param value value of encoding type
     */
    EncodingType(int value) {
        this.value = value;
    }

    /**
     * Returns the corresponding integer value of the encoding type.
     *
     * @return corresponding integer value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the encoding type constant corresponding to the given integer
     * value. If the given value cannot be mapped to any valid encoding type,
     * a null is returned.
     *
     * @param value integer value
     * @return corresponding encoding type constant
     */
    public static EncodingType of(int value) {
        switch (value) {
            case 1:
                return LSP_ENCODING_PACKET;
            case 2:
                return LSP_ENCODING_ETHERNET;
            case 3:
                return LSP_ENCODING_PDH;
            case 5:
                return LSP_ENCODING_SDH;
            case 7:
                return LSP_ENCODING_DIGITAL_WRAPPER;
            case 8:
                return LSP_ENCODING_LAMBDA;
            case 9:
                return LSP_ENCODING_FIBER;
            case 11:
                return LSP_ENCODING_FIBER_CHANNEL;
            case 12:
                return LSP_ENCODING_ODUK;
            default:
                return null;
        }
    }
}
