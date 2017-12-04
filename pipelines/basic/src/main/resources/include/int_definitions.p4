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
#ifndef __INT_DEFINE__
#define __INT_DEFINE__

#include "defines.p4"

/* indicate INT at LSB of DSCP */
const bit<6> INT_DSCP = 0x1;

typedef bit<48> timestamp_t;
typedef bit<32> switch_id_t;

const bit<8> INT_HEADER_LEN_WORD = 4;

const bit<8> CPU_MIRROR_SESSION_ID = 250;

#endif