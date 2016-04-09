/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.pcepio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.types.PcepObjectHeader;

/**
 * Abstraction of an entity providing PCEP Metric Object.
 */
public interface PcepMetricObject {

    /**
     * Returns Metric value in Metric Object.
     *
     * @return Metric value
     */
    int getMetricVal();

    /**
     * Sets Metric value in Metric Object with specified value.
     *
     * @param value Metric value
     */
    void setMetricVal(int value);

    /**
     * Returns Y flag in Metric Object.
     *
     * @return Y flag in Metric Object
     */
    byte getYFlag();

    /**
     * Sets Y flag in Metric Object with specified value.
     *
     * @param value Y flag
     */
    void setYFlag(byte value);

    /**
     * Returns C flag in Metric Object.
     *
     * @return C flag in Metric Object
     */
    boolean getCFlag();

    /**
     * Sets C flag in Metric Object with specified value.
     *
     * @param value C flag
     */
    void setCFlag(boolean value);

    /**
     * Returns B flag in Metric Object.
     *
     * @return B flag in Metric Object
     */
    boolean getBFlag();

    /**
     * Sets B flag in Metric Object with specified value.
     *
     * @param value B flag
     */
    void setBFlag(boolean value);

    /**
     * Returns BType field in Metric Object.
     *
     * @return BType field in Metric Object
     */
    byte getBType();

    /**
     * Sets BType field in Metric Object with specified value.
     *
     * @param value BType field
     */
    void setBType(byte value);

    /**
     * Writes the Metric Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing METRIC object into Channel Buffer.
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build Metric object.
     */
    interface Builder {

        /**
         * Builds Metric Object.
         *
         * @return Metric Object
         * @throws PcepParseException when mandatory object is not set
         */
        PcepMetricObject build() throws PcepParseException;

        /**
         * Returns Metric object header.
         *
         * @return Metric object header
         */
        PcepObjectHeader getMetricObjHeader();

        /**
         * Sets Metric object header and returns its builder.
         *
         * @param obj Metric object header
         * @return Builder by setting Metric object header
         */
        Builder setMetricObjHeader(PcepObjectHeader obj);

        /**
         * Returns Metric value in Metric Object.
         *
         * @return Metric value
         */
        int getMetricVal();

        /**
         * Sets Metric Value in Metric Object and returns its builder.
         *
         * @param value Metric Value
         * @return Builder by setting Metric Value
         */
        Builder setMetricVal(int value);

        /**
         * Returns Flags in Metric Object.
         *
         * @return Flags in Metric Object
         */
        byte getYFlag();

        /**
         * Sets Flags in Metric Object and returns its builder.
         *
         * @param value Flags
         * @return Builder by setting Flags
         */
        Builder setYFlag(byte value);

        /**
         * Returns C flag in Metric Object.
         *
         * @return C flag in Metric Object
         */
        boolean getCFlag();

        /**
         * Sets C flag in Metric Object and returns its builder.
         *
         * @param value C flag
         * @return Builder by setting C flag
         */
        Builder setCFlag(boolean value);

        /**
         * Returns B flag in Metric Object.
         *
         * @return B flag in Metric Object
         */
        boolean getBFlag();

        /**
         * Sets B flag in Metric Object and returns its builder.
         *
         * @param value B flag
         * @return Builder by setting B flag
         */
        Builder setBFlag(boolean value);

        /**
         * Returns BType field in Metric Object.
         *
         * @return BType field in Metric Object
         */
        byte getBType();

        /**
         * Sets B Type field in Metric Object and returns its builder.
         *
         * @param value B Type field
         * @return Builder by setting B Type field
         */
        Builder setBType(byte value);

        /**
         * Sets P flag in Metric object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in Metric object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
