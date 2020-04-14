/*
 * Copyright 2018-present Open Networking Foundation
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
interface DeviceTableResponse extends TableResponse {
    devices: Device[];
}

/**
 * Model of the devices returned from the WebSocket
 */
interface Device {
    available: boolean;
    chassisid: string;
    hw: string;
    id: string;
    masterid: string;
    mfr: string;
    name: string;
    num_ports: number;
    protocol: string;
    serial: string;
    sw: string;
    _iconid_available: string;
    _iconid_type: string;
}


/**
 * ONOS GUI -- Device View Component
 */
@Component({
    selector: 'onos-device',
    templateUrl: './device.component.html',
    styleUrls: ['./device.component.css', './device.theme.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})
export class DeviceComponent extends TableBaseImpl implements OnInit, OnDestroy {

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
        super(fs, log, wss, 'device');
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
        this.log.debug('DeviceComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('DeviceComponent destroyed');
    }

    deviceResponseCb(data: DeviceTableResponse) {
        this.log.debug('Device response received for ', data.devices.length, 'devices');
    }

    navto(path) {
        this.log.debug('navigate to', path);
        if (this.selId) {
            this.router.navigate([path], { queryParams: { devId: this.selId } });
        }
    }

}
