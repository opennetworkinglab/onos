/*
 * Copyright 2016-present Open Networking Laboratory
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

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;

/**
 * Abstraction of an entity providing PCEP LS-Report Message.
 */
public interface PcepLSReportMsg extends PcepObject, PcepMessage {

    @Override
    PcepVersion getVersion();

    @Override
    PcepType getType();

    /**
     * Returns list of PCEP LS Objects.
     *
     * @return list of PCEP LS Objects
     */
    List<PcepLSObject> getLSReportList();

    /**
     * Sets list of Optional Tlvs in LS-Report Message.
     *
     * @param lsReportList list of optional Tlvs
     */
    void setLSReportList(List<PcepLSObject> lsReportList);

    @Override
    void writeTo(ChannelBuffer channelBuffer) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build LS-Report message.
     */
    interface Builder extends PcepMessage.Builder {

        @Override
        PcepLSReportMsg build();

        @Override
        PcepVersion getVersion();

        @Override
        PcepType getType();

        /**
         * Returns list of LS Object in LS Report Message.
         *
         * @return list of LS Objects
         */
        List<PcepLSObject> getLSReportList();

        /**
         * Sets list of LS Objects and returns its builder.
         *
         * @param lsReportList list of LS Objects
         * @return Builder object for LS-Report message
         */
        Builder setLSReportList(List<PcepLSObject> lsReportList);
    }
}
