/*
 * Copyright 2021-present Open Networking Foundation
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

#include <core.p4>
#include <v1model.p4>

#include "../header.p4"

control PreNext(inout parsed_headers_t hdr,
                inout fabric_metadata_t fabric_metadata) {
    /*
     * Next MPLS table.
     * Set the MPLS label based on the next ID.
     */

    direct_counter(CounterType.packets_and_bytes) next_mpls_counter;

    action set_mpls_label(mpls_label_t label) {
        fabric_metadata.mpls_label = label;
        next_mpls_counter.count();
    }

    table next_mpls {
        key = {
            fabric_metadata.next_id: exact @name("next_id");
        }
        actions = {
            set_mpls_label;
            @defaultonly nop;
        }
        const default_action = nop();
        counters = next_mpls_counter;
        size = NEXT_MPLS_TABLE_SIZE;
    }

    /*
     * Next VLAN table.
     * Modify VLAN ID based on next ID.
     */

    direct_counter(CounterType.packets_and_bytes) next_vlan_counter;

    action set_vlan(vlan_id_t vlan_id) {
        fabric_metadata.vlan_id = vlan_id;
        next_vlan_counter.count();
    }

#ifdef WITH_DOUBLE_VLAN_TERMINATION
    action set_double_vlan(vlan_id_t outer_vlan_id, vlan_id_t inner_vlan_id) {
        set_vlan(outer_vlan_id);
        fabric_metadata.push_double_vlan = _TRUE;
        fabric_metadata.inner_vlan_id = inner_vlan_id;
#ifdef WITH_BNG
        fabric_metadata.bng.s_tag = outer_vlan_id;
        fabric_metadata.bng.c_tag = inner_vlan_id;
#endif // WITH_BNG
    }
#endif // WITH_DOUBLE_VLAN_TERMINATION

    table next_vlan {
        key = {
            fabric_metadata.next_id: exact @name("next_id");
        }
        actions = {
            set_vlan;
#ifdef WITH_DOUBLE_VLAN_TERMINATION
            set_double_vlan;
#endif // WITH_DOUBLE_VLAN_TERMINATION
            @defaultonly nop;
        }
        const default_action = nop();
        counters = next_vlan_counter;
        size = NEXT_VLAN_TABLE_SIZE;
    }

    apply {
        next_mpls.apply();
        next_vlan.apply();
    }
}
