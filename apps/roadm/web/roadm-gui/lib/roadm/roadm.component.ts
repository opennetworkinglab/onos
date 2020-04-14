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
import { Component, OnInit, OnDestroy} from '@angular/core';

import {
    FnService,
    LogService,
    WebSocketService,
    SortDir, TableBaseImpl, TableResponse
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

import { ActivatedRoute, Router } from '@angular/router';

/**
 * Model of the response from WebSocket
 */
interface RoadmDeviceTableResponse extends TableResponse {
    roadms: RoadmDevice[];
}

/**
 * Model of the ROADM devices returned from the WebSocket
 */
interface RoadmDevice {
    available: boolean;
    chassisid: string;
    hwVersion: string;
    id: string;
    master: string;
    Vendor: string;
    name: string;
    ports: number;
    protocol: string;
    serial: string;
    swVersion: string;
    type: string;
    _iconid_available: string;
    _iconid_type: string;
}


/**
 * ONOS GUI -- Roadm Device View Component
 */
@Component({
    selector: 'roadm-device',
    templateUrl: './roadm.component.html',
    styleUrls: ['./roadm.component.css', './roadm.theme.css',
        '../../../../../../web/gui2-fw-lib/lib/widget/table.css',
        '../../../../../../web/gui2-fw-lib/lib/widget/table.theme.css'
    ]
})
export class RoadmDeviceComponent extends TableBaseImpl implements OnInit, OnDestroy {

    // TODO: Update for LION
    flowTip = 'Show flow view for selected device';
    portTip = 'Show port view for selected device';
    groupTip = 'Show group view for selected device';
    meterTip = 'Show meter view for selected device';
    pipeconfTip = 'Show pipeconf view for selected device';

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected as: ActivatedRoute,
        protected router: Router,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'roadm');
        this.responseCallback = this.deviceResponseCb;

        this.as.queryParams.subscribe(params => {
            this.selId = params['devId'];

        });

        this.payloadParams = {
            devId: this.selId
        };

        this.sortParams = {
            firstCol: 'name',
            firstDir: SortDir.asc,
            secondCol: 'id',
            secondDir: SortDir.desc,
        };
    }

    ngOnInit() {
        this.init();
        this.log.debug('RoadmDeviceComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('RoadmDeviceComponent destroyed');
    }

    deviceResponseCb(data: RoadmDeviceTableResponse) {
        this.log.debug('Device response received for ', data.roadms.length, 'roadm devices');
    }

    navto(path) {
        this.log.debug('navigate');
        if (this.selId) {
            this.router.navigate([path], { queryParams: { devId: this.selId } });
        }
    }

}
