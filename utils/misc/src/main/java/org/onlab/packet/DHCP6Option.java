/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onlab.packet;

/**
 * Representation of an DHCPv6 Option.
 * Base on RFC-3315.
 */
public class DHCP6Option {
    private short code;
    private short length;
    private byte[] data;

    /**
     * Sets the code of this option.
     *
     * @param code the code to set
     */
    public void setCode(short code) {
        this.code = code;
    }

    /**
     * Sets the data and length of this option.
     *
     * @param data the data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Sets length of this option.
     *
     * @param length the length to set
     */
    public void setLength(short length) {
        this.length = length;
    }

    /**
     * Gets the code of this option.
     *
     * @return the code
     */
    public short getCode() {
        return code;
    }

    /**
     * Gets the length of this option.
     *
     * @return the length of this option
     */
    public short getLength() {
        return length;
    }

    /**
     * Gets the data of this option.
     *
     * @return the data of this option
     */
    public byte[] getData() {
        return data;
    }
}
