/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.util.Arrays;

/**
 * Representation of DHCPOption field.
 */
public class DHCPOption {
    protected byte code;
    protected byte length;
    protected byte[] data;

    /**
     * @return the code
     */
    public byte getCode() {
        return this.code;
    }

    /**
     * @param code the code to set
     * @return this
     */
    public DHCPOption setCode(final byte code) {
        this.code = code;
        return this;
    }

    /**
     * @return the length
     */
    public byte getLength() {
        return this.length;
    }

    /**
     * @param length the length to set
     * @return this
     */
    public DHCPOption setLength(final byte length) {
        this.length = length;
        return this;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * @param data the data to set
     * @return this
     */
    public DHCPOption setData(final byte[] data) {
        this.data = data;
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.code;
        result = prime * result + Arrays.hashCode(this.data);
        result = prime * result + this.length;
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DHCPOption)) {
            return false;
        }
        final DHCPOption other = (DHCPOption) obj;
        if (this.code != other.code) {
            return false;
        }
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        if (this.length != other.length) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "DHCPOption [code=" + this.code + ", length=" + this.length
                + ", data=" + Arrays.toString(this.data) + "]";
    }
}
