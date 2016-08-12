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
package org.onosproject.net.flow;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;

/**
 * Represents a generalized match &amp; action pair to be applied to an
 * infrastructure device.
 */
public interface FlowRule {

    int MAX_TIMEOUT = 60;
    int MIN_PRIORITY = 0;
    int MAX_PRIORITY = 65535;

    /**
     * Reason for flow parameter received from switches.
     * Used to check reason parameter in flows.
     */
    enum FlowRemoveReason {
        IDLE_TIMEOUT,
        HARD_TIMEOUT,
        DELETE,
        GROUP_DELETE,
        METER_DELETE,
        EVICTION,
        NO_REASON;

        /**
         * Covert short to enum.
         * @return reason in enum
         * @param reason remove reason in integer
         */
        public static FlowRemoveReason parseShort(short reason) {
            switch (reason) {
                case -1 :
                    return NO_REASON;
                case 0:
                    return IDLE_TIMEOUT;
                case 1:
                    return HARD_TIMEOUT;
                case 2 :
                    return DELETE;
                case 3:
                    return GROUP_DELETE;
                case 4:
                    return METER_DELETE;
                case 5:
                    return EVICTION;
                default :
                    return NO_REASON;
            }
        }
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
     * Returns the hard timeout for this flow requested by an application.
     * This parameter configure switch's flow hard timeout.
     * In case of controller-switch connection lost, this variable can be useful.
     * @return integer value of the hard Timeout
     */
    int hardTimeout();

    /**
     * Returns the reason for the flow received from switches.
     *
     * @return FlowRemoveReason value of reason
     */
    FlowRemoveReason reason();

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
     * {@inheritDoc}
     *
     * Equality for flow rules only considers 'match equality'. This means that
     * two flow rules with the same match conditions will be equal, regardless
     * of the treatment or other characteristics of the flow.
     *
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if this object is the same as the obj
     *          argument; {@code false} otherwise.
     */
    boolean equals(Object obj);

    /**
     * Returns whether this flow rule is an exact match to the flow rule given
     * in the argument.
     * <p>
     * Exact match means that deviceId, priority, selector,
     * tableId, flowId and treatment are equal. Note that this differs from
     * the notion of object equality for flow rules, which does not consider the
     * flowId or treatment when testing equality.
     * </p>
     *
     * @param rule other rule to match against
     * @return true if the rules are an exact match, otherwise false
     */
    boolean exactMatch(FlowRule rule);

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
         * Sets the idle timeout parameter in flow table.
         *
         * Will automatically make it permanent or temporary if the timeout is 0 or not, respectively.
         * @param timeout an integer
         * @return this
         */
        default Builder withIdleTimeout(int timeout) {
            if (timeout == 0) {
                return makePermanent();
            } else {
                return makeTemporary(timeout);
            }
        }

        /**
         * Sets hard timeout parameter in flow table.
         * @param timeout an integer
         * @return this
         */
        Builder withHardTimeout(int timeout);

        /**
         * Sets reason parameter received from switches .
         * @param reason a short
         * @return this
         */
        Builder withReason(FlowRemoveReason reason);

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
     * @deprecated in Junco release
     */
    @Deprecated
    FlowRuleExtPayLoad payLoad();
}
