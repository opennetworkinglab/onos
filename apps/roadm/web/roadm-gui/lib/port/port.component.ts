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
import { Component, OnDestroy, OnInit } from '@angular/core';
import {
    FnService,
    LogService,
    PrefsService,
    WebSocketService,
    IconService,
    SortDir, TableBaseImpl, TableResponse
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { ActivatedRoute } from '@angular/router';
import {FormGroup, FormControl} from '@angular/forms';

/**
 * Model of the response from WebSocket
 */
interface RoadmPortTableResponse extends TableResponse {
    roadmPorts: RoadmPort[];
}

/**
 * Model of the roadm ports returned from the WebSocket
 */
interface RoadmPort {
    id: string;
    reversePort: string;
    name: string;
    type: string;
    enabled: string;
    minFreq: string;
    maxFreq: string;
    grid: string;
    currFreq: string;
    powerRange: string;
    currentPower: string;
    targetPower: string;
    modulation: string;
    hasTargetPower: string;
    serviceState: string;
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
    selector: 'roadm-port',
    templateUrl: './port.component.html',
    styleUrls: ['./port.component.css',
        '../../../../../../web/gui2-fw-lib/lib/widget/table.theme.css',
        '../../../../../../web/gui2-fw-lib/lib/widget/table.css',
    ]
})
export class RoadmPortComponent extends TableBaseImpl implements OnInit, OnDestroy {
    devId: string;
    nzFilter: boolean = true;
    showDelta: boolean = false;
    prefsState = {};
    toggleState: FilterToggleState;

    powerForm: FormGroup;
    modulationForm: FormGroup;
    freqForm: FormGroup;
    SET_POWER_REQ = 'roadmSetTargetPowerRequest';
    SET_POWER_RESP = 'roadmSetTargetPowerResponse';
    SET_MODULATION = 'roadmSetModulationRequest';
    SET_FREQUENCY = 'roadmSetFrequencyRequest';

    restorePrefsConfig; // Function

    deviceTip = 'Show device table';

    constructor(protected fs: FnService,
                protected log: LogService,
                protected ar: ActivatedRoute,
                protected wss: WebSocketService,
                protected prefs: PrefsService,
                protected is: IconService,
    ) {
        super(fs, log, wss, 'roadmPort');
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
            secondCol: 'type',
            secondDir: SortDir.asc,
        };

        this.is.loadIconDef('switch');
    }

    ngOnInit() {
        this.init();
        this.createForm();
        this.wss.bindHandlers(new Map<string, (data) => void>([
            [this.SET_POWER_RESP, (data) => this.powerConfigCb(data)]
        ]));
        this.log.debug('RoadmPortComponent initialized');
    }

    createForm() {
        this.powerForm = new FormGroup({
            newPower: new FormControl(''),
        });
        this.modulationForm = new FormGroup({
            newModulation: new FormControl(''),
        });
        this.freqForm = new FormGroup({
            newFreq: new FormControl(''),
        });
        this.log.debug('Create Forms');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('RoadmPortComponent destroyed');
    }

    portResponseCb(data: RoadmPortTableResponse) {
        this.log.debug('Roadm Port response received for ', data.roadmPorts.length, 'port');
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

    submitPower(devId, port) {
        this.log.debug('Set power of port ', port, 'in device ', devId, 'as value ', this.powerForm.value['newPower'], 'dBm.');
        this.wss.sendEvent(this.SET_POWER_REQ, {
            'targetPower': this.powerForm.value['newPower'],
            'devId': devId,
            'id': port,
        });
    }

    submitModulation(devId, port) {
        this.log.debug('Set Modulation of port ', port, 'in device ', devId, 'as value ', this.modulationForm.value['newModulation']);
        this.wss.sendEvent(this.SET_MODULATION, {
            'modulation': this.modulationForm.value['newModulation'],
            'devId': devId,
            'id': port,
        });
    }

    submitFrequency(devId, port) {
        this.log.debug('Set Frequency of port ', port, 'in device ', devId, 'as value ', this.freqForm.value['newFreq']);
        this.wss.sendEvent(this.SET_FREQUENCY, {
            'currFreq': this.freqForm.value['newFreq'],
            'devId': devId,
            'id': port,
        });
    }

    powerConfigCb(data) {
        if (!data.valid) {
            const info = 'The power config operation is failed. The reason is: \n' + data.message;
            alert(info);
        } else {
            this.log.debug('The power config operation is successful!');
        }
    }

    convertNumber(str: string): number {
        return Number(str);
    }
}
