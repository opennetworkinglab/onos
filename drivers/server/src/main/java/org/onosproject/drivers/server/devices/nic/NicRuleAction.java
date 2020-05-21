/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.server.devices.nic;

import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_RULE_ACTION_TYPE_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_RULE_ACTION_VAL_NULL;

/**
 * Definition of network interface card (NIC) rule action.
 * This class acts as a translator from FlowRule objects
 * to NicFlowRule objects and vice versa.
 */
public class NicRuleAction {

    /**
     * Set of possible NIC rule actions.
     * Source: https://doc.dpdk.org/guides/prog_guide/rte_flow.html
     */
    public enum Action {

        /**
         * Leaves traffic up for additional processing
         * by subsequent flow rules.
         */
        PASSTHRU("passthru"),
        /**
         * The packet must be redirected to a group on this NIC.
         */
        JUMP("jump"),
        /**
         * The packet must be marked with a specific value.
         */
        MARK("mark"),
        /**
         * The packet must be marked without a specific value.
         */
        FLAG("flag"),
        /**
         * The packet must be counted.
         */
        COUNT("count"),
        /**
         * The packet must be placed into a hardware queue.
         * This field is then used by a NIC's Flow Director component.
         */
        QUEUE("queue"),
        /**
         * The packet must be placed into a hardware queue.
         * This field is then used by a NIC's RSS component.
         */
        RSS("rss"),
        /**
         * The packet must be sent to the physical function of the NIC.
         */
        PF("pf"),
        /**
         * The packet must be sent to a virtual function of the NIC.
         */
        VF("vf"),
        /**
         * The packet must be sent to a physical port index of the NIC.
         */
        PHY_PORT("phy-port"),
        /**
         * The packet must be sent to a NIC port ID.
         */
        PORT_ID("port-id"),
        /**
         * The packet must undergo a stage of metering and policing.
         */
        METER("meter"),
        /**
         * The packet must undergo a security action.
         */
        SECURITY("security"),
        /**
         * Sets the MPLS TTL as defined by the OpenFlow Switch Specification.
         */
        OF_SET_MPLS_TTL("of_set_mpls_ttl"),
        /**
         * Decrements the MPLS TTL as defined by the OpenFlow Switch Specification.
         */
        OF_DEC_MPLS_TTL("of_dec_mpls_ttl"),
        /**
         * Sets the IP TTL as defined by the OpenFlow Switch Specification.
         */
        OF_SET_NW_TTL("of_set_nw_ttl"),
        /**
         * Decrements the IP TTL as defined by the OpenFlow Switch Specification.
         */
        OF_DEC_NW_TTL("of_dec_nw_ttl"),
        /**
         * Implements OFPAT_COPY_TTL_IN (“copy TTL “inwards” – from outermost to next-to-outermost”)
         * as defined by the OpenFlow Switch Specification.
         */
        OF_COPY_TTL_IN("of_copy_ttl_in"),
        /**
         * Implements OFPAT_COPY_TTL_OUT (“copy TTL “outwards” – from next-to-outermost to outermost”)
         * as defined by the OpenFlow Switch Specification.
         */
        OF_COPY_TTL_OUT("of_copy_ttl_out"),
        /**
         * Pops the outer MPLS tag as defined by the OpenFlow Switch Specification.
         */
        OF_POP_MPLS("of_pop_mpls"),
        /**
         * Pushes a new MPLS tag as defined by the OpenFlow Switch Specification.
         */
        OF_PUSH_MPLS("of_push_mpls"),
        /**
         * Pops the outer VLAN tag as defined by the OpenFlow Switch Specification.
         */
        OF_POP_VLAN("of_pop_vlan"),
        /**
         * Pushes a new VLAN tag as defined by the OpenFlow Switch Specification.
         */
        OF_PUSH_VLAN("of_push_vlan"),
        /**
         * Sets the 802.1q VLAN ID as defined by the OpenFlow Switch Specification.
         */
        OF_SET_VLAN_VID("of_set_vlan_vid"),
        /**
         * Sets the 802.1q VLAN priority as defined by the OpenFlow Switch Specification.
         */
        OF_SET_VLAN_PCP("of_set_vlan_pcp"),
        /**
         * Performs a VXLAN encapsulation action by encapsulating the matched flow
         * in an VXLAN tunnel.
         */
        VXLAN_ENCAP("vxlan_encap"),
        /**
         * Performs a decapsulation action by stripping all headers of the VXLAN tunnel
         * network overlay from the matched flow.
         */
        VXLAN_DECAP("vxlan_decap"),
        /**
         * Performs a NVGRE encapsulation action by encapsulating the matched flow
         * in an NVGRE tunnel.
         */
        NVGRE_ENCAP("nvgre_encap"),
        /**
         * Performs a decapsulation action by stripping all headers of the NVGRE tunnel
         * network overlay from the matched flow.
         */
        NVGRE_DECAP("nvgre_decap"),
        /**
         * The packet must be dropped.
         */
        DROP("drop"),
        /**
         * Denotes end of actions.
         */
        END("end");

