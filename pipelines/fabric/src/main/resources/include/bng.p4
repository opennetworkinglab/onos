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

 /*
  * BNG processor implementation. Provides upstream and downstream termination
  * based on double VLAN tags (s_tag, c_tag) and PPPoE.
  *
  * This implementation is based on the P4 Service Edge (p4se) contribution from
  * Deutsche Telekom:
  * https://github.com/opencord/p4se
  */

#ifndef __BNG__
#define __BNG__

#define BNG_MAX_SUBSC 8192
#define BNG_MAX_NET_PER_SUBSC 4
#define BNG_MAX_SUBSC_NET BNG_MAX_NET_PER_SUBSC * BNG_MAX_SUBSC

#define BNG_SUBSC_IPV6_NET_PREFIX_LEN 64

control bng_ingress_upstream(
        inout parsed_headers_t hdr,
        inout fabric_metadata_t fmeta,
        inout standard_metadata_t smeta) {

    counter(BNG_MAX_SUBSC, CounterType.packets) c_terminated;
    counter(BNG_MAX_SUBSC, CounterType.packets) c_dropped;
    counter(BNG_MAX_SUBSC, CounterType.packets) c_control;

    vlan_id_t s_tag = hdr.vlan_tag.vlan_id;
    vlan_id_t c_tag = hdr.inner_vlan_tag.vlan_id;

    _BOOL drop = _FALSE;
    // TABLE: t_line_map
    // Maps double VLAN tags to line ID. Line IDs are used to uniquelly identify
    // a subscriber.

    action set_line(bit<32> line_id) {
        fmeta.bng.line_id = line_id;
    }

    table t_line_map {
        actions = {
            @defaultonly nop;
            set_line;
        }
        key = {
            s_tag: exact @name("s_tag");
            c_tag: exact @name("c_tag");
        }
        size = BNG_MAX_SUBSC;
        const default_action = nop;
    }

    // TABLE: t_pppoe_cp
    // Punt to CPU for PPPeE control packets.

    action punt_to_cpu() {
        smeta.egress_spec = CPU_PORT;
        fmeta.skip_forwarding = _TRUE;
        fmeta.skip_next = _TRUE;
        c_control.count(fmeta.bng.line_id);
    }

    table t_pppoe_cp {
        key = {
            hdr.pppoe.code     : exact   @name("pppoe_code");
            hdr.pppoe.protocol : ternary @name("pppoe_protocol");
        }
        actions = {
            punt_to_cpu;
            @defaultonly nop;
        }
        size = 16;
        const default_action = nop;
    }

    // TABLE: PPPoE termination for IPv4
    // Check subscriber IPv4 source address, line_id, and pppoe_session_id
    // (antispoofing), if line is enabled, pop PPPoE and double VLANs.

    @hidden
    action term_enabled(bit<16> eth_type) {
        hdr.ethernet.eth_type = eth_type;
        fmeta.eth_type = eth_type;
        hdr.pppoe.setInvalid();
        hdr.vlan_tag.setInvalid();
        hdr.inner_vlan_tag.setInvalid();
        c_terminated.count(fmeta.bng.line_id);
    }

    action term_disabled() {
        fmeta.bng.type = BNG_TYPE_INVALID;
        fmeta.skip_forwarding = _TRUE;
        fmeta.skip_next = _TRUE;
        mark_to_drop(smeta);
        drop = _TRUE;
    }

    action term_enabled_v4() {
        term_enabled(ETHERTYPE_IPV4);
    }

    table t_pppoe_term_v4 {
        key = {
            fmeta.bng.line_id    : exact @name("line_id");
            hdr.ipv4.src_addr    : exact @name("ipv4_src");
            hdr.pppoe.session_id : exact @name("pppoe_session_id");
        }
        actions = {
            term_enabled_v4;
            @defaultonly term_disabled;
        }
        size = BNG_MAX_SUBSC_NET;
        const default_action = term_disabled;
    }

#ifdef WITH_IPV6
    action term_enabled_v6() {
        term_enabled(ETHERTYPE_IPV6);
    }

    table t_pppoe_term_v6 {
        key = {
            fmeta.bng.line_id         : exact @name("line_id");
            hdr.ipv6.src_addr[127:64] : exact @name("ipv6_src_net_id");
            hdr.pppoe.session_id      : exact @name("pppoe_session_id");
        }
        actions = {
            term_enabled_v6;
            @defaultonly term_disabled;
        }
        size = BNG_MAX_SUBSC_NET;
        const default_action = term_disabled;
    }
#endif // WITH_IPV6

    apply {
        // If table miss, line_id will be 0 (default metadata value).
        t_line_map.apply();

        if (t_pppoe_cp.apply().hit) {
            return;
        }

        if (hdr.ipv4.isValid()) {
            t_pppoe_term_v4.apply();
            if (drop == _TRUE) {
                c_dropped.count(fmeta.bng.line_id);
            }
        }
#ifdef WITH_IPV6
        else if (hdr.ipv6.isValid()) {
            t_pppoe_term_v6.apply();
            if (drop == _TRUE) {
                c_dropped.count(fmeta.bng.line_id);
             }
        }
#endif // WITH_IPV6
    }
}

