/*
 * Copyright 2014-present Open Networking Foundation
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

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.*;

/**
 *
 */
public class Data extends BasePacket {
    protected byte[] data;

    /**
     *
     */
    public Data() {
        data = new byte[0];
    }

    /**
     * @param data the data
     */
    public Data(final byte[] data) {
        this.data = data;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * @param data
     *            the data to set
     * @return self
     */
    public Data setData(final byte[] data) {
        this.data = data;
        return this;
    }

    @Override
    public byte[] serialize() {
        return this.data;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 1571;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(this.data);
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
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Data)) {
            return false;
        }
        final Data other = (Data) obj;
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for generic payload data.
     *
     * @return deserializer function
     */
    public static Deserializer<Data> deserializer() {
        return (data, offset, length) -> {
            // Allow zero-length data for now
            if (length == 0) {
                return new Data();
            }

            checkInput(data, offset, length, 1);

            Data dataObject = new Data();

            dataObject.data = Arrays.copyOfRange(data, offset, offset + length);

            return dataObject;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("data", Arrays.toString(data))
                .toString();
    }
}
