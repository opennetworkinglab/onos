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

control Filtering (
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fabric_metadata,
    inout standard_metadata_t standard_metadata) {

    /*
     * Ingress Port VLAN Table.
     * Process packets for different interfaces (Port number + VLAN).
     * For example, an untagged packet will be tagged when it entered to an
     * interface with untagged VLAN configuration.
     */
    direct_counter(CounterType.packets_and_bytes) ingress_port_vlan_counter;

    action drop() {
        mark_to_drop();
        ingress_port_vlan_counter.count();
    }

    action set_vlan(vlan_id_t new_vlan_id) {
        hdr.vlan_tag.vlan_id = new_vlan_id;
        ingress_port_vlan_counter.count();
    }

    action push_internal_vlan(vlan_id_t new_vlan_id) {
        // Add internal VLAN header, will be removed before packet emission.
        // cfi and pri values are dummy.
        hdr.vlan_tag.setValid();
        hdr.vlan_tag.cfi = 0;
        hdr.vlan_tag.pri = 0;
        hdr.vlan_tag.ether_type = hdr.ethernet.ether_type;
        hdr.ethernet.ether_type = ETHERTYPE_VLAN;
        hdr.vlan_tag.vlan_id = new_vlan_id;

        // pop internal vlan before packet in
        fabric_metadata.pop_vlan_when_packet_in = _TRUE;
        ingress_port_vlan_counter.count();
    }

    table ingress_port_vlan {
        key = {
            standard_metadata.ingress_port: exact;
            hdr.vlan_tag.isValid(): exact @name("hdr.vlan_tag.is_valid");
            hdr.vlan_tag.vlan_id: ternary;
        }

        actions = {
            push_internal_vlan;
            set_vlan;
            @defaultonly nop;
            drop;
        }

        const default_action = nop();
        counters = ingress_port_vlan_counter;
    }

    /*
     * Forwarding Classifier.
     * Setup Forwarding Type metadata for Forwarding control block.
     * There are six types of tables in Forwarding control block:
     * - Bridging: default forwarding type
     * - MPLS: destination mac address is the router mac and ethernet type is
     *         MPLS(0x8847)
     * - IP Multicast: destination mac address is multicast address and ethernet
     *                 type is IP(0x0800 or 0x86dd)
     * - IP Unicast: destination mac address is router mac and ethernet type is
     *               IP(0x0800 or 0x86dd)
     */
    direct_counter(CounterType.packets_and_bytes) fwd_classifier_counter;

    action set_forwarding_type(fwd_type_t fwd_type) {
        fabric_metadata.fwd_type = fwd_type;
        fwd_classifier_counter.count();
    }

    table fwd_classifier {
        key = {
            standard_metadata.ingress_port: exact;
            hdr.ethernet.dst_addr: exact;
            fabric_metadata.original_ether_type: exact;
        }

        actions = {
            set_forwarding_type;
        }

        const default_action = set_forwarding_type(FWD_BRIDGING);
        counters = fwd_classifier_counter;
    }

    apply {
        ingress_port_vlan.apply();
        fwd_classifier.apply();
    }
}
