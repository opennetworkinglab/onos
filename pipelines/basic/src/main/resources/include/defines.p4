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

#ifndef __DEFINES__
#define __DEFINES__

#define ETH_TYPE_IPV4 0x0800
#define IP_PROTO_TCP 8w6
#define IP_PROTO_UDP 8w17
#define IP_VERSION_4 4w4
#define IPV4_IHL_MIN 4w5
#define MAX_PORTS 511

#ifndef _BOOL
#define _BOOL bool
#endif
#ifndef _TRUE
#define _TRUE true
#endif
#ifndef _FALSE
#define _FALSE false
#endif

typedef bit<48> mac_t;
typedef bit<32> ip_address_t;
typedef bit<16> l4_port_t;
typedef bit<9>  port_t;
typedef bit<16> next_hop_id_t;

const port_t CPU_PORT = 255;

typedef bit<8> MeterColor;
const MeterColor MeterColor_GREEN = 8w0;
const MeterColor MeterColor_YELLOW = 8w1;
const MeterColor MeterColor_RED = 8w2;

#endif