control bng_ingress_downstream(
        inout parsed_headers_t hdr,
        inout fabric_metadata_t fmeta,
        inout standard_metadata_t smeta) {

    counter(BNG_MAX_SUBSC, CounterType.packets_and_bytes) c_line_rx;

    meter(BNG_MAX_SUBSC, MeterType.bytes) m_besteff;
    meter(BNG_MAX_SUBSC, MeterType.bytes) m_prio;

    // Downstream line map tables.
    // Map IP dest address to line ID and next ID. Setting a next ID here
    // allows to skip the fabric.p4 forwarding stage later.
    _BOOL prio = _FALSE;

    @hidden
    action set_line(bit<32> line_id) {
        fmeta.bng.type = BNG_TYPE_DOWNSTREAM;
        fmeta.bng.line_id = line_id;
        c_line_rx.count(line_id);
    }

    action set_line_next(bit<32> line_id, next_id_t next_id) {
        set_line(line_id);
        fmeta.next_id = next_id;
        fmeta.skip_forwarding = _TRUE;
    }

    action set_line_drop(bit<32> line_id) {
        set_line(line_id);
        fmeta.skip_forwarding = _TRUE;
        fmeta.skip_next = _TRUE;
        mark_to_drop(smeta);
    }

    table t_line_map_v4 {
        key = {
            hdr.ipv4.dst_addr: exact @name("ipv4_dst");
        }
        actions = {
            @defaultonly nop;
            set_line_next;
            set_line_drop;
        }
        size = BNG_MAX_SUBSC_NET;
        const default_action = nop;
    }

#ifdef WITH_IPV6
    table t_line_map_v6 {
        key = {
            hdr.ipv6.dst_addr[127:64]: exact @name("ipv6_dst_net_id");
        }
        actions = {
            @defaultonly nop;
            set_line_next;
            set_line_drop;
        }
        size = BNG_MAX_SUBSC_NET;
        const default_action = nop;
    }
#endif // WITH_IPV6

    // Downstream QoS tables.
    // Provide coarse metering before prioritazion in the OLT. By default
    // everything is tagged and metered as best-effort traffic.

    action qos_prio() {
        prio = _TRUE;
    }

    action qos_besteff() {
        // no-op
    }

    table t_qos_v4 {
        key = {
            fmeta.bng.line_id : ternary @name("line_id");
            hdr.ipv4.src_addr : lpm     @name("ipv4_src");
            hdr.ipv4.dscp     : ternary @name("ipv4_dscp");
            hdr.ipv4.ecn      : ternary @name("ipv4_ecn");
        }
        actions = {
            qos_prio;
            qos_besteff;
        }
        size = 256;
        const default_action = qos_besteff;
    }

#ifdef WITH_IPV6
    table t_qos_v6 {
        key = {
            fmeta.bng.line_id      : ternary @name("line_id");
            hdr.ipv6.src_addr      : lpm     @name("ipv6_src");
            hdr.ipv6.traffic_class : ternary @name("ipv6_traffic_class");
        }
        actions = {
            qos_prio;
            qos_besteff;
        }
        size = 256;
        const default_action = qos_besteff;
    }
#endif // WITH_IPV6

    apply {
        // IPv4
        if (hdr.ipv4.isValid()) {
            if (t_line_map_v4.apply().hit) {
                // Apply QoS only to subscriber traffic. This makes sense only
                // if the downstream ports are used to receive IP traffic NOT
                // destined to subscribers, e.g. to services in the compute
                // nodes.
                t_qos_v4.apply();
                if (prio == _TRUE) {
                    m_prio.execute_meter((bit<32>)fmeta.bng.line_id,
                                          fmeta.bng.ds_meter_result);
                } else {
                    m_besteff.execute_meter((bit<32>)fmeta.bng.line_id,
                                            fmeta.bng.ds_meter_result);
                }
            }
        }
#ifdef WITH_IPV6
        // IPv6
        else if (hdr.ipv6.isValid()) {
            if (t_line_map_v6.apply().hit) {
                t_qos_v6.apply();
                if (prio == _TRUE) {
                    m_prio.execute_meter((bit<32>)fmeta.bng.line_id,
                                          fmeta.bng.ds_meter_result);
                } else {
                    m_besteff.execute_meter((bit<32>)fmeta.bng.line_id,
                                            fmeta.bng.ds_meter_result);
                }
            }
        }
#endif // WITH_IPV6
    }
}

