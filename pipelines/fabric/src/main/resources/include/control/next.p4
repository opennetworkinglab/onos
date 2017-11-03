/*
 * Copyright 2017-present Open Networking Foundation
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
#include "../action.p4"

control Next (
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fabric_metadata,
    inout standard_metadata_t standard_metadata) {
    direct_counter(CounterType.packets_and_bytes) next_id_mapping_counter;
    direct_counter(CounterType.packets_and_bytes) simple_counter;
    direct_counter(CounterType.packets_and_bytes) hashed_counter;
    direct_counter(CounterType.packets_and_bytes) broadcast_counter;
    action_selector(HashAlgorithm.crc16, 32w64, 32w16) ecmp_selector;

    action set_next_type(next_type_t next_type) {
        fabric_metadata.next_type = next_type;
    }

    action output(port_num_t port_num) {
        standard_metadata.egress_spec = port_num;
        if(!hdr.mpls.isValid()) {
            if(hdr.ipv4.isValid()) {
                hdr.ipv4.ttl = hdr.ipv4.ttl - 1;
            }
            else if (hdr.ipv6.isValid()) {
                hdr.ipv6.hop_limit = hdr.ipv6.hop_limit - 1;
            }
        }
    }

    action set_vlan_output(vlan_id_t new_vlan_id, port_num_t port_num){
        hdr.vlan_tag.vlan_id = new_vlan_id;

        // don't remove the vlan from egress since we set the vlan to it.
        fabric_metadata.pop_vlan_at_egress = false;
        output(port_num);
    }

    action rewrite_smac(mac_addr_t smac) {
        hdr.ethernet.src_addr = smac;
    }

    action rewrite_dmac(mac_addr_t dmac) {
        hdr.ethernet.dst_addr = dmac;
    }

    action l3_routing(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac) {
        rewrite_smac(smac);
        rewrite_dmac(dmac);
        output(port_num);
    }

    action set_mcast_group(group_id_t gid, mac_addr_t smac) {
        standard_metadata.mcast_grp = gid;
        rewrite_smac(smac);
    }

    table next_id_mapping {
        key = {
            fabric_metadata.next_id: exact;
        }

        actions = {
            set_next_type;
        }
        counters = next_id_mapping_counter;
    }

    table simple {
        key = {
            fabric_metadata.next_id: exact;
        }

        actions = {
            output;
            set_vlan_output;
            l3_routing;
        }
        counters = simple_counter;
    }

    table hashed {
        key = {
            fabric_metadata.next_id: exact;
            hdr.ipv4.src_addr: selector;
            hdr.ipv4.dst_addr: selector;
            hdr.ipv4.protocol: selector;
            hdr.ipv6.src_addr: selector;
            hdr.ipv6.dst_addr: selector;
            hdr.ipv6.next_hdr: selector;
            fabric_metadata.l4_src_port: selector;
            fabric_metadata.l4_dst_port: selector;
        }

        actions = {
            l3_routing;
        }

        implementation = ecmp_selector;
        counters = hashed_counter;
    }

    /*
     * Work in progress
     */
    table broadcast {
        key = {
            fabric_metadata.next_id: exact;
        }
        actions = {
            set_mcast_group;
        }
        counters = broadcast_counter;
    }

    apply {
        next_id_mapping.apply();
        if (fabric_metadata.next_type == NEXT_TYPE_SIMPLE) simple.apply();
        else if (fabric_metadata.next_type == NEXT_TYPE_HASHED) hashed.apply();
        else if (fabric_metadata.next_type == NEXT_TYPE_BROADCAST) broadcast.apply();
        // next_type == PUNT, leave it to packet-io egress
    }
}

control EgressNextControl (
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fabric_metadata,
    inout standard_metadata_t standard_metadata){

    apply {
        // pop internal vlan if the meta is set
        if (fabric_metadata.pop_vlan_at_egress) {
            hdr.vlan_tag.setInvalid();
        }
    }
}
