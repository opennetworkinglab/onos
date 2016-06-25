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

/**
 * Abstraction of an entity providing PCEP Label Range Reservation Message.
 */
public interface PcepLabelRangeResvMsg extends PcepObject, PcepMessage {

    @Override
    PcepVersion getVersion();

    @Override
    PcepType getType();

    /**
     * Returns LabelRange field in Label Range Reservation message.
     *
     * @return LabelRange field
     */
    PcepLabelRange getLabelRange();

    /**
     * Sets LabelRange field in Label Range Reservation message with specified value.
     *
     * @param lR label range object
     */
    void setLabelRange(PcepLabelRange lR);

    @Override
    void writeTo(ChannelBuffer channelBuffer) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build Label Range Reservation message.
     */
    interface Builder extends PcepMessage.Builder {

        @Override
        PcepLabelRangeResvMsg build();

        @Override
        PcepVersion getVersion();

        @Override
        PcepType getType();

        /**
         * Returns LabelRange field in Label Range Reservation message.
         *
         * @return LabelRange object
         */
        PcepLabelRange getLabelRange();

        /**
         * Sets LabelRange field and returns its Builder.
         *
         * @param lR label range object
         * @return builder by setting LabelRange field
         */
        Builder setLabelRange(PcepLabelRange lR);
    }
}