control bng_egress_downstream(
        inout parsed_headers_t hdr,
        inout fabric_metadata_t fmeta,
        inout standard_metadata_t smeta) {

    counter(BNG_MAX_SUBSC, CounterType.packets_and_bytes) c_line_tx;

    @hidden
    action encap(vlan_id_t c_tag, bit<16> pppoe_session_id) {
        // s_tag (outer VLAN) should be already set via the next_vlan table.
        // Here we add c_tag (inner VLAN) and PPPoE.
        hdr.vlan_tag.eth_type = ETHERTYPE_VLAN;
        hdr.inner_vlan_tag.setValid();
        hdr.inner_vlan_tag.vlan_id = c_tag;
        hdr.inner_vlan_tag.eth_type = ETHERTYPE_PPPOES;
        hdr.pppoe.setValid();
        hdr.pppoe.version = 4w1;
        hdr.pppoe.type_id = 4w1;
        hdr.pppoe.code = 8w0; // 0 means session stage.
        hdr.pppoe.session_id = pppoe_session_id;
        c_line_tx.count(fmeta.bng.line_id);
    }

    action encap_v4(vlan_id_t c_tag, bit<16> pppoe_session_id) {
        encap(c_tag, pppoe_session_id);
        hdr.pppoe.length = hdr.ipv4.total_len + 16w2;
        hdr.pppoe.protocol = PPPOE_PROTOCOL_IP4;
    }

#ifdef WITH_IPV6
    action encap_v6(vlan_id_t c_tag, bit<16> pppoe_session_id) {
        encap(c_tag, pppoe_session_id);
        hdr.pppoe.length = hdr.ipv6.payload_len + 16w42;
        hdr.pppoe.protocol = PPPOE_PROTOCOL_IP6;
    }
#endif // WITH_IPV6

    table t_session_encap {
        key = {
            fmeta.bng.line_id : exact @name("line_id");
        }
        actions = {
            @defaultonly nop;
            encap_v4;
#ifdef WITH_IPV6
            encap_v6;
#endif // WITH_IPV6
        }
        size = BNG_MAX_SUBSC;
        const default_action = nop();
    }

    apply {
        t_session_encap.apply();
    }
}

control bng_ingress(
        inout parsed_headers_t hdr,
        inout fabric_metadata_t fmeta,
        inout standard_metadata_t smeta) {

    bng_ingress_upstream() upstream;
    bng_ingress_downstream() downstream;

    apply {
        if (hdr.pppoe.isValid()) {
            fmeta.bng.type = BNG_TYPE_UPSTREAM;
            upstream.apply(hdr, fmeta, smeta);
        }
        else {
            // We are not sure the pkt is a BNG downstream one, first we need to
            // verify the IP dst matches the IP addr of a subscriber...
            downstream.apply(hdr, fmeta, smeta);
        }
    }
}

control bng_egress(
        inout parsed_headers_t hdr,
        inout fabric_metadata_t fmeta,
        inout standard_metadata_t smeta) {

    bng_egress_downstream() downstream;

    apply {
        if (fmeta.bng.type == BNG_TYPE_DOWNSTREAM) {
            downstream.apply(hdr, fmeta, smeta);
        }
    }
}

#endif
