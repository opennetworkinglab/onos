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

import java.util.LinkedList;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;

/**
 * Abstraction of an entity which Provides List of PCEP Attributes.
 */
public interface PcepAttribute {

    /**
     * writes lspa , bandwidth , Metriclist and Iro objects to the channel.
     *
     * @param bb of type channel buffer.
     * @return object length index.
     * @throws PcepParseException while writing objects to channel buffer
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Returns PcepLspaObject.
     *
     * @return LspaObject
     */
    PcepLspaObject getLspaObject();

    /**
     * Returns PcepBandwidthObject.
     *
     * @return BandwidthObject
     */
    PcepBandwidthObject getBandwidthObject();

    /**
     * Returns PcepIroObject.
     *
     * @return iroObject
     */
    PcepIroObject getIroObject();

    /**
     * Sets the PcepBandwidthObject.
     *
     * @param bandwidthObject bandwidth object
     */
    void setBandwidthObject(PcepBandwidthObject bandwidthObject);

    /**
     * Sets the PcepLspaObject.
     *
     * @param lspaObject lspa object
     */
    void setLspaObject(PcepLspaObject lspaObject);

    /**
     * Sets the PcepIroObject.
     *
     * @param iroObject iro object
     */
    void setIroObject(PcepIroObject iroObject);

    /**
     * Returns PcepMetricObject List.
     *
     * @return list of metric objects
     */
    LinkedList<PcepMetricObject> getMetricObjectList();

    /**
     * Sets PcepMetricObject List.
     *
     * @param llMetricList list of metric objects
     */
    void setMetricObjectList(LinkedList<PcepMetricObject> llMetricList);

    /**
     * Builder interface with get and set functions to build PcepAttribute.
     */
    interface Builder {

        /**
         * Builds PcepAttribute.
         *
         * @return PcepAttribute
         */
        PcepAttribute build();

        /**
         * Returns PcepLspaObject.
         *
         * @return LspaObject
         */
        PcepLspaObject getLspaObject();

        /**
         * Returns PcepBandwidthObject.
         *
         * @return BandwidthObject
         */
        PcepBandwidthObject getBandwidthObject();

        /**
         * Returns PcepIroObject.
         *
         * @return iroObject
         */
        PcepIroObject getIroObject();

        /**
         * Sets the PcepBandwidthObject.
         *
         * @param bandwidthObject bandwidth object
         * @return Builder object for PcepAttrubute
         */
        Builder setBandwidthObject(PcepBandwidthObject bandwidthObject);

        /**
         * Sets the PcepLspaObject.
         *
         * @param lspaObject lspa object
         * @return Builder object for PcepAttrubute
         */
        Builder setLspaObject(PcepLspaObject lspaObject);

        /**
         * Sets the PcepIroObject.
         *
         * @param iroObject iro object
         * @return Builder object for PcepAttrubute
         */
        Builder setIroObject(PcepIroObject iroObject);

        /**
         * Returns PcepMetricObject List.
         *
         * @return list of metric objects
         */
        LinkedList<PcepMetricObject> getMetricObjectList();

        /**
         * Sets PcepMetricObject List.
         *
         * @param llMetricList list of metric objects
         * @return Builder object for PcepAttrubute
         */
        Builder setMetricObjectList(LinkedList<PcepMetricObject> llMetricList);
    }
}