        protected String action;

        private Action(String action) {
            this.action = action.toLowerCase();
        }

        @Override
        public String toString() {
            return action;
        }

    }

    private final Action actionType;
    private final long actionValue;

    // Statically maps NIC rule action types to their fields
    private static final Map<Action, String> ACTION_FIELD =
        new HashMap<Action, String>();

    static {
        ACTION_FIELD.put(Action.PASSTHRU, "");
        ACTION_FIELD.put(Action.JUMP, "group");
        ACTION_FIELD.put(Action.MARK, "id");
        ACTION_FIELD.put(Action.FLAG, "");
        ACTION_FIELD.put(Action.COUNT, "");
        ACTION_FIELD.put(Action.QUEUE, "index");
        ACTION_FIELD.put(Action.RSS, "queue");
        ACTION_FIELD.put(Action.PF, "");
        ACTION_FIELD.put(Action.VF, "id");
        ACTION_FIELD.put(Action.PHY_PORT, "index");
        ACTION_FIELD.put(Action.PORT_ID, "id");
        ACTION_FIELD.put(Action.METER, "mtr_id");
        ACTION_FIELD.put(Action.SECURITY, "security_session");
        ACTION_FIELD.put(Action.OF_SET_MPLS_TTL, "mpls_ttl");
        ACTION_FIELD.put(Action.OF_DEC_MPLS_TTL, "");
        ACTION_FIELD.put(Action.OF_SET_NW_TTL, "nw_ttl");
        ACTION_FIELD.put(Action.OF_DEC_NW_TTL, "");
        ACTION_FIELD.put(Action.OF_COPY_TTL_IN, "");
        ACTION_FIELD.put(Action.OF_COPY_TTL_OUT, "");
        ACTION_FIELD.put(Action.OF_POP_MPLS, "ethertype");
        ACTION_FIELD.put(Action.OF_PUSH_MPLS, "ethertype");
        ACTION_FIELD.put(Action.OF_POP_VLAN, "");
        ACTION_FIELD.put(Action.OF_PUSH_VLAN, "ethertype");
        ACTION_FIELD.put(Action.OF_SET_VLAN_VID, "vlan_vid");
        ACTION_FIELD.put(Action.OF_SET_VLAN_PCP, "vlan_pcp");
        ACTION_FIELD.put(Action.VXLAN_ENCAP, "definition");
        ACTION_FIELD.put(Action.VXLAN_DECAP, "");
        ACTION_FIELD.put(Action.NVGRE_ENCAP, "definition");
        ACTION_FIELD.put(Action.NVGRE_DECAP, "");
        ACTION_FIELD.put(Action.DROP, "");
        ACTION_FIELD.put(Action.END, "");
    }

    public static final long DEF_ACTION_VALUE = (long) 0;
    public static final long NO_ACTION_VALUE  = (long) -1;

    public NicRuleAction(Action actionType, long actionValue) {
        checkNotNull(actionType, MSG_NIC_FLOW_RULE_ACTION_TYPE_NULL);
        checkArgument(actionValue >= 0, MSG_NIC_FLOW_RULE_ACTION_VAL_NULL);

        this.actionType = actionType;
        this.actionValue = actionValue;
    }

    public NicRuleAction(Action actionType) {
        checkNotNull(actionType, MSG_NIC_FLOW_RULE_ACTION_TYPE_NULL);

        this.actionType = actionType;
        this.actionValue = NO_ACTION_VALUE;
    }

    /**
     * Returns the NIC's action type associated with a rule.
     *
     * @return NIC's rule action type
     */
    public Action actionType() {
        return actionType;
    }

    /**
     * Returns the NIC's action value associated with a rule.
     *
     * @return NIC's rule action value
     */
    public long actionValue() {
        return actionValue;
    }

    /**
     * Returns the NIC's action field associated with its action type.
     *
     * @return NIC's rule action field
     */
    public String actionField() {
        return ACTION_FIELD.get(actionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionType, actionValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof NicRuleAction) {
            NicRuleAction that = (NicRuleAction) obj;
            return  Objects.equals(actionType, that.actionType) &&
                    Objects.equals(actionValue, that.actionValue);
        }

        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("NIC rule action type", actionType())
                .add("NIC rule action value", actionValue())
                .toString();
    }

}
