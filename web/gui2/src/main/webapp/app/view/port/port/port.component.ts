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
    PrefsService,
    WebSocketService,
    SortDir, TableBaseImpl, TableResponse
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { ActivatedRoute } from '@angular/router';

/**
 * Model of the response from WebSocket
 */
interface PortTableResponse extends TableResponse {
    ports: Port[];
}

/**
 * Model of the ports returned from the WebSocket
 */
interface Port {
    id: string;
    pktsRecieved: string;
    pktsSent: string;
    byteRecieved: string;
    byteSent: string;
    pktsRxDropped: string;
    pktsTxDropped: string;
    duration: string;
}

interface FilterToggleState {
    devId: string;
    nzFilter: boolean;
    showDelta: boolean;
}

const defaultPortPrefsState = {
    nzFilter: 1,
    showDelta: 0,
};

/**
 * ONOS GUI -- Port View Component
 */
@Component({
    selector: 'onos-port',
    templateUrl: './port.component.html',
    styleUrls: ['./port.component.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})
export class PortComponent extends TableBaseImpl implements OnInit, OnDestroy {
    devId: string;
    nzFilter: boolean = true;
    showDelta: boolean = false;
    prefsState = {};
    toggleState: FilterToggleState;

    restorePrefsConfig; // Function

    deviceTip = 'Show device table';
    flowTip = 'Show flow view for this device';
    groupTip = 'Show group view for this device';
    meterTip = 'Show meter view for selected device';
    pipeconfTip = 'Show pipeconf view for selected device';
    toggleDeltaTip = 'Toggle port delta statistics';
    toggleNZTip = 'Toggle non zero port statistics';

    constructor(protected fs: FnService,
        protected log: LogService,
        protected ar: ActivatedRoute,
        protected wss: WebSocketService,
        protected prefs: PrefsService,
    ) {
        super(fs, log, wss, 'port');
        this.ar.queryParams.subscribe(params => {
            this.devId = params['devId'];

        });

        this.payloadParams = {
            devId: this.devId
        };

        this.responseCallback = this.portResponseCb;
        this.restorePrefsConfig = this.restoreConfigFromPrefs;

        this.sortParams = {
            firstCol: 'id',
            firstDir: SortDir.desc,
            secondCol: 'pkt_rx',
            secondDir: SortDir.asc,
        };
    }

    ngOnInit() {
        this.init();
        this.log.debug('PortComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('PortComponent destroyed');
    }

    portResponseCb(data: PortTableResponse) {
        this.log.debug('Port response received for ', data.ports.length, 'port');
    }

    isNz(): boolean {
        return this.nzFilter;
    }

    isDelta(): boolean {
        return this.showDelta;
    }

    toggleNZState(b?: any) {
        if (b === undefined) {
            this.nzFilter = !this.nzFilter;
        } else {
            this.nzFilter = b;
        }
        this.payloadParams = this.filterToggleState();
        this.updatePrefsState('nzFilter', this.nzFilter);
        this.forceRefesh();
    }

    toggleDeltaState(b?: any) {
        if (b === undefined) {
            this.showDelta = !this.showDelta;
        } else {
            this.showDelta = b;
        }

        this.payloadParams = this.filterToggleState();
        this.updatePrefsState('showDelta', this.showDelta);
        this.forceRefesh();
    }

    updatePrefsState(what: any, b: any) {
        this.prefsState[what] = b ? 1 : 0;
        this.prefs.setPrefs('port_prefs', this.prefsState);
    }

    filterToggleState(): FilterToggleState {
        return this.toggleState = {
            devId: this.devId,
            nzFilter: this.nzFilter,
            showDelta: this.showDelta,
        };
    }

    forceRefesh() {
        this.requestTableData();
    }

    restoreConfigFromPrefs() {
        this.prefsState = this.prefs.asNumbers(
            this.prefs.getPrefs('port_prefs', defaultPortPrefsState, )
        );

        this.log.debug('Port - Prefs State:', this.prefsState);
        this.toggleDeltaState(this.prefsState['showDelta']);
        this.toggleNZState(this.prefsState['nzFilter']);
    }

}
