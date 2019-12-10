/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.net.behaviour;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.pi.runtime.PiCounterCellData;

import java.util.Map;

/**
 * BNG programmable behavior. Provides means to update device forwarding state
 * to perform BNG user plane functions (e.g., tunnel termination, accounting,
 * etc.). An implementation of this API should not write state directly to the
 * device, but instead, always rely on core ONOS subsystems (e.g.,
 * FlowRuleService, GroupService, etc).
 */
public interface BngProgrammable extends HandlerBehaviour {

    /**
     * Provision rules to punt BNG control packets to ONOS. Control packets
     * might include Session Discovery, Authentication, Address Assignment etc.
     * Apps are expected to call this method as the first one when they are
     * ready to process attachment connection requests.
     *
     * @param appId Application ID of the caller of this API.
     * @return True if initialized, false otherwise.
     */
    boolean init(ApplicationId appId);

    /**
     * Remove any state previously created by this API for the given application
     * ID.
     *
     * @param appId Application ID of the application using the
     *              BngProgrammable.
     * @throws BngProgrammableException while writing on BNG device.
     */
    void cleanUp(ApplicationId appId)
            throws BngProgrammableException;

    /**
     * Set up the necessary state to enable termination of the attachment
     * traffic. If the attachment is active, packets will be
     * forwarded/terminated after calling this method, if not they will be
     * dropped. State, if already present in the data plane, will not be cleaned
     * (e.g., counters).
     *
     * @param attachmentInfo Attachment information to configure the line
     *                       termination.
     * @throws BngProgrammableException while writing on BNG device.
     */
    void setupAttachment(Attachment attachmentInfo)
            throws BngProgrammableException;

    /**
     * Remove any state associated with the given attachment, including
     * termination flow rules. Calling this method while an attachment is
     * generating/receiving traffic, will eventually cause all packets to be
     * dropped for that attachment.
     *
     * @param attachmentInfo Attachment information to remove the line
     *                       termination.
     * @throws BngProgrammableException while writing on BNG device.
     */
    void removeAttachment(Attachment attachmentInfo)
            throws BngProgrammableException;

    /**
     * Read all counters for a given attachment and returns a map with keys
     * BngCounterType and values the ones obtained from the device. If a
     * specific BngCounterType is not found in the map, it means the device does
     * not support it.
     *
     * @param attachmentInfo Attachment information.
     * @return The counter values of the attachment.
     * @throws BngProgrammableException while reading from BNG device.
     */
    Map<BngCounterType, PiCounterCellData> readCounters(Attachment attachmentInfo)
            throws BngProgrammableException;

    /**
     * Read a specific counter value of a specific attachment. If the given
     * BngCounterType is not supported by the device, returns null.
     *
     * @param attachmentInfo Attachment information.
     * @param counter        The counter to be read.
     * @return The value of the specific counter.
     * @throws BngProgrammableException while reading from BNG device.
     */
    PiCounterCellData readCounter(Attachment attachmentInfo, BngCounterType counter)
            throws BngProgrammableException;

    /**
     * Read the control plane traffic counter of packets punted before
     * attachment creation (e.g., when an attachment is not created yet). If
     * unsupported, return null value.
     *
     * @return The value of the control traffic counter.
     * @throws BngProgrammableException while reading from BNG device.
     */
    PiCounterCellData readControlTrafficCounter()
            throws BngProgrammableException;

    /**
     * Reset the given counter of a specific attachment.
     *
     * @param attachmentInfo Attachment information.
     * @param counter        The counter to be reset.
     * @throws BngProgrammableException while writing on BNG device.
     */
    void resetCounter(Attachment attachmentInfo, BngCounterType counter)
            throws BngProgrammableException;

    /**
     * Reset the all the counters of a specific attachment.
     *
     * @param attachmentInfo Attachment information.
     * @throws BngProgrammableException while writing on BNG device.
     */
    void resetCounters(Attachment attachmentInfo)
            throws BngProgrammableException;

    /**
     * Reset the control plane traffic counter of packets punted before
     * attachment creation (e.g., when an attachment is not created yet).
     *
     * @throws BngProgrammableException while writing on BNG device.
     */
    void resetControlTrafficCounter()
            throws BngProgrammableException;

    /**
     * Counters to implement BNG accounting. Some of these counters can be
     * unsupported by the implementations.
     */
    enum BngCounterType {
        /**
         * Count the received packets in the downstream direction.
         */
        DOWNSTREAM_RX,

        /**
         * Count the transmitted packets in the downstream direction.
         */
        DOWNSTREAM_TX,

        /**
         * Count the dropped packets in the downstream direction.
         */
        DOWNSTREAM_DROPPED,

        /**
         * Count the received packets in the upstream direction.
         */
        UPSTREAM_RX,

        /**
         * Count the transmitted packets in the upstream direction.
         */
        UPSTREAM_TX,

        /**
         * Count the dropped packets in the upstream direction.
         */
        UPSTREAM_DROPPED,

        /**
         * Count the received control plane packets.
         */
        CONTROL_PLANE
    }

    /**
     * Immutable representation of an attachment in the BNG context. It
     * identifies a L2/L2.5 tunnel line between the RG and the BNG.
     */
    interface Attachment {

        /**
         * Returns the application that is responsible for managing the
         * attachment.
         *
         * @return The application ID.
         */
        ApplicationId appId();

        /**
         * Returns the VLAN S-tag of the attachment.
         *
         * @return The VLAN S-tag of the attachment.
         */
        VlanId sTag();

        /**
         * Returns the VLAN C-tag of the attachment.
         *
         * @return The VLAN C-tag of the attachment.
         */
        VlanId cTag();

        /**
         * Returns the MAC address of the attachment.
         *
         * @return The MAC address of the attachment.
         */
        MacAddress macAddress();

        /**
         * Returns the IP address of the attachment.
         *
         * @return The IP address of the attachment.
         */
        IpAddress ipAddress();

        /**
         * Defines if the line related to the attachment is active.
         *
         * @return True if the line is active, False otherwise.
         */
        boolean lineActive();

        /**
         * Returns the type of attachment.
         *
         * @return type of attachment
         */
        AttachmentType type();

        /**
         * Returns the PPPoE session ID of the attachment. This method is
         * meaningful only if the attachment type is PPPoE.
         *
         * @return The PPPoE session ID.
         */
        short pppoeSessionId();

        /**
         * Types of attachment.
         */
        enum AttachmentType {
            /**
             * PPPoE attachment.
             */
            PPPoE,

            /**
             * IPoE attachment.
             */
            // TODO: unsupported.
            IPoE
        }
    }

    /**
     * An exception indicating a an error happened in the BNG programmable
     * behaviour.
     */
    class BngProgrammableException extends Exception {
        /**
         * Creates a new exception for the given message.
         *
         * @param message message
         */
        public BngProgrammableException(String message) {
            super(message);
        }
    }
}


