/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.pcep.api;

import org.onosproject.net.Port;

/**
 * Abstraction of a network infrastructure link.
 */
public interface PcepLink extends PcepOperator {

    public enum SubType {
        /**
         * Optical Transmission Section Link.
         */
        OTS,

        /**
         * Optical Physical Section Link.
         */
        OPS,

        /**
         * User-to-Network Interface Link.
         */
        UNI,

        /**
         * Optical channel Data Unit-k link.
         */
        ODUk,

        /**
         * Optical Transport Network link.
         */
        OTU,
    }


    public enum PortType {
        ODU_PORT, OCH_PORT, OMS_PORT
    }

    /**
     * Returns the link endpoint port type.
     *
     * @return endpoint port type
     */
    public PortType portType();

    /**
     * Returns the link sub type,OTS,OPS,PKT_OPTICAL or ODUK.
     *
     * @return link subType
     */

    public SubType linkSubType();

    /**
     * Returns the link state, up or down.
     *
     * @return link state
     */
    public String linkState();

    /**
     * Returns the distance of a link.
     *
     * @return distance
     */
    public int linkDistance();

    /**
     * Returns the capacity type of a link,1: WAVELENGTHNUM, 2:SLOTNUM, 3,
     * BANDWIDTH.
     *
     * @return capacity type
     */
    public String linkCapacityType();

    /**
     * Returns the available capacity value ,such as available bandwidth.
     *
     * @return availValue
     */
    public int linkAvailValue();

    /**
     * Returns the max capacity value ,such as max bandwidth.
     *
     * @return maxValue
     */
    public int linkMaxValue();

    /**
     * Returns the source device did of a link.
     *
     * @return source did
     */
    public PcepDpid linkSrcDeviceID();

    /**
     * Returns the destination device did of a link.
     *
     * @return destination did
     */
    public PcepDpid linkDstDeviceId();

    /**
     * Returns the source port of a link.
     *
     * @return port number
     */
    public Port linkSrcPort();

    /**
     * Returns the destination port of a link.
     *
     * @return port number
     */
    public Port linkDstPort();

}
