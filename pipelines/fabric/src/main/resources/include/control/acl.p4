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

#include "../define.p4"
#include "../header.p4"

control Acl (inout parsed_headers_t hdr,
             inout fabric_metadata_t fabric_metadata,
             inout standard_metadata_t standard_metadata) {

    /*
     * ACL Table.
     */
    direct_counter(CounterType.packets_and_bytes) acl_counter;

    action set_next_id_acl(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
        acl_counter.count();
    }

    // Send immendiatelly to CPU - skip the rest of ingress.
    action punt_to_cpu() {
        standard_metadata.egress_spec = CPU_PORT;
        fabric_metadata.skip_next = _TRUE;
        acl_counter.count();
    }

    action clone_to_cpu() {
        // FIXME: works only if pkt will be replicated via PRE multicast group.
        fabric_metadata.clone_to_cpu = _TRUE;
        acl_counter.count();
    }

    action drop() {
        mark_to_drop(standard_metadata);
        fabric_metadata.skip_next = _TRUE;
        acl_counter.count();
    }

    action nop_acl() {
        acl_counter.count();
    }

    table acl {
        key = {
            standard_metadata.ingress_port: ternary @name("ig_port"); // 9
            fabric_metadata.ip_proto: ternary @name("ip_proto"); // 8
            fabric_metadata.l4_sport: ternary @name("l4_sport"); // 16
            fabric_metadata.l4_dport: ternary @name("l4_dport"); // 16
            hdr.ethernet.dst_addr: ternary @name("eth_src"); // 48
            hdr.ethernet.src_addr: ternary @name("eth_dst"); // 48
            hdr.vlan_tag.vlan_id: ternary @name("vlan_id"); // 12
            fabric_metadata.eth_type: ternary @name("eth_type"); //16
            hdr.ipv4.src_addr: ternary @name("ipv4_src"); // 32
            hdr.ipv4.dst_addr: ternary @name("ipv4_dst"); // 32
            hdr.icmp.icmp_type: ternary @name("icmp_type"); // 8
            hdr.icmp.icmp_code: ternary @name("icmp_code"); // 8
        }

        actions = {
            set_next_id_acl;
            punt_to_cpu;
            clone_to_cpu;
            drop;
            nop_acl;
        }

        const default_action = nop_acl();
        size = ACL_TABLE_SIZE;
        counters = acl_counter;
    }

    apply {
        acl.apply();
    }
}
