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
import { Component, OnInit, OnDestroy } from '@angular/core';
import {
    FnService,
    LogService,
    WebSocketService,
    SortDir, TableBaseImpl, TableResponse
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * Model of the response from WebSocket
 */
interface TunnelTableResponse extends TableResponse {
    tunnels: Tunnel[];
}

/**
 * Model of the tunnels returned from the WebSocket
 */
interface Tunnel {
    id: string;
    name: string;
    port1: string;
    port2: string;
    type: string;
    groupId: string;
    bandwidth: string;
    path: string;
}

/**
 * ONOS GUI -- Tunnel View Component
 */
@Component({
    selector: 'onos-tunnel',
    templateUrl: './tunnel.component.html',
    styleUrls: ['./tunnel.component.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})
export class TunnelComponent extends TableBaseImpl implements OnInit, OnDestroy {

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'tunnel');
        this.responseCallback = this.tunnelResponseCb;
        this.sortParams = {
            firstCol: 'id',
            firstDir: SortDir.desc,
            secondCol: 'name',
            secondDir: SortDir.asc,
        };
    }

    ngOnInit() {
        this.init();
        this.log.debug('TunnelComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('TunnelComponent destroyed');
    }

    tunnelResponseCb(data: TunnelTableResponse) {
        this.log.debug('Tunnel response received for ', data.tunnels.length, 'tunnels');
    }

}
