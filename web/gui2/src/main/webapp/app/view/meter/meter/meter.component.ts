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
import { Component, OnDestroy, OnInit } from '@angular/core';
import {
    FnService,
    LogService,
    WebSocketService,
    SortDir, TableBaseImpl, TableResponse
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { ActivatedRoute } from '@angular/router';

/**
* Model of the response from WebSocket
*/
interface MeterTableResponse extends TableResponse {
    meters: Meter[];
}

/**
* Model of the meter returned from the WebSocket
*/
interface Meter {
    id: string;
    appId: string;
    state: string;
    packets: string;
    bytes: string;
}

/**
 * ONOS GUI -- Meter View Component
 */
@Component({
    selector: 'onos-meter',
    templateUrl: './meter.component.html',
    styleUrls: ['./meter.component.css', './meter.theme.css',
        '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})
export class MeterComponent extends TableBaseImpl implements OnInit, OnDestroy {

    id: string;
    brief: boolean = true;

    // TODO: Update for LION
    deviceTip = 'Show device table';
    detailTip = 'Switch to detail view';
    flowTip = 'Show flow view for selected device';
    portTip = 'Show port view for selected device';
    groupTip = 'Show group view for selected device';
    pipeconfTip = 'Show pipeconf view for selected device';

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected as: ActivatedRoute,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'meter');
        this.as.queryParams.subscribe(params => {
            this.id = params['devId'];
        });

        this.payloadParams = {
            devId: this.id
        };

        this.responseCallback = this.meterResponseCb;
        this.sortParams = {
            firstCol: 'id',
            firstDir: SortDir.desc,
            secondCol: 'app_id',
            secondDir: SortDir.asc,
        };
    }

    ngOnInit() {
        this.init();
        this.log.debug('MeterComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('MeterComponent destroyed');
    }

    meterResponseCb(data: MeterTableResponse) {
        this.log.debug('Meter response received for ', data.meters.length, 'meter');
    }

    briefToggle() {
        this.brief = !this.brief;
    }

}
