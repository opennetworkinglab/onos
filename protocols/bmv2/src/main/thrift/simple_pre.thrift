namespace java org.p4.bmv2.thrift
/* Copyright 2013-present Barefoot Networks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Antonin Bas (antonin@barefootnetworks.com)
 *
 */

namespace cpp bm_runtime.simple_pre
namespace py bm_runtime.simple_pre

typedef i32 BmMcMgrp
typedef i32 BmMcRid
typedef i32 BmMcMgrpHandle
typedef i32 BmMcL1Handle
typedef string BmMcPortMap // string of 0s and 1s

enum McOperationErrorCode {
  TABLE_FULL = 1,
  INVALID_HANDLE = 2,
  INVALID_MGID = 3,
  INVALID_L1_HANDLE = 4,
  INVALID_L2_HANLDE = 5,
  ERROR = 6
}

exception InvalidMcOperation {
  1:McOperationErrorCode code
}

service SimplePre {

  BmMcMgrpHandle bm_mc_mgrp_create(
    1:i32 cxt_id,
    2:BmMcMgrp mgrp
  ) throws (1:InvalidMcOperation ouch),

  void bm_mc_mgrp_destroy(
    1:i32 cxt_id,
    2:BmMcMgrpHandle mgrp_handle
  ) throws (1:InvalidMcOperation ouch),

  BmMcL1Handle bm_mc_node_create(
    1:i32 cxt_id,
    2:BmMcRid rid
    3:BmMcPortMap port_map
  ) throws (1:InvalidMcOperation ouch),

  void bm_mc_node_associate(
    1:i32 cxt_id,
    2:BmMcMgrpHandle mgrp_handle,
    3:BmMcL1Handle l1_handle
  ) throws (1:InvalidMcOperation ouch),

  void bm_mc_node_dissociate(
    1:i32 cxt_id,
    2:BmMcMgrpHandle mgrp_handle,
    3:BmMcL1Handle l1_handle
  ) throws (1:InvalidMcOperation ouch),

  void bm_mc_node_destroy(
    1:i32 cxt_id,
    2:BmMcL1Handle l1_handle
  ) throws (1:InvalidMcOperation ouch),

  void bm_mc_node_update(
    1:i32 cxt_id,
    2:BmMcL1Handle l1_handle,
    3:BmMcPortMap port_map
  ) throws (1:InvalidMcOperation ouch),
}
