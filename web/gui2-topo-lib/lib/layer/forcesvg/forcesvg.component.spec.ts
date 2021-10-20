/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {ForceSvgComponent} from './forcesvg.component';
import {
    FnService, IconService,
    LionService,
    LogService, SvgUtilService,
    UrlFnService,
    WebSocketService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {DraggableDirective} from './draggable/draggable.directive';
import {ActivatedRoute, Params} from '@angular/router';
import {of} from 'rxjs';
import {DeviceNodeSvgComponent} from './visuals/devicenodesvg/devicenodesvg.component';
import {SubRegionNodeSvgComponent} from './visuals/subregionnodesvg/subregionnodesvg.component';
import {HostNodeSvgComponent} from './visuals/hostnodesvg/hostnodesvg.component';
import {LinkSvgComponent} from './visuals/linksvg/linksvg.component';
import {Device, Host, Link, LinkType, LinkHighlight, Region, Node} from './models';
import {ChangeDetectorRef, SimpleChange} from '@angular/core';
import {TopologyService} from '../../topology.service';
import {BadgeSvgComponent} from './visuals/badgesvg/badgesvg.component';

export const test_module_topo2CurrentRegion = `{
  "event": "topo2CurrentRegion",
  "payload": {
    "id": "(root)",
    "subregions": [],
    "links": [
      {
        "id": "00:AA:00:00:00:03/None~of:0000000000000205/6",
        "epA": "00:AA:00:00:00:03/None",
        "epB": "of:0000000000000205",
        "type": "UiEdgeLink",
        "portB": "6",
        "rollup": [
          {
            "id": "00:AA:00:00:00:03/None~of:0000000000000205/6",
            "epA": "00:AA:00:00:00:03/None",
            "epB": "of:0000000000000205",
            "type": "UiEdgeLink",
            "portB": "6"
          }
        ]
      },
      {
        "id": "of:0000000000000205/3~of:0000000000000227/5",
        "epA": "of:0000000000000205/3",
        "epB": "of:0000000000000227/5",
        "type": "UiDeviceLink",
        "portA": "3",
        "portB": "5",
        "rollup": [
          {
            "id": "of:0000000000000205/3~of:0000000000000227/5",
            "epA": "of:0000000000000205/3",
            "epB": "of:0000000000000227/5",
            "type": "UiDeviceLink",
            "portA": "3",
            "portB": "5"
          }
        ]
      },
      {
        "id": "of:0000000000000206/2~of:0000000000000226/8",
        "epA": "of:0000000000000206/2",
        "epB": "of:0000000000000226/8",
        "type": "UiDeviceLink",
        "portA": "2",
        "portB": "8",
        "rollup": [
          {
            "id": "of:0000000000000206/2~of:0000000000000226/8",
            "epA": "of:0000000000000206/2",
            "epB": "of:0000000000000226/8",
            "type": "UiDeviceLink",
            "portA": "2",
            "portB": "8"
          }
        ]
      },
      {
        "id": "00:BB:00:00:00:05/None~of:0000000000000203/7",
        "epA": "00:BB:00:00:00:05/None",
        "epB": "of:0000000000000203",
        "type": "UiEdgeLink",
        "portB": "7",
        "rollup": [
          {
            "id": "00:BB:00:00:00:05/None~of:0000000000000203/7",
            "epA": "00:BB:00:00:00:05/None",
            "epB": "of:0000000000000203",
            "type": "UiEdgeLink",
            "portB": "7"
          }
        ]
      },
      {
        "id": "00:DD:00:00:00:01/None~of:0000000000000207/3",
        "epA": "00:DD:00:00:00:01/None",
        "epB": "of:0000000000000207",
        "type": "UiEdgeLink",
        "portB": "3",
        "rollup": [
          {
            "id": "00:DD:00:00:00:01/None~of:0000000000000207/3",
            "epA": "00:DD:00:00:00:01/None",
            "epB": "of:0000000000000207",
            "type": "UiEdgeLink",
            "portB": "3"
          }
        ]
      },
      {
        "id": "of:0000000000000203/1~of:0000000000000226/1",
        "epA": "of:0000000000000203/1",
        "epB": "of:0000000000000226/1",
        "type": "UiDeviceLink",
        "portA": "1",
        "portB": "1",
        "rollup": [
          {
            "id": "of:0000000000000203/1~of:0000000000000226/1",
            "epA": "of:0000000000000203/1",
            "epB": "of:0000000000000226/1",
            "type": "UiDeviceLink",
            "portA": "1",
            "portB": "1"
          }
        ]
      },
      {
        "id": "of:0000000000000207/2~of:0000000000000247/1",
        "epA": "of:0000000000000207/2",
        "epB": "of:0000000000000247/1",
        "type": "UiDeviceLink",
        "portA": "2",
        "portB": "1",
        "rollup": [
          {
            "id": "of:0000000000000207/2~of:0000000000000247/1",
            "epA": "of:0000000000000207/2",
            "epB": "of:0000000000000247/1",
            "type": "UiDeviceLink",
            "portA": "2",
            "portB": "1"
          }
        ]
      },
      {
        "id": "00:99:66:00:00:01/None~of:0000000000000205/10",
        "epA": "00:99:66:00:00:01/None",
        "epB": "of:0000000000000205",
        "type": "UiEdgeLink",
        "portB": "10",
        "rollup": [
          {
            "id": "00:99:66:00:00:01/None~of:0000000000000205/10",
            "epA": "00:99:66:00:00:01/None",
            "epB": "of:0000000000000205",
            "type": "UiEdgeLink",
            "portB": "10"
          }
        ]
      },
      {
        "id": "of:0000000000000208/1~of:0000000000000246/2",
        "epA": "of:0000000000000208/1",
        "epB": "of:0000000000000246/2",
        "type": "UiDeviceLink",
        "portA": "1",
        "portB": "2",
        "rollup": [
          {
            "id": "of:0000000000000208/1~of:0000000000000246/2",
            "epA": "of:0000000000000208/1",
            "epB": "of:0000000000000246/2",
            "type": "UiDeviceLink",
            "portA": "1",
            "portB": "2"
          }
        ]
      },
      {
        "id": "of:0000000000000206/1~of:0000000000000226/7",
        "epA": "of:0000000000000206/1",
        "epB": "of:0000000000000226/7",
        "type": "UiDeviceLink",
        "portA": "1",
        "portB": "7",
        "rollup": [
          {
            "id": "of:0000000000000206/1~of:0000000000000226/7",
            "epA": "of:0000000000000206/1",
            "epB": "of:0000000000000226/7",
            "type": "UiDeviceLink",
            "portA": "1",
            "portB": "7"
          }
        ]
      },
      {
        "id": "of:0000000000000226/9~of:0000000000000246/3",
        "epA": "of:0000000000000226/9",
        "epB": "of:0000000000000246/3",
        "type": "UiDeviceLink",
        "portA": "9",
        "portB": "3",
        "rollup": [
          {
            "id": "of:0000000000000226/9~of:0000000000000246/3",
            "epA": "of:0000000000000226/9",
            "epB": "of:0000000000000246/3",
            "type": "UiDeviceLink",
            "portA": "9",
            "portB": "3"
          }
        ]
      },
      {
        "id": "00:AA:00:00:00:04/None~of:0000000000000205/7",
        "epA": "00:AA:00:00:00:04/None",
        "epB": "of:0000000000000205",
        "type": "UiEdgeLink",
        "portB": "7",
        "rollup": [
          {
            "id": "00:AA:00:00:00:04/None~of:0000000000000205/7",
            "epA": "00:AA:00:00:00:04/None",
            "epB": "of:0000000000000205",
            "type": "UiEdgeLink",
            "portB": "7"
          }
        ]
      },
      {
        "id": "00:88:00:00:00:03/110~of:0000000000000205/11",
        "epA": "00:88:00:00:00:03/110",
        "epB": "of:0000000000000205",
        "type": "UiEdgeLink",
        "portB": "11",
        "rollup": [
          {
            "id": "00:88:00:00:00:03/110~of:0000000000000205/11",
            "epA": "00:88:00:00:00:03/110",
            "epB": "of:0000000000000205",
            "type": "UiEdgeLink",
            "portB": "11"
          }
        ]
      },
      {
        "id": "of:0000000000000204/1~of:0000000000000226/3",
        "epA": "of:0000000000000204/1",
        "epB": "of:0000000000000226/3",
        "type": "UiDeviceLink",
        "portA": "1",
        "portB": "3",
        "rollup": [
          {
            "id": "of:0000000000000204/1~of:0000000000000226/3",
            "epA": "of:0000000000000204/1",
            "epB": "of:0000000000000226/3",
            "type": "UiDeviceLink",
            "portA": "1",
            "portB": "3"
          }
        ]
      },
      {
        "id": "of:0000000000000203/2~of:0000000000000226/2",
        "epA": "of:0000000000000203/2",
        "epB": "of:0000000000000226/2",
        "type": "UiDeviceLink",
        "portA": "2",
        "portB": "2",
        "rollup": [
          {
            "id": "of:0000000000000203/2~of:0000000000000226/2",
            "epA": "of:0000000000000203/2",
            "epB": "of:0000000000000226/2",
            "type": "UiDeviceLink",
            "portA": "2",
            "portB": "2"
          }
        ]
      },
      {
        "id": "00:88:00:00:00:01/None~of:0000000000000205/12",
        "epA": "00:88:00:00:00:01/None",
        "epB": "of:0000000000000205",
        "type": "UiEdgeLink",
        "portB": "12",
        "rollup": [
          {
            "id": "00:88:00:00:00:01/None~of:0000000000000205/12",
            "epA": "00:88:00:00:00:01/None",
            "epB": "of:0000000000000205",
            "type": "UiEdgeLink",
            "portB": "12"
          }
        ]
      },
      {
        "id": "00:88:00:00:00:04/160~of:0000000000000206/6",
        "epA": "00:88:00:00:00:04/160",
        "epB": "of:0000000000000206",
        "type": "UiEdgeLink",
        "portB": "6",
        "rollup": [
          {
            "id": "00:88:00:00:00:04/160~of:0000000000000206/6",
            "epA": "00:88:00:00:00:04/160",
            "epB": "of:0000000000000206",
            "type": "UiEdgeLink",
            "portB": "6"
          }
        ]
      },
      {
        "id": "00:DD:00:00:00:02/None~of:0000000000000208/3",
        "epA": "00:DD:00:00:00:02/None",
        "epB": "of:0000000000000208",
        "type": "UiEdgeLink",
        "portB": "3",
        "rollup": [
          {
            "id": "00:DD:00:00:00:02/None~of:0000000000000208/3",
            "epA": "00:DD:00:00:00:02/None",
            "epB": "of:0000000000000208",
            "type": "UiEdgeLink",
            "portB": "3"
          }
        ]
      },
      {
        "id": "of:0000000000000203/3~of:0000000000000227/1",
        "epA": "of:0000000000000203/3",
        "epB": "of:0000000000000227/1",
        "type": "UiDeviceLink",
        "portA": "3",
        "portB": "1",
        "rollup": [
          {
            "id": "of:0000000000000203/3~of:0000000000000227/1",
            "epA": "of:0000000000000203/3",
            "epB": "of:0000000000000227/1",
            "type": "UiDeviceLink",
            "portA": "3",
            "portB": "1"
          }
        ]
      },
      {
        "id": "of:0000000000000208/2~of:0000000000000247/2",
        "epA": "of:0000000000000208/2",
        "epB": "of:0000000000000247/2",
        "type": "UiDeviceLink",
        "portA": "2",
        "portB": "2",
        "rollup": [
          {
            "id": "of:0000000000000208/2~of:0000000000000247/2",
            "epA": "of:0000000000000208/2",
            "epB": "of:0000000000000247/2",
            "type": "UiDeviceLink",
            "portA": "2",
            "portB": "2"
          }
        ]
      },
      {
        "id": "of:0000000000000205/1~of:0000000000000226/5",
        "epA": "of:0000000000000205/1",
        "epB": "of:0000000000000226/5",
        "type": "UiDeviceLink",
        "portA": "1",
        "portB": "5",
        "rollup": [
          {
            "id": "of:0000000000000205/1~of:0000000000000226/5",
            "epA": "of:0000000000000205/1",
            "epB": "of:0000000000000226/5",
            "type": "UiDeviceLink",
            "portA": "1",
            "portB": "5"
          }
        ]
      },
      {
        "id": "of:0000000000000204/2~of:0000000000000226/4",
        "epA": "of:0000000000000204/2",
        "epB": "of:0000000000000226/4",
        "type": "UiDeviceLink",
        "portA": "2",
        "portB": "4",
        "rollup": [
          {
            "id": "of:0000000000000204/2~of:0000000000000226/4",
            "epA": "of:0000000000000204/2",
            "epB": "of:0000000000000226/4",
            "type": "UiDeviceLink",
            "portA": "2",
            "portB": "4"
          }
        ]
      },
      {
        "id": "00:AA:00:00:00:01/None~of:0000000000000204/6",
        "epA": "00:AA:00:00:00:01/None",
        "epB": "of:0000000000000204",
        "type": "UiEdgeLink",
        "portB": "6",
        "rollup": [
          {
            "id": "00:AA:00:00:00:01/None~of:0000000000000204/6",
            "epA": "00:AA:00:00:00:01/None",
            "epB": "of:0000000000000204",
            "type": "UiEdgeLink",
            "portB": "6"
          }
        ]
      },
      {
        "id": "00:BB:00:00:00:03/None~of:0000000000000205/8",
        "epA": "00:BB:00:00:00:03/None",
        "epB": "of:0000000000000205",
        "type": "UiEdgeLink",
        "portB": "8",
        "rollup": [
          {
            "id": "00:BB:00:00:00:03/None~of:0000000000000205/8",
            "epA": "00:BB:00:00:00:03/None",
            "epB": "of:0000000000000205",
            "type": "UiEdgeLink",
            "portB": "8"
          }
        ]
      },
      {
        "id": "of:0000000000000206/4~of:0000000000000227/8",
        "epA": "of:0000000000000206/4",
        "epB": "of:0000000000000227/8",
        "type": "UiDeviceLink",
        "portA": "4",
        "portB": "8",
        "rollup": [
          {
            "id": "of:0000000000000206/4~of:0000000000000227/8",
            "epA": "of:0000000000000206/4",
            "epB": "of:0000000000000227/8",
            "type": "UiDeviceLink",
            "portA": "4",
            "portB": "8"
          }
        ]
      },
      {
        "id": "00:AA:00:00:00:05/None~of:0000000000000203/6",
        "epA": "00:AA:00:00:00:05/None",
        "epB": "of:0000000000000203",
        "type": "UiEdgeLink",
        "portB": "6",
        "rollup": [
          {
            "id": "00:AA:00:00:00:05/None~of:0000000000000203/6",
            "epA": "00:AA:00:00:00:05/None",
            "epB": "of:0000000000000203",
            "type": "UiEdgeLink",
            "portB": "6"
          }
        ]
      },
      {
        "id": "of:0000000000000205/5~of:0000000000000206/5",
        "epA": "of:0000000000000205/5",
        "epB": "of:0000000000000206/5",
        "type": "UiDeviceLink",
        "portA": "5",
        "portB": "5",
        "rollup": [
          {
            "id": "of:0000000000000205/5~of:0000000000000206/5",
            "epA": "of:0000000000000205/5",
            "epB": "of:0000000000000206/5",
            "type": "UiDeviceLink",
            "portA": "5",
            "portB": "5"
          }
        ]
      },
      {
        "id": "00:BB:00:00:00:02/None~of:0000000000000204/9",
        "epA": "00:BB:00:00:00:02/None",
        "epB": "of:0000000000000204",
        "type": "UiEdgeLink",
        "portB": "9",
        "rollup": [
          {
            "id": "00:BB:00:00:00:02/None~of:0000000000000204/9",
            "epA": "00:BB:00:00:00:02/None",
            "epB": "of:0000000000000204",
            "type": "UiEdgeLink",
            "portB": "9"
          }
        ]
      },
      {
        "id": "of:0000000000000204/3~of:0000000000000227/3",
        "epA": "of:0000000000000204/3",
        "epB": "of:0000000000000227/3",
        "type": "UiDeviceLink",
        "portA": "3",
        "portB": "3",
        "rollup": [
          {
            "id": "of:0000000000000204/3~of:0000000000000227/3",
            "epA": "of:0000000000000204/3",
            "epB": "of:0000000000000227/3",
            "type": "UiDeviceLink",
            "portA": "3",
            "portB": "3"
          }
        ]
      },
      {
        "id": "00:EE:00:00:00:01/None~of:0000000000000207/4",
        "epA": "00:EE:00:00:00:01/None",
        "epB": "of:0000000000000207",
        "type": "UiEdgeLink",
        "portB": "4",
        "rollup": [
          {
            "id": "00:EE:00:00:00:01/None~of:0000000000000207/4",
            "epA": "00:EE:00:00:00:01/None",
            "epB": "of:0000000000000207",
            "type": "UiEdgeLink",
            "portB": "4"
          }
        ]
      },
      {
        "id": "of:0000000000000203/4~of:0000000000000227/2",
        "epA": "of:0000000000000203/4",
        "epB": "of:0000000000000227/2",
        "type": "UiDeviceLink",
        "portA": "4",
        "portB": "2",
        "rollup": [
          {
            "id": "of:0000000000000203/4~of:0000000000000227/2",
            "epA": "of:0000000000000203/4",
            "epB": "of:0000000000000227/2",
            "type": "UiDeviceLink",
            "portA": "4",
            "portB": "2"
          }
        ]
      },
      {
        "id": "of:0000000000000205/2~of:0000000000000226/6",
        "epA": "of:0000000000000205/2",
        "epB": "of:0000000000000226/6",
        "type": "UiDeviceLink",
        "portA": "2",
        "portB": "6",
        "rollup": [
          {
            "id": "of:0000000000000205/2~of:0000000000000226/6",
            "epA": "of:0000000000000205/2",
            "epB": "of:0000000000000226/6",
            "type": "UiDeviceLink",
            "portA": "2",
            "portB": "6"
          }
        ]
      },
      {
        "id": "00:99:00:00:00:01/None~of:0000000000000205/10",
        "epA": "00:99:00:00:00:01/None",
        "epB": "of:0000000000000205",
        "type": "UiEdgeLink",
        "portB": "10",
        "rollup": [
          {
            "id": "00:99:00:00:00:01/None~of:0000000000000205/10",
            "epA": "00:99:00:00:00:01/None",
            "epB": "of:0000000000000205",
            "type": "UiEdgeLink",
            "portB": "10"
          }
        ]
      },
      {
        "id": "of:0000000000000205/4~of:0000000000000227/6",
        "epA": "of:0000000000000205/4",
        "epB": "of:0000000000000227/6",
        "type": "UiDeviceLink",
        "portA": "4",
        "portB": "6",
        "rollup": [
          {
            "id": "of:0000000000000205/4~of:0000000000000227/6",
            "epA": "of:0000000000000205/4",
            "epB": "of:0000000000000227/6",
            "type": "UiDeviceLink",
            "portA": "4",
            "portB": "6"
          }
        ]
      },
      {
        "id": "of:0000000000000206/3~of:0000000000000227/7",
        "epA": "of:0000000000000206/3",
        "epB": "of:0000000000000227/7",
        "type": "UiDeviceLink",
        "portA": "3",
        "portB": "7",
        "rollup": [
          {
            "id": "of:0000000000000206/3~of:0000000000000227/7",
            "epA": "of:0000000000000206/3",
            "epB": "of:0000000000000227/7",
            "type": "UiDeviceLink",
            "portA": "3",
            "portB": "7"
          }
        ]
      },
      {
        "id": "00:BB:00:00:00:04/None~of:0000000000000205/9",
        "epA": "00:BB:00:00:00:04/None",
        "epB": "of:0000000000000205",
        "type": "UiEdgeLink",
        "portB": "9",
        "rollup": [
          {
            "id": "00:BB:00:00:00:04/None~of:0000000000000205/9",
            "epA": "00:BB:00:00:00:04/None",
            "epB": "of:0000000000000205",
            "type": "UiEdgeLink",
            "portB": "9"
          }
        ]
      },
      {
        "id": "00:AA:00:00:00:02/None~of:0000000000000204/7",
        "epA": "00:AA:00:00:00:02/None",
        "epB": "of:0000000000000204",
        "type": "UiEdgeLink",
        "portB": "7",
        "rollup": [
          {
            "id": "00:AA:00:00:00:02/None~of:0000000000000204/7",
            "epA": "00:AA:00:00:00:02/None",
            "epB": "of:0000000000000204",
            "type": "UiEdgeLink",
            "portB": "7"
          }
        ]
      },
      {
        "id": "00:BB:00:00:00:01/None~of:0000000000000204/8",
        "epA": "00:BB:00:00:00:01/None",
        "epB": "of:0000000000000204",
        "type": "UiEdgeLink",
        "portB": "8",
        "rollup": [
          {
            "id": "00:BB:00:00:00:01/None~of:0000000000000204/8",
            "epA": "00:BB:00:00:00:01/None",
            "epB": "of:0000000000000204",
            "type": "UiEdgeLink",
            "portB": "8"
          }
        ]
      },
      {
        "id": "of:0000000000000207/1~of:0000000000000246/1",
        "epA": "of:0000000000000207/1",
        "epB": "of:0000000000000246/1",
        "type": "UiDeviceLink",
        "portA": "1",
        "portB": "1",
        "rollup": [
          {
            "id": "of:0000000000000207/1~of:0000000000000246/1",
            "epA": "of:0000000000000207/1",
            "epB": "of:0000000000000246/1",
            "type": "UiDeviceLink",
            "portA": "1",
            "portB": "1"
          }
        ]
      },
      {
        "id": "00:88:00:00:00:02/None~of:0000000000000206/7",
        "epA": "00:88:00:00:00:02/None",
        "epB": "of:0000000000000206",
        "type": "UiEdgeLink",
        "portB": "7",
        "rollup": [
          {
            "id": "00:88:00:00:00:02/None~of:0000000000000206/7",
            "epA": "00:88:00:00:00:02/None",
            "epB": "of:0000000000000206",
            "type": "UiEdgeLink",
            "portB": "7"
          }
        ]
      },
      {
        "id": "00:EE:00:00:00:02/None~of:0000000000000208/4",
        "epA": "00:EE:00:00:00:02/None",
        "epB": "of:0000000000000208",
        "type": "UiEdgeLink",
        "portB": "4",
        "rollup": [
          {
            "id": "00:EE:00:00:00:02/None~of:0000000000000208/4",
            "epA": "00:EE:00:00:00:02/None",
            "epB": "of:0000000000000208",
            "type": "UiEdgeLink",
            "portB": "4"
          }
        ]
      },
      {
        "id": "of:0000000000000204/4~of:0000000000000227/4",
        "epA": "of:0000000000000204/4",
        "epB": "of:0000000000000227/4",
        "type": "UiDeviceLink",
        "portA": "4",
        "portB": "4",
        "rollup": [
          {
            "id": "of:0000000000000204/4~of:0000000000000227/4",
            "epA": "of:0000000000000204/4",
            "epB": "of:0000000000000227/4",
            "type": "UiDeviceLink",
            "portA": "4",
            "portB": "4"
          }
        ]
      },
      {
        "id": "of:0000000000000203/5~of:0000000000000204/5",
        "epA": "of:0000000000000203/5",
        "epB": "of:0000000000000204/5",
        "type": "UiDeviceLink",
        "portA": "5",
        "portB": "5",
        "rollup": [
          {
            "id": "of:0000000000000203/5~of:0000000000000204/5",
            "epA": "of:0000000000000203/5",
            "epB": "of:0000000000000204/5",
            "type": "UiDeviceLink",
            "portA": "5",
            "portB": "5"
          }
        ]
      },
      {
        "id": "of:0000000000000227/9~of:0000000000000247/3",
        "epA": "of:0000000000000227/9",
        "epB": "of:0000000000000247/3",
        "type": "UiDeviceLink",
        "portA": "9",
        "portB": "3",
        "rollup": [
          {
            "id": "of:0000000000000227/9~of:0000000000000247/3",
            "epA": "of:0000000000000227/9",
            "epB": "of:0000000000000247/3",
            "type": "UiDeviceLink",
            "portA": "9",
            "portB": "3"
          }
        ]
      }
    ],
    "devices": [
      [],
      [],
      [
        {
          "id": "of:0000000000000246",
          "nodeType": "device",
          "type": "switch",
          "online": true,
          "master": "10.192.19.68",
          "layer": "def",
          "props": {
            "managementAddress": "10.192.19.69",
            "protocol": "OF_13",
            "driver": "ofdpa-ovs",
            "latitude": "40.15",
            "name": "s246",
            "locType": "geo",
            "channelId": "10.192.19.69:59980",
            "longitude": "-121.679"
          },
          "location": {
            "locType": "geo",
            "latOrY": 40.15,
            "longOrX": -121.679
          }
        },
        {
          "id": "of:0000000000000206",
          "nodeType": "device",
          "type": "switch",
          "online": true,
          "master": "10.192.19.68",
          "layer": "def",
          "props": {
            "managementAddress": "10.192.19.69",
            "protocol": "OF_13",
            "driver": "ofdpa-ovs",
            "latitude": "36.766",
            "name": "s206",
            "locType": "geo",
            "channelId": "10.192.19.69:59975",
            "longitude": "-92.029"
          },
          "location": {
            "locType": "geo",
            "latOrY": 36.766,
            "longOrX": -92.029
          }
        },
        {
          "id": "of:0000000000000227",
          "nodeType": "device",
          "type": "switch",
          "online": true,
          "master": "10.192.19.68",
          "layer": "def",
          "props": {
            "managementAddress": "10.192.19.69",
            "protocol": "OF_13",
            "driver": "ofdpa-ovs",
            "latitude": "44.205",
            "name": "s227",
            "locType": "geo",
            "channelId": "10.192.19.69:59979",
            "longitude": "-96.359"
          },
          "location": {
            "locType": "geo",
            "latOrY": 44.205,
            "longOrX": -96.359
          }
        },
        {
          "id": "of:0000000000000208",
          "nodeType": "device",
          "type": "switch",
          "online": true,
          "master": "10.192.19.68",
          "layer": "def",
          "props": {
            "managementAddress": "10.192.19.69",
            "protocol": "OF_13",
            "driver": "ofdpa-ovs",
            "latitude": "36.766",
            "name": "s208",
            "locType": "geo",
            "channelId": "10.192.19.69:59977",
            "longitude": "-116.029"
          },
          "location": {
            "locType": "geo",
            "latOrY": 36.766,
            "longOrX": -116.029
          }
        },
        {
          "id": "of:0000000000000205",
          "nodeType": "device",
          "type": "switch",
          "online": true,
          "master": "10.192.19.68",
          "layer": "def",
          "props": {
            "managementAddress": "10.192.19.69",
            "protocol": "OF_13",
            "driver": "ofdpa-ovs",
            "latitude": "36.766",
            "name": "s205",
            "locType": "geo",
            "channelId": "10.192.19.69:59974",
            "longitude": "-96.89"
          },
          "location": {
            "locType": "geo",
            "latOrY": 36.766,
            "longOrX": -96.89
          }
        },
        {
          "id": "of:0000000000000247",
          "nodeType": "device",
          "type": "switch",
          "online": true,
          "master": "10.192.19.68",
          "layer": "def",
          "props": {
            "managementAddress": "10.192.19.69",
            "protocol": "OF_13",
            "driver": "ofdpa-ovs",
            "latitude": "40.205",
            "name": "s247",
            "locType": "geo",
            "channelId": "10.192.19.69:59981",
            "longitude": "-117.359"
          },
          "location": {
            "locType": "geo",
            "latOrY": 40.205,
            "longOrX": -117.359
          }
        },
        {
          "id": "of:0000000000000226",
          "nodeType": "device",
          "type": "switch",
          "online": true,
          "master": "10.192.19.68",
          "layer": "def",
          "props": {
            "managementAddress": "10.192.19.69",
            "protocol": "OF_13",
            "driver": "ofdpa-ovs",
            "latitude": "44.15",
            "name": "s226",
            "locType": "geo",
            "channelId": "10.192.19.69:59978",
            "longitude": "-107.679"
          },
          "location": {
            "locType": "geo",
            "latOrY": 44.15,
            "longOrX": -107.679
          }
        },
        {
          "id": "of:0000000000000203",
          "nodeType": "device",
          "type": "switch",
          "online": true,
          "master": "10.192.19.68",
          "layer": "def",
          "props": {
            "managementAddress": "10.192.19.69",
            "protocol": "OF_13",
            "driver": "ofdpa-ovs",
            "latitude": "36.766",
            "name": "s203",
            "locType": "geo",
            "channelId": "10.192.19.69:59972",
            "longitude": "-111.359"
          },
          "location": {
            "locType": "geo",
            "latOrY": 36.766,
            "longOrX": -111.359
          }
        },
        {
          "id": "of:0000000000000204",
          "nodeType": "device",
          "type": "switch",
          "online": true,
          "master": "10.192.19.68",
          "layer": "def",
          "props": {
            "managementAddress": "10.192.19.69",
            "protocol": "OF_13",
            "driver": "ofdpa-ovs",
            "latitude": "36.766",
            "name": "s204",
            "locType": "geo",
            "channelId": "10.192.19.69:59973",
            "longitude": "-106.359"
          },
          "location": {
            "locType": "geo",
            "latOrY": 36.766,
            "longOrX": -106.359
          }
        },
        {
          "id": "of:0000000000000207",
          "nodeType": "device",
          "type": "switch",
          "online": true,
          "master": "10.192.19.68",
          "layer": "def",
          "props": {
            "managementAddress": "10.192.19.69",
            "protocol": "OF_13",
            "driver": "ofdpa-ovs",
            "latitude": "36.766",
            "name": "s207",
            "locType": "geo",
            "channelId": "10.192.19.69:59976",
            "longitude": "-122.359"
          },
          "location": {
            "locType": "geo",
            "latOrY": 36.766,
            "longOrX": -122.359
          }
        }
      ]
    ],
    "hosts": [
      [],
      [],
      [
        {
          "id": "00:88:00:00:00:03/110",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::288:ff:fe00:3",
            "2000::102",
            "10.0.1.2"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:DD:00:00:00:01/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [],
          "props": {},
          "configured": false
        },
        {
          "id": "00:88:00:00:00:04/160",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::288:ff:fe00:4",
            "10.0.6.2",
            "2000::602"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:BB:00:00:00:02/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::2bb:ff:fe00:2"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:AA:00:00:00:05/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [],
          "props": {},
          "configured": false
        },
        {
          "id": "00:88:00:00:00:01/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::288:ff:fe00:1",
            "2000::101",
            "10.0.1.1"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:AA:00:00:00:01/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [],
          "props": {},
          "configured": false
        },
        {
          "id": "00:AA:00:00:00:03/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [],
          "props": {},
          "configured": false
        },
        {
          "id": "00:BB:00:00:00:04/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::2bb:ff:fe00:4"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:EE:00:00:00:02/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::2ee:ff:fe00:2"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:99:00:00:00:01/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "10.0.3.253",
            "fe80::299:ff:fe00:1"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:99:66:00:00:01/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::299:66ff:fe00:1",
            "2000::3fd"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:EE:00:00:00:01/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::2ee:ff:fe00:1"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:BB:00:00:00:01/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::2bb:ff:fe00:1"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:BB:00:00:00:03/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::2bb:ff:fe00:3"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:AA:00:00:00:04/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [],
          "props": {},
          "configured": false
        },
        {
          "id": "00:BB:00:00:00:05/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::2bb:ff:fe00:5"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:88:00:00:00:02/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [
            "fe80::288:ff:fe00:2",
            "2000::601",
            "10.0.6.1"
          ],
          "props": {},
          "configured": false
        },
        {
          "id": "00:AA:00:00:00:02/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [],
          "props": {},
          "configured": false
        },
        {
          "id": "00:DD:00:00:00:02/None",
          "nodeType": "host",
          "layer": "def",
          "ips": [],
          "props": {},
          "configured": false
        }
      ]
    ],
    "layerOrder": [
      "opt",
      "pkt",
      "def"
    ]
  }
}`;

const test_OdtnConfig_topo2CurrentRegion = `{
    "event": "topo2CurrentRegion",
    "payload": {
        "id": "(root)",
        "subregions": [],
        "links": [
            {
                "id": "netconf:127.0.0.1:11002/201~netconf:127.0.0.1:11003/201",
                "epA": "netconf:127.0.0.1:11002/201",
                "epB": "netconf:127.0.0.1:11003/201",
                "type": "UiDeviceLink",
                "portA": "201",
                "portB": "201",
                "rollup": [
                    {
                        "id": "netconf:127.0.0.1:11002/201~netconf:127.0.0.1:11003/201",
                        "epA": "netconf:127.0.0.1:11002/201",
                        "epB": "netconf:127.0.0.1:11003/201",
                        "type": "UiDeviceLink",
                        "portA": "201",
                        "portB": "201"
                    }
                ]
            },
            {
                "id": "netconf:127.0.0.1:11002/202~netconf:127.0.0.1:11003/202",
                "epA": "netconf:127.0.0.1:11002/202",
                "epB": "netconf:127.0.0.1:11003/202",
                "type": "UiDeviceLink",
                "portA": "202",
                "portB": "202",
                "rollup": [
                    {
                        "id": "netconf:127.0.0.1:11002/202~netconf:127.0.0.1:11003/202",
                        "epA": "netconf:127.0.0.1:11002/202",
                        "epB": "netconf:127.0.0.1:11003/202",
                        "type": "UiDeviceLink",
                        "portA": "202",
                        "portB": "202"
                    }
                ]
            },
            {
                "id": "netconf:127.0.0.1:11002/203~netconf:127.0.0.1:11003/203",
                "epA": "netconf:127.0.0.1:11002/203",
                "epB": "netconf:127.0.0.1:11003/203",
                "type": "UiDeviceLink",
                "portA": "203",
                "portB": "203",
                "rollup": [
                    {
                        "id": "netconf:127.0.0.1:11002/203~netconf:127.0.0.1:11003/203",
                        "epA": "netconf:127.0.0.1:11002/203",
                        "epB": "netconf:127.0.0.1:11003/203",
                        "type": "UiDeviceLink",
                        "portA": "203",
                        "portB": "203"
                    }
                ]
            },
            {
                "id": "netconf:127.0.0.1:11002/204~netconf:127.0.0.1:11003/204",
                "epA": "netconf:127.0.0.1:11002/204",
                "epB": "netconf:127.0.0.1:11003/204",
                "type": "UiDeviceLink",
                "portA": "204",
                "portB": "204",
                "rollup": [
                    {
                        "id": "netconf:127.0.0.1:11002/204~netconf:127.0.0.1:11003/204",
                        "epA": "netconf:127.0.0.1:11002/204",
                        "epB": "netconf:127.0.0.1:11003/204",
                        "type": "UiDeviceLink",
                        "portA": "204",
                        "portB": "204"
                    }
                ]
            },
            {
                "id": "netconf:127.0.0.1:11002/205~netconf:127.0.0.1:11003/205",
                "epA": "netconf:127.0.0.1:11002/205",
                "epB": "netconf:127.0.0.1:11003/205",
                "type": "UiDeviceLink",
                "portA": "205",
                "portB": "205",
                "rollup": [
                    {
                        "id": "netconf:127.0.0.1:11002/205~netconf:127.0.0.1:11003/205",
                        "epA": "netconf:127.0.0.1:11002/205",
                        "epB": "netconf:127.0.0.1:11003/205",
                        "type": "UiDeviceLink",
                        "portA": "205",
                        "portB": "205"
                    }
                ]
            },
            {
                "id": "netconf:127.0.0.1:11002/206~netconf:127.0.0.1:11003/206",
                "epA": "netconf:127.0.0.1:11002/206",
                "epB": "netconf:127.0.0.1:11003/206",
                "type": "UiDeviceLink",
                "portA": "206",
                "portB": "206",
                "rollup": [
                    {
                        "id": "netconf:127.0.0.1:11002/206~netconf:127.0.0.1:11003/206",
                        "epA": "netconf:127.0.0.1:11002/206",
                        "epB": "netconf:127.0.0.1:11003/206",
                        "type": "UiDeviceLink",
                        "portA": "206",
                        "portB": "206"
                    }
                ]
            }
        ],
        "devices": [
            [],
            [],
            [
                {
                    "id": "netconf:127.0.0.1:11002",
                    "nodeType": "device",
                    "type": "terminal_device",
                    "online": true,
                    "master": "127.0.0.1",
                    "layer": "def",
                    "props": {
                        "ipaddress": "127.0.0.1",
                        "protocol": "NETCONF",
                        "driver": "cassini-ocnos",
                        "port": "11002",
                        "name": "cassini2",
                        "locType": "none"
                    }
                },
                {
                    "id": "netconf:127.0.0.1:11003",
                    "nodeType": "device",
                    "type": "terminal_device",
                    "online": true,
                    "master": "127.0.0.1",
                    "layer": "def",
                    "props": {
                        "ipaddress": "127.0.0.1",
                        "protocol": "NETCONF",
                        "driver": "cassini-ocnos",
                        "port": "11003",
                        "name": "cassini1",
                        "locType": "none"
                    }
                }
            ]
        ],
        "hosts": [
            [],
            [],
            []
        ],
        "layerOrder": [
            "opt",
            "pkt",
            "def"
        ]
    }
}`;

const topo2Highlights_base_data = `{
    "event": "topo2CurrentRegion",
    "payload": {
        "id": "(root)",
        "subregions": [],
        "links": [
            {
                "id": "00:00:00:00:00:22/120~device:leaf4/[3/0](3)",
                "epA": "00:00:00:00:00:22/120",
                "epB": "device:leaf4",
                "type": "UiEdgeLink",
                "portB": "[3/0](3)",
                "rollup": [
                    {
                        "id": "00:00:00:00:00:22/120~device:leaf4/[3/0](3)",
                        "epA": "00:00:00:00:00:22/120",
                        "epB": "device:leaf4",
                        "type": "UiEdgeLink",
                        "portB": "[3/0](3)"
                    }
                ]
            },
            {
                "id": "00:00:00:00:00:1E/None~device:leaf4/[2/0](2)",
                "epA": "00:00:00:00:00:1E/None",
                "epB": "device:leaf4",
                "type": "UiEdgeLink",
                "portB": "[2/0](2)",
                "rollup": [
                    {
                        "id": "00:00:00:00:00:1E/None~device:leaf4/[2/0](2)",
                        "epA": "00:00:00:00:00:1E/None",
                        "epB": "device:leaf4",
                        "type": "UiEdgeLink",
                        "portB": "[2/0](2)"
                    }
                ]
            },
            {
                "id": "device:leaf4/[1/0](1)~device:spine1/[3/0](3)",
                "epA": "device:leaf4/[1/0](1)",
                "epB": "device:spine1/[3/0](3)",
                "type": "UiDeviceLink",
                "portA": "[1/0](1)",
                "portB": "[3/0](3)",
                "rollup": [
                    {
                        "id": "device:leaf4/[1/0](1)~device:spine1/[3/0](3)",
                        "epA": "device:leaf4/[1/0](1)",
                        "epB": "device:spine1/[3/0](3)",
                        "type": "UiDeviceLink",
                        "portA": "[1/0](1)",
                        "portB": "[3/0](3)"
                    }
                ]
            },
            {
                "id": "00:00:00:00:00:21/120~device:leaf3/[leaf3-eth3](3)",
                "epA": "00:00:00:00:00:21/120",
                "epB": "device:leaf3",
                "type": "UiEdgeLink",
                "portB": "[leaf3-eth3](3)",
                "rollup": [
                    {
                        "id": "00:00:00:00:00:21/120~device:leaf3/[leaf3-eth3](3)",
                        "epA": "00:00:00:00:00:21/120",
                        "epB": "device:leaf3",
                        "type": "UiEdgeLink",
                        "portB": "[leaf3-eth3](3)"
                    }
                ]
            },
            {
                "id": "00:00:00:00:00:1D/None~device:leaf3/[leaf3-eth2](2)",
                "epA": "00:00:00:00:00:1D/None",
                "epB": "device:leaf3",
                "type": "UiEdgeLink",
                "portB": "[leaf3-eth2](2)",
                "rollup": [
                    {
                        "id": "00:00:00:00:00:1D/None~device:leaf3/[leaf3-eth2](2)",
                        "epA": "00:00:00:00:00:1D/None",
                        "epB": "device:leaf3",
                        "type": "UiEdgeLink",
                        "portB": "[leaf3-eth2](2)"
                    }
                ]
            },
            {
                "id": "device:leaf3/[leaf3-eth1](1)~device:spine1/[spine1-eth3](3)",
                "epA": "device:leaf3/[leaf3-eth1](1)",
                "epB": "device:spine1/[spine1-eth3](3)",
                "type": "UiDeviceLink",
                "portA": "[leaf3-eth1](1)",
                "portB": "[spine1-eth3](3)",
                "rollup": [
                    {
                        "id": "device:leaf3/[leaf3-eth1](1)~device:spine1/[spine1-eth3](3)",
                        "epA": "device:leaf3/[leaf3-eth1](1)",
                        "epB": "device:spine1/[spine1-eth3](3)",
                        "type": "UiDeviceLink",
                        "portA": "[leaf3-eth1](1)",
                        "portB": "[spine1-eth3](3)"
                    }
                ]
            },
            {
                "id": "00:00:00:00:00:1F/120~device:leaf1/7",
                "epA": "00:00:00:00:00:1F/120",
                "epB": "device:leaf1",
                "type": "UiEdgeLink",
                "portB": "7",
                "rollup": [
                    {
                        "id": "00:00:00:00:00:1F/120~device:leaf1/7",
                        "epA": "00:00:00:00:00:1F/120",
                        "epB": "device:leaf1",
                        "type": "UiEdgeLink",
                        "portB": "7"
                    }
                ]
            },
            {
                "id": "device:leaf1/1~device:spine1/1",
                "epA": "device:leaf1/1",
                "epB": "device:spine1/1",
                "type": "UiDeviceLink",
                "portA": "1",
                "portB": "1",
                "rollup": [
                    {
                        "id": "device:leaf1/1~device:spine1/1",
                        "epA": "device:leaf1/1",
                        "epB": "device:spine1/1",
                        "type": "UiDeviceLink",
                        "portA": "1",
                        "portB": "1"
                    }
                ]
            },
            {
                "id": "device:leaf2/2~device:spine2/2",
                "epA": "device:leaf2/2",
                "epB": "device:spine2/2",
                "type": "UiDeviceLink",
                "portA": "2",
                "portB": "2",
                "rollup": [
                    {
                        "id": "device:leaf2/2~device:spine2/2",
                        "epA": "device:leaf2/2",
                        "epB": "device:spine2/2",
                        "type": "UiDeviceLink",
                        "portA": "2",
                        "portB": "2"
                    }
                ]
            },
            {
                "id": "00:00:00:00:00:1A/None~device:leaf1/3",
                "epA": "00:00:00:00:00:1A/None",
                "epB": "device:leaf1",
                "type": "UiEdgeLink",
                "portB": "3",
                "rollup": [
                    {
                        "id": "00:00:00:00:00:1A/None~device:leaf1/3",
                        "epA": "00:00:00:00:00:1A/None",
                        "epB": "device:leaf1",
                        "type": "UiEdgeLink",
                        "portB": "3"
                    }
                ]
            },
            {
                "id": "00:00:00:00:00:30/None~device:leaf2/3",
                "epA": "00:00:00:00:00:30/None",
                "epB": "device:leaf2",
                "type": "UiEdgeLink",
                "portB": "3",
                "rollup": [
                    {
                        "id": "00:00:00:00:00:30/None~device:leaf2/3",
                        "epA": "00:00:00:00:00:30/None",
                        "epB": "device:leaf2",
                        "type": "UiEdgeLink",
                        "portB": "3"
                    }
                ]
            },
            {
                "id": "device:leaf1/2~device:spine2/1",
                "epA": "device:leaf1/2",
                "epB": "device:spine2/1",
                "type": "UiDeviceLink",
                "portA": "2",
                "portB": "1",
                "rollup": [
                    {
                        "id": "device:leaf1/2~device:spine2/1",
                        "epA": "device:leaf1/2",
                        "epB": "device:spine2/1",
                        "type": "UiDeviceLink",
                        "portA": "2",
                        "portB": "1"
                    }
                ]
            },
            {
                "id": "device:leaf2/1~device:spine1/2",
                "epA": "device:leaf2/1",
                "epB": "device:spine1/2",
                "type": "UiDeviceLink",
                "portA": "1",
                "portB": "2",
                "rollup": [
                    {
                        "id": "device:leaf2/1~device:spine1/2",
                        "epA": "device:leaf2/1",
                        "epB": "device:spine1/2",
                        "type": "UiDeviceLink",
                        "portA": "1",
                        "portB": "2"
                    }
                ]
            },
            {
                "id": "00:00:00:00:00:20/None~device:leaf1/6",
                "epA": "00:00:00:00:00:20/None",
                "epB": "device:leaf1",
                "type": "UiEdgeLink",
                "portB": "6",
                "rollup": [
                    {
                        "id": "00:00:00:00:00:20/None~device:leaf1/6",
                        "epA": "00:00:00:00:00:20/None",
                        "epB": "device:leaf1",
                        "type": "UiEdgeLink",
                        "portB": "6"
                    }
                ]
            },
            {
                "id": "00:00:00:00:00:1C/None~device:leaf1/5",
                "epA": "00:00:00:00:00:1C/None",
                "epB": "device:leaf1",
                "type": "UiEdgeLink",
                "portB": "5",
                "rollup": [
                    {
                        "id": "00:00:00:00:00:1C/None~device:leaf1/5",
                        "epA": "00:00:00:00:00:1C/None",
                        "epB": "device:leaf1",
                        "type": "UiEdgeLink",
                        "portB": "5"
                    }
                ]
            },
            {
                "id": "00:00:00:00:00:1B/None~device:leaf1/4",
                "epA": "00:00:00:00:00:1B/None",
                "epB": "device:leaf1",
                "type": "UiEdgeLink",
                "portB": "4",
                "rollup": [
                    {
                        "id": "00:00:00:00:00:1B/None~device:leaf1/4",
                        "epA": "00:00:00:00:00:1B/None",
                        "epB": "device:leaf1",
                        "type": "UiEdgeLink",
                        "portB": "4"
                    }
                ]
            }
        ],
        "devices": [
            [],
            [],
            [
                {
                    "id": "device:leaf4",
                    "nodeType": "device",
                    "type": "switch",
                    "online": true,
                    "master": "172.24.0.3",
                    "layer": "def",
                    "props": {
                        "managementAddress": "grpc://pippo:50003?device_id=1",
                        "protocol": "P4Runtime, gNMI, gNOI",
                        "gridX": "400.0",
                        "gridY": "400.0",
                        "driver": "stratum-tofino",
                        "name": "device:leaf4",
                        "p4DeviceId": "1",
                        "locType": "grid"
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 400.0,
                        "longOrX": 400.0
                    }
                },
                {
                    "id": "device:leaf3",
                    "nodeType": "device",
                    "type": "switch",
                    "online": true,
                    "master": "172.24.0.3",
                    "layer": "def",
                    "props": {
                        "managementAddress": "grpc://mininet:50003?device_id=1",
                        "protocol": "P4Runtime, gNMI, gNOI",
                        "gridX": "400.0",
                        "gridY": "400.0",
                        "driver": "stratum-bmv2",
                        "name": "device:leaf3",
                        "p4DeviceId": "1",
                        "locType": "grid"
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 400.0,
                        "longOrX": 400.0
                    }
                },
                {
                    "id": "device:spine1",
                    "nodeType": "device",
                    "type": "switch",
                    "online": true,
                    "master": "172.24.0.3",
                    "layer": "def",
                    "props": {
                        "managementAddress": "grpc://mininet:50003?device_id=1",
                        "protocol": "P4Runtime, gNMI, gNOI",
                        "gridX": "400.0",
                        "gridY": "400.0",
                        "driver": "stratum-bmv2",
                        "name": "device:spine1",
                        "p4DeviceId": "1",
                        "locType": "grid"
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 400.0,
                        "longOrX": 400.0
                    }
                },
                {
                    "id": "device:spine2",
                    "nodeType": "device",
                    "type": "switch",
                    "online": true,
                    "master": "172.24.0.3",
                    "layer": "def",
                    "props": {
                        "managementAddress": "grpc://mininet:50004?device_id=1",
                        "protocol": "P4Runtime, gNMI, gNOI",
                        "gridX": "600.0",
                        "gridY": "400.0",
                        "driver": "stratum-bmv2",
                        "name": "device:spine2",
                        "p4DeviceId": "1",
                        "locType": "grid"
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 400.0,
                        "longOrX": 600.0
                    }
                },
                {
                    "id": "device:leaf2",
                    "nodeType": "device",
                    "type": "switch",
                    "online": true,
                    "master": "172.24.0.3",
                    "layer": "def",
                    "props": {
                        "managementAddress": "grpc://mininet:50002?device_id=1",
                        "protocol": "P4Runtime, gNMI, gNOI",
                        "gridX": "800.0",
                        "gridY": "600.0",
                        "driver": "stratum-bmv2",
                        "name": "device:leaf2",
                        "p4DeviceId": "1",
                        "locType": "grid"
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 600.0,
                        "longOrX": 800.0
                    }
                },
                {
                    "id": "device:leaf1",
                    "nodeType": "device",
                    "type": "switch",
                    "online": true,
                    "master": "172.24.0.3",
                    "layer": "def",
                    "props": {
                        "managementAddress": "grpc://mininet:50001?device_id=1",
                        "protocol": "P4Runtime, gNMI, gNOI",
                        "gridX": "200.0",
                        "gridY": "600.0",
                        "driver": "stratum-bmv2",
                        "name": "device:leaf1",
                        "p4DeviceId": "1",
                        "locType": "grid"
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 600.0,
                        "longOrX": 200.0
                    }
                }
            ]
        ],
        "hosts": [
            [],
            [],
            [
                {
                    "id": "00:00:00:00:00:22/120",
                    "nodeType": "host",
                    "layer": "def",
                    "ips": [
                        "2001:2:3::1"
                    ],
                    "props": {
                        "gridX": "750.0",
                        "gridY": "700.0",
                        "latitude": null,
                        "name": "h3",
                        "locType": "grid",
                        "longitude": null
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 700.0,
                        "longOrX": 750.0
                    },
                    "configured": false
                },
                {
                    "id": "00:00:00:00:00:21/120",
                    "nodeType": "host",
                    "layer": "def",
                    "ips": [
                        "2001:2:3::1"
                    ],
                    "props": {
                        "gridX": "750.0",
                        "gridY": "700.0",
                        "latitude": null,
                        "name": "h3",
                        "locType": "grid",
                        "longitude": null
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 700.0,
                        "longOrX": 750.0
                    },
                    "configured": false
                },
                {
                    "id": "00:00:00:00:00:1F/120",
                    "nodeType": "host",
                    "layer": "def",
                    "ips": [
                        "2001:2:3::1"
                    ],
                    "props": {
                        "gridX": "750.0",
                        "gridY": "700.0",
                        "latitude": null,
                        "name": "h3",
                        "locType": "grid",
                        "longitude": null
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 700.0,
                        "longOrX": 750.0
                    },
                    "configured": false
                },
                {
                    "id": "00:00:00:00:00:1E/None",
                    "nodeType": "host",
                    "layer": "def",
                    "ips": [
                        "2001:2:3::1"
                    ],
                    "props": {
                        "gridX": "750.0",
                        "gridY": "700.0",
                        "latitude": null,
                        "name": "h3",
                        "locType": "grid",
                        "longitude": null
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 700.0,
                        "longOrX": 750.0
                    },
                    "configured": false
                },
                {
                    "id": "00:00:00:00:00:1D/None",
                    "nodeType": "host",
                    "layer": "def",
                    "ips": [
                        "2001:2:3::1"
                    ],
                    "props": {
                        "gridX": "750.0",
                        "gridY": "700.0",
                        "latitude": null,
                        "name": "h3",
                        "locType": "grid",
                        "longitude": null
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 700.0,
                        "longOrX": 750.0
                    },
                    "configured": false
                },
                {
                    "id": "00:00:00:00:00:30/None",
                    "nodeType": "host",
                    "layer": "def",
                    "ips": [
                        "2001:2:3::1"
                    ],
                    "props": {
                        "gridX": "750.0",
                        "gridY": "700.0",
                        "latitude": null,
                        "name": "h3",
                        "locType": "grid",
                        "longitude": null
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 700.0,
                        "longOrX": 750.0
                    },
                    "configured": false
                },
                {
                    "id": "00:00:00:00:00:1A/None",
                    "nodeType": "host",
                    "layer": "def",
                    "ips": [
                        "2001:1:1::a"
                    ],
                    "props": {
                        "gridX": "100.0",
                        "gridY": "700.0",
                        "latitude": null,
                        "name": "h1a",
                        "locType": "grid",
                        "longitude": null
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 700.0,
                        "longOrX": 100.0
                    },
                    "configured": false
                },
                {
                    "id": "00:00:00:00:00:1B/None",
                    "nodeType": "host",
                    "layer": "def",
                    "ips": [
                        "2001:1:1::b"
                    ],
                    "props": {
                        "gridX": "100.0",
                        "gridY": "800.0",
                        "latitude": null,
                        "name": "h1b",
                        "locType": "grid",
                        "longitude": null
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 800.0,
                        "longOrX": 100.0
                    },
                    "configured": false
                },
                {
                    "id": "00:00:00:00:00:1C/None",
                    "nodeType": "host",
                    "layer": "def",
                    "ips": [
                        "2001:1:1::c"
                    ],
                    "props": {
                        "gridX": "250.0",
                        "gridY": "800.0",
                        "latitude": null,
                        "name": "h1c",
                        "locType": "grid",
                        "longitude": null
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 800.0,
                        "longOrX": 250.0
                    },
                    "configured": false
                },
                {
                    "id": "00:00:00:00:00:20/None",
                    "nodeType": "host",
                    "layer": "def",
                    "ips": [
                        "2001:1:2::1"
                    ],
                    "props": {
                        "gridX": "400.0",
                        "gridY": "700.0",
                        "latitude": null,
                        "name": "h2",
                        "locType": "grid",
                        "longitude": null
                    },
                    "location": {
                        "locType": "grid",
                        "latOrY": 700.0,
                        "longOrX": 400.0
                    },
                    "configured": false
                }
            ]
        ],
        "layerOrder": [
            "opt",
            "pkt",
            "def"
        ]
    }
}`;

const topo2Highlights_sample = `
{
  "event": "topo2Highlights",
  "payload": {
    "devices": [],
    "hosts": [],
    "links": [
      {
        "id": "00:00:00:00:00:22/120~device:leaf4/[3/0](3)",
        "label": "964.91 Kbps",
        "css": "secondary port-traffic-green"
      },
      {
        "id": "00:00:00:00:00:1E/None~device:leaf4/[2/0](2)",
        "label": "964.91 Kbps",
        "css": "secondary port-traffic-green"
      },
      {
        "id": "device:leaf4/[1/0](1)~device:spine1/[3/0](3)",
        "label": "964.91 Kbps",
        "css": "secondary port-traffic-green"
      },
      {
        "id": "00:00:00:00:00:21/120~device:leaf3/[leaf3-eth3](3)",
        "label": "964.91 Kbps",
        "css": "secondary port-traffic-green"
      },
      {
        "id": "00:00:00:00:00:1D/None~device:leaf3/[leaf3-eth2](2)",
        "label": "964.91 Kbps",
        "css": "secondary port-traffic-green"
      },
      {
        "id": "device:leaf3/[leaf3-eth1](1)~device:spine1/[spine1-eth3](3)",
        "label": "964.91 Kbps",
        "css": "secondary port-traffic-green"
      },
      {
        "id": "00:00:00:00:00:1F/120~device:leaf1/7",
        "label": "964.91 Kbps",
        "css": "secondary port-traffic-green"
      },
      {
        "id": "device:leaf2/2~device:spine2/2",
        "label": "964.91 Kbps",
        "css": "secondary port-traffic-green"
      },
      {
        "id": "device:leaf1/1~device:spine1/1",
        "label": "3.92 Mbps",
        "css": "secondary port-traffic-yellow"
      },
      {
        "id": "00:00:00:00:00:30/None~device:leaf2/3",
        "label": "4.46 Mbps",
        "css": "secondary port-traffic-yellow"
      },
      {
        "id": "device:leaf2/1~device:spine1/2",
        "label": "3.53 Mbps",
        "css": "secondary port-traffic-yellow"
      },
      {
        "id": "device:leaf1/2~device:spine2/1",
        "label": "1.06 Mbps",
        "css": "secondary port-traffic-yellow"
      },
      {
        "id": "00:00:00:00:00:20/None~device:leaf1/6",
        "label": "4.98 Mbps",
        "css": "secondary port-traffic-yellow"
      }
    ]
  }
}`;

const topo2Highlights_sample2 = `
{
  "event": "topo2Highlights",
  "payload": {
    "devices": [],
    "hosts": [],
    "links": []
  }
}`;

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockIconService {
    loadIconDef() { }
}

class MockSvgUtilService {

    cat7() {
        const tcid = 'd3utilTestCard';

        function getColor(id, muted, theme) {
            // NOTE: since we are lazily assigning domain ids, we need to
            //       get the color from all 4 scales, to keep the domains
            //       in sync.
            const ln = '#5b99d2';
            const lm = '#9ebedf';
            const dn = '#5b99d2';
            const dm = '#9ebedf';
            if (theme === 'dark') {
                return muted ? dm : dn;
            } else {
                return muted ? lm : ln;
            }
        }

        return {
            // testCard: testCard,
            getColor: getColor,
        };
    }
}

class MockUrlFnService { }

class MockWebSocketService {
    createWebSocket() { }
    isConnected() { return false; }
    unbindHandlers() { }
    bindHandlers() { }
}

class MockTopologyService {
    public instancesIndex: Map<string, number>;
    constructor() {
        this.instancesIndex = new Map();
    }
}

describe('ForceSvgComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: ForceSvgComponent;
    let fixture: ComponentFixture<ForceSvgComponent>;
    const openflowSampleData = JSON.parse(test_module_topo2CurrentRegion);
    const openflowRegionData: Region = <Region><unknown>(openflowSampleData.payload);

    const odtnSampleData = JSON.parse(test_OdtnConfig_topo2CurrentRegion);
    const odtnRegionData: Region = <Region><unknown>(odtnSampleData.payload);

    const topo2BaseData = JSON.parse(topo2Highlights_base_data);
    const topo2BaseRegionData: Region = <Region><unknown>(topo2BaseData.payload);

    const highlightSampleData = JSON.parse(topo2Highlights_sample);
    const linkHightlights: LinkHighlight[] = <LinkHighlight[]><unknown>(highlightSampleData.payload.links);

    const emptyRegion: Region = <Region>{devices: [ [], [], [] ], hosts: [ [], [], [] ], links: []};

    beforeEach(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({ 'debug': 'txrx' });

        windowMock = <any>{
            location: <any>{
                hostname: 'foo',
                host: 'foo',
                port: '80',
                protocol: 'http',
                search: { debug: 'true' },
                href: 'ws://foo:123/onos/ui/websock/path',
                absUrl: 'ws://foo:123/onos/ui/websock/path'
            }
        };

        const bundleObj = {
            'core.view.Topo': {
                test: 'test1'
            }
        };
        const mockLion = (key) => {
            return bundleObj[key] || '%' + key + '%';
        };

        fs = new FnService(ar, logSpy, windowMock);

        TestBed.configureTestingModule({
            declarations: [
                ForceSvgComponent,
                DeviceNodeSvgComponent,
                HostNodeSvgComponent,
                SubRegionNodeSvgComponent,
                LinkSvgComponent,
                DraggableDirective,
                BadgeSvgComponent
            ],
            providers: [
                { provide: LogService, useValue: logSpy },
                { provide: ActivatedRoute, useValue: ar },
                { provide: FnService, useValue: fs },
                { provide: ChangeDetectorRef, useClass: ChangeDetectorRef },
                { provide: UrlFnService, useClass: MockUrlFnService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: LionService, useFactory: (() => {
                        return {
                            bundle: ((bundleId) => mockLion),
                            ubercache: new Array(),
                            loadCbs: new Map<string, () => void>([])
                        };
                    })
                },
                { provide: IconService, useClass: MockIconService },
                { provide: SvgUtilService, useClass: MockSvgUtilService },
                { provide: TopologyService, useClass: MockTopologyService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
        .compileComponents();
        logServiceSpy = TestBed.get(LogService);

        fixture = TestBed.createComponent(ForceSvgComponent);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('load sample files', () => {
        expect(openflowSampleData).toBeTruthy();
        expect(openflowSampleData.payload).toBeTruthy();
        expect(openflowSampleData.payload.id).toBe('(root)');

        expect(odtnSampleData).toBeTruthy();
        expect(odtnSampleData.payload).toBeTruthy();
        expect(odtnSampleData.payload.id).toBe('(root)');
    });

    it('should read sample data payload as Region', () => {
        expect(openflowRegionData).toBeTruthy();
        // console.log(regionData);
        expect(openflowRegionData.id).toBe('(root)');
        expect(openflowRegionData.devices).toBeTruthy();
        expect(openflowRegionData.devices.length).toBe(3);
        expect(openflowRegionData.devices[2].length).toBe(10);
        expect(openflowRegionData.hosts.length).toBe(3);
        expect(openflowRegionData.hosts[2].length).toBe(20);
        expect(openflowRegionData.links.length).toBe(44);
    });

    it('should read device246 correctly', () => {
        const device246: Device = openflowRegionData.devices[2][0];
        expect(device246.id).toBe('of:0000000000000246');
        expect(device246.nodeType).toBe('device');
        expect(device246.type).toBe('switch');
        expect(device246.online).toBe(true);
        expect(device246.master).toBe('10.192.19.68');
        expect(device246.layer).toBe('def');

        expect(device246.props.managementAddress).toBe('10.192.19.69');
        expect(device246.props.protocol).toBe('OF_13');
        expect(device246.props.driver).toBe('ofdpa-ovs');
        expect(device246.props.latitude).toBe('40.15');
        expect(device246.props.name).toBe('s246');
        expect(device246.props.locType).toBe('geo');
        expect(device246.props.channelId).toBe('10.192.19.69:59980');
        expect(device246.props.longitude).toBe('-121.679');

        expect(device246.location.locType).toBe('geo');
        expect(device246.location.latOrY).toBe(40.15);
        expect(device246.location.longOrX).toBe(-121.679);
    });

    it('should read host 3 correctly', () => {
        const host3: Host = openflowRegionData.hosts[2][0];
        expect(host3.id).toBe('00:88:00:00:00:03/110');
        expect(host3.nodeType).toBe('host');
        expect(host3.layer).toBe('def');
        expect(host3.configured).toBe(false);
        expect(host3.ips.length).toBe(3);
        expect(host3.ips[0]).toBe('fe80::288:ff:fe00:3');
        expect(host3.ips[1]).toBe('2000::102');
        expect(host3.ips[2]).toBe('10.0.1.2');
    });

    it('should read link 3-205 correctly', () => {
        const link3_205: Link = openflowRegionData.links[0];
        expect(link3_205.id).toBe('00:AA:00:00:00:03/None~of:0000000000000205/6');
        expect(link3_205.epA).toBe('00:AA:00:00:00:03/None');
        expect(link3_205.epB).toBe('of:0000000000000205');
        expect(String(LinkType[link3_205.type])).toBe('2');
        expect(link3_205.portA).toBe(undefined);
        expect(link3_205.portB).toBe('6');

        expect(link3_205.rollup).toBeTruthy();
        expect(link3_205.rollup.length).toBe(1);
        expect(link3_205.rollup[0].id).toBe('00:AA:00:00:00:03/None~of:0000000000000205/6');
        expect(link3_205.rollup[0].epA).toBe('00:AA:00:00:00:03/None');
        expect(link3_205.rollup[0].epB).toBe('of:0000000000000205');
        expect(String(LinkType[link3_205.rollup[0].type])).toBe('2');
        expect(link3_205.rollup[0].portA).toBe(undefined);
        expect(link3_205.rollup[0].portB).toBe('6');

    });

    it('should handle regionData change - empty Region', () => {
        component.ngOnChanges(
            {'regionData' : new SimpleChange(<Region>{}, emptyRegion, true)});

        expect(component.graph.nodes.length).toBe(0);
    });

    it('should know how to format names', () => {
        expect(ForceSvgComponent.extractNodeName('00:AA:00:00:00:03/None', undefined))
            .toEqual('00:AA:00:00:00:03/None');

        expect(ForceSvgComponent.extractNodeName('00:AA:00:00:00:03/161', '161'))
            .toEqual('00:AA:00:00:00:03');

        // Like epB of first example in sampleData file - endPtStr contains port number
        expect(ForceSvgComponent.extractNodeName('of:0000000000000206/6', '6'))
            .toEqual('of:0000000000000206');

        // Like epB of second example in sampleData file - endPtStr does not contain port number
        expect(ForceSvgComponent.extractNodeName('of:0000000000000206', '6'))
            .toEqual('of:0000000000000206');

        // bmv2 case - no port in the endpoint
        expect(ForceSvgComponent.extractNodeName('device:leaf1', '[leaf1-eth1](1)'))
            .toEqual('device:leaf1');

        // bmv2 case - port in the endpoint
        expect(ForceSvgComponent.extractNodeName('device:leaf1/[leaf1-eth1](1)', '[leaf1-eth1](1)'))
            .toEqual('device:leaf1');

        // tofino case - no port in the endpoint
        expect(ForceSvgComponent.extractNodeName('device:leaf1', '[1/0](1)'))
            .toEqual('device:leaf1');

        // tofino case - port in the endpoint
        expect(ForceSvgComponent.extractNodeName('device:leaf1/[1/0](1)', '[1/0](1)'))
            .toEqual('device:leaf1');

    });

    it('should handle openflow regionData change - sample Region', () => {
        component.regionData = openflowRegionData;
        component.ngOnChanges(
            {'regionData' : new SimpleChange(<Region>{}, openflowRegionData, true)});

        expect(component.graph.nodes.length).toBe(30);

        expect(component.graph.links.length).toBe(44);

    });

    it('should handle odtn regionData change - sample odtn Region', () => {
        component.regionData = odtnRegionData;
        component.ngOnChanges(
            {'regionData' : new SimpleChange(<Region>{}, odtnRegionData, true)});

        expect(component.graph.nodes.length).toBe(2);

        expect(component.graph.links.length).toBe(6);

    });

    it('should handle highlights and match them to existing links', () => {
        component.regionData = topo2BaseRegionData;
        component.ngOnChanges(
            {'regionData' : new SimpleChange(<Region>{}, topo2BaseRegionData, true)});

        expect(component.graph.links.length).toBe(16);
        expect(component.graph.nodes.length).toBe(16)
        expect(linkHightlights.length).toBe(13);

        // sanitize deviceNameFromEp
        component.graph.links.forEach((l: Link) => {
            if (<LinkType><unknown>LinkType[l.type] === LinkType.UiEdgeLink) {
                // edge link has only one epoint valid (the other is not deviceId)
                const foundNode = component.graph.nodes.find((n: Node) => n.id === Link.deviceNameFromEp(l.epB));
                expect(foundNode).toBeDefined();
            } else {
                var foundNode = component.graph.nodes.find((n: Node) => n.id === Link.deviceNameFromEp(l.epA));
                expect(foundNode).toBeDefined();
                foundNode = component.graph.nodes.find((n: Node) => n.id === Link.deviceNameFromEp(l.epB));
                expect(foundNode).toBeDefined();
            }
        });

        // should be able to find all of the highlighted links in the original data set
        linkHightlights.forEach((lh: LinkHighlight) => {
            const foundLink = component.graph.links.find((l: Link) => l.id === Link.linkIdFromShowHighlights(lh.id));
            expect(foundLink).toBeDefined();
        });
    });
});
