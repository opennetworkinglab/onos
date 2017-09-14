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

#ifndef DEFINES
#define DEFINES

#define MAX_PORTS 255

#define ETH_TYPE_IPV4 16w0x800
#define IP_TYPE_TCP 8w6
#define IP_TYPE_UDP 8w17

typedef bit<9> port_t;
typedef bit<8> ecmp_group_id_t;

const port_t CPU_PORT = 255;
const port_t DROP_PORT = 511;

#endif
