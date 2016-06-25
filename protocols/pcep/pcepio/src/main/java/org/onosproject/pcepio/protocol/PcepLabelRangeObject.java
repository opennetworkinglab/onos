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
 * Abstraction of an entity providing PCEP LabelRange Object.
 */
public interface PcepLabelRangeObject {

    /**
     * Sets LabelRange Object header.
     *
     * @param obj LabelRange Object header
     */
    void setLabelRangeObjHeader(PcepObjectHeader obj);

    /**
     * Sets LabelType in LabelRange Object.
     *
     * @param labelType label type value
     */
    void setLabelType(byte labelType);

    /**
     * Sets RangeSize in LabelRange Object.
     *
     * @param rangeSize range size value
     */
    void setRangeSize(int rangeSize);

    /**
     * Sets LabelBase in LabelRange Object.
     *
     * @param labelBase label base value
     */
    void setLabelBase(int labelBase);

    /**
     * Returns LabelRange object header.
     *
     * @return LabelRange object header
     */
    PcepObjectHeader getLabelRangeObjHeader();

    /**
     * Returns LabelType field in LabelRange object.
     *
     * @return LabelType field in LabelRange object
     */
    byte getLabelType();

    /**
     * Returns RangeSize field in LabelRange object.
     *
     * @return RangeSize field in LabelRange object
     */
    int getRangeSize();

    /**
     * Returns LabelBase field in LabelRange object.
     *
     * @return LabelBase field in LabelRange object
     */
    int getLabelBase();

    /**
     * Writes the LabelRange Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing LABEL RANGE object into Channel Buffer.
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build LabelRange object.
     */
    interface Builder {

        /**
         * Builds LabelRange Object.
         *
         * @return LabelRange Object
         * @throws PcepParseException while building LABEL RANGE object.
         */
        PcepLabelRangeObject build() throws PcepParseException;

        /**
         * Returns LabelRange object header.
         *
         * @return LabelRange object header
         */
        PcepObjectHeader getLabelRangeObjHeader();

        /**
         * Sets LabelRange object header and returns its builder.
         *
         * @param obj LabelRange object header
         * @return Builder by setting LabelRange object header
         */
        Builder setLabelRangeObjHeader(PcepObjectHeader obj);

        /**
         * Returns LabelType field in LabelRange object.
         *
         * @return LabelType field in LabelRange object
         */
        byte getLabelType();

        /**
         * Sets LabelType field and returns its builder.
         *
         * @param labelType LabelType field
         * @return Builder by setting LabelType field
         */
        Builder setLabelType(byte labelType);

        /**
         * Returns RangeSize field in LabelRange object.
         *
         * @return RangeSize field in LabelRange object
         */
        int getRangeSize();

        /**
         * Sets RangeSize field and returns its builder.
         *
         * @param rangeSize RangeSize field
         * @return Builder by setting RangeSize field
         */
        Builder setRangeSize(int rangeSize);

        /**
         * Returns LabelBase field in LabelRange object.
         *
         * @return LabelBase field in LabelRange object
         */
        int getLabelBase();

        /**
         * Sets LabelBase field and returns its builder.
         *
         * @param labelBase LabelBase field
         * @return Builder by setting LabelBase field
         */
        Builder setLabelBase(int labelBase);

        /**
         * Sets P flag in TE object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in TE object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
