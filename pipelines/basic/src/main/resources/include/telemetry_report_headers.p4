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

/* -*- P4_16 -*- */
#ifndef __TELEMETRY_REPORT_HEADERS__
#define __TELEMETRY_REPORT_HEADERS__

const bit<3> NPROTO_ETHERNET = 0;
const bit<3> NPROTO_TELEMETRY_DROP_HEADER = 1;
const bit<3> NPROTO_TELEMETRY_SWITCH_LOCAL_HEADER = 2;


// Report Telemetry Headers
header report_fixed_header_t {
    bit<4>  ver;
    bit<4>  len;
    bit<3>  nproto;
    bit<6>  rep_md_bits;
    bit<1>  d;
    bit<1>  q;
    bit<1>  f;
    bit<6>  rsvd;
    bit<6>  hw_id;
    bit<32> sw_id;
    bit<32> seq_no;
    bit<32> ingress_tstamp;
}
const bit<8> REPORT_FIXED_HEADER_LEN = 16;

// Telemetry drop report header
header drop_report_header_t {
    bit<32> switch_id;
    bit<16> ingress_port_id;
    bit<16> egress_port_id;
    bit<8>  queue_id;
    bit<8>  drop_reason;
    bit<16> pad;
}
const bit<8> DROP_REPORT_HEADER_LEN = 12;

// Switch Local Report Header
header local_report_header_t {
    bit<32> switch_id;
    bit<16> ingress_port_id;
    bit<16> egress_port_id;
    bit<8>  queue_id;
    bit<24> queue_occupancy;
    bit<32> egress_tstamp;
}
const bit<8> LOCAL_REPORT_HEADER_LEN = 16;

header_union local_report_t {
    drop_report_header_t drop_report_header;
    local_report_header_t local_report_header;
}

#endif
