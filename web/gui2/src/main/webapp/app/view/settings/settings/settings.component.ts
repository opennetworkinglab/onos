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

/**
 * Model of the data returned through the Web Socket about settings.
 */
interface SettingsTableResponse extends TableResponse {
    settings: Settings[];
}

/**
 * Model of the data returned through Web Socket for a single settings
 */
export interface Settings {
    fqComponent: string;
    component: string;
    prop: string;
    type: string;
    value: number;
    defValue: number;
    desc: string;
}

/**
 * ONOS GUI -- Settings View Component
 */
@Component({
    selector: 'onos-settings',
    templateUrl: './settings.component.html',
    styleUrls: ['./settings.component.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})

export class SettingsComponent extends TableBaseImpl implements OnInit, OnDestroy {

    settingsDetails: Settings;

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService
    ) {
        super(fs, log, wss, 'setting');
        this.responseCallback = this.settingsResponseCb;
        this.parentSelCb = this.rowSelection;

        this.sortParams = {
            firstCol: 'component',
            firstDir: SortDir.desc,
            secondCol: 'prop',
            secondDir: SortDir.asc,
        };
    }

    ngOnInit() {
        this.init();
        this.log.debug('SettingsComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('SettingsComponent destroyed');
    }

    settingsResponseCb(data: SettingsTableResponse) {
        this.log.debug('Settings response received for ', data.settings.length, 'settings');
    }

    rowSelection(event: any, row: any) {
        this.settingsDetails = row;
    }
}
