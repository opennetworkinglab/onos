/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.flow;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;

/**
 * Represents a generalized match &amp; action pair to be applied to an
 * infrastructure device.
 */
public interface FlowRule {

    static final int MAX_TIMEOUT = 60;
    static final int MIN_PRIORITY = 0;

    /**
     * The FlowRule type is used to determine in which table the flow rule needs
     * to be put for multi-table support switch. For single table switch,
     * Default is used.
     *
     * @deprecated in Cardinal Release
     */
    @Deprecated
    static enum Type {
        /*
         * Default type - used in flow rule for single table switch NOTE: this
         * setting should not be used as Table 0 in a multi-table pipeline
         */
        DEFAULT,
        /* Used in flow entry for IP table */
        IP,
        /* Used in flow entry for MPLS table */
        MPLS,
        /* Used in flow entry for ACL table */
        ACL,

        /* VLAN-to-MPLS table */
        VLAN_MPLS,

        /* VLAN table */
        VLAN,

        /* Ethtype table */
        ETHER,

        /* Class of Service table */
        COS,

        /* Table 0 in a multi-table pipeline */
        FIRST,
    }

    /**
     * Returns the ID of this flow.
     *
     * @return the flow ID
     */
    FlowId id();

    /**
     * Returns the application id of this flow.
     *
     * @return an applicationId
     */
    short appId();

    /**
     * Returns the group id of this flow.
     *
     * @return an groupId
     */
    GroupId groupId();

    /**
     * Returns the flow rule priority given in natural order; higher numbers
     * mean higher priorities.
     *
     * @return flow rule priority
     */
    int priority();

    /**
     * Returns the identity of the device where this rule applies.
     *
     * @return device identifier
     */
    DeviceId deviceId();

    /**
     * Returns the traffic selector that identifies what traffic this rule
     * should apply to.
     *
     * @return traffic selector
     */
    TrafficSelector selector();

    /**
     * Returns the traffic treatment that applies to selected traffic.
     *
     * @return traffic treatment
     */
    TrafficTreatment treatment();

    /**
     * Returns the timeout for this flow requested by an application.
     *
     * @return integer value of the timeout
     */
    int timeout();

    /**
     * Returns whether the flow is permanent i.e. does not time out.
     *
     * @return true if the flow is permanent, otherwise false
     */
    boolean isPermanent();

    /**
     * Returns the table id for this rule.
     *
     * @return an integer.
     */
    int tableId();

    /**
     * A flowrule builder.
     */
    interface Builder {

        /**
         * Assigns a cookie value to this flowrule. Mutually exclusive with the
         * fromApp method. This method is intended to take a cookie value from
         * the dataplane and not from the application.
         *
         * @param cookie a long value
         * @return this
         */
        Builder withCookie(long cookie);

        /**
         * Assigns the application that built this flow rule to this object.
         * The short value of the appId will be used as a basis for the
         * cookie value computation. It is expected that application use this
         * call to set their application id.
         *
         * @param appId an application id
         * @return this
         */
        Builder fromApp(ApplicationId appId);

        /**
         * Sets the priority for this flow rule.
         *
         * @param priority an integer
         * @return this
         */
        Builder withPriority(int priority);

        /**
         * Sets the deviceId for this flow rule.
         *
         * @param deviceId a device id
         * @return this
         */
        Builder forDevice(DeviceId deviceId);

        /**
         * Sets the table id for this flow rule. Default value is 0.
         *
         * @param tableId an integer
         * @return this
         */
        Builder forTable(int tableId);

        /**
         * Sets the selector (or match field) for this flow rule.
         *
         * @param selector a traffic selector
         * @return this
         */
        Builder withSelector(TrafficSelector selector);

        /**
         * Sets the traffic treatment for this flow rule.
         *
         * @param treatment a traffic treatment
         * @return this
         */
        Builder withTreatment(TrafficTreatment treatment);

        /**
         * Makes this rule permanent on the dataplane.
         *
         * @return this
         */
        Builder makePermanent();

        /**
         * Makes this rule temporary and timeout after the specified amount
         * of time.
         *
         * @param timeout an integer
         * @return this
         */
        Builder makeTemporary(int timeout);

        /**
         * Builds a flow rule object.
         *
         * @return a flow rule.
         */
        FlowRule build();

    }

    /**
     * Returns the third party original flow rule.
     *
     * @return FlowRuleExtPayLoad
     */
    FlowRuleExtPayLoad payLoad();
}
