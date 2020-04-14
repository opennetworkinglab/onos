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
import { Component, OnInit, OnDestroy } from '@angular/core';
import {
    FnService,
    LogService,
    WebSocketService,
    SortDir, TableBaseImpl, TableResponse
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

interface HostTableResponse extends TableResponse {
    hosts: Host[];
}

interface Host {
    name: boolean;
    id: string;
    hw: string;
    vlanId: string;
    configured: string;
    address: string;
    location: string;
    _iconid_type: string;
}

/**
 * ONOS GUI -- Host View Component
 */
@Component({
    selector: 'onos-host',
    templateUrl: './host.component.html',
    styleUrls: ['./host.component.css',
        '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})
export class HostComponent extends TableBaseImpl implements OnInit, OnDestroy {

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'host');
        this.responseCallback = this.hostResponseCb;
        this.sortParams = {
            firstCol: 'name',
            firstDir: SortDir.desc,
            secondCol: 'id',
            secondDir: SortDir.asc,
        };
    }

    ngOnInit() {
        this.init();
        this.log.debug('HostComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('HostComponent destroyed');
    }

    hostResponseCb(data: HostTableResponse) {
        this.log.debug('Host response received for ', data.hosts.length, 'host');
    }

}
