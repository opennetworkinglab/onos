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
import { Component, Input, Inject, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {
    FnService,
    LogService,
    WebSocketService,
    SortDir, TableBaseImpl, TableResponse
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * Model of the response from the WebSocket
 */
export interface AlarmTableResponse extends TableResponse {
    alarmTables: Alarm[];
}

/**
 * Model of the alarms returned from the WebSocket
 */
export interface Alarm {
    id: string;
    alarmDesc: string;
    alarmDeviceId: string;
    alarmSource: string;
    alarmTimeRaised: string;
    alarmTimeUpdated: string;
    alarmTimeCleared: string;
    alarmSeverity: string;
}

export enum AlarmAction {
    NONE = 0,
    ACKNOWLEDGE = 1,
    CLEAR = 2,
}

/**
 * Model of the Control Button
 */
export interface CtrlBtnState {
    acknowledged: boolean;
    cleared: boolean;
    selection: string;
}

const ACKNOWLEDGED = 'ACKNOWLEDGED';
const CLEARED = 'CLEARED';

/**
* ONOS GUI -- Alarm Table Component extends TableBaseImpl
*/
@Component({
    selector: 'onos-alarmtable',
    templateUrl: './alarmtable.component.html',
    styleUrls: ['./alarmtable.component.css',
      '../../../../../web/gui2-fw-lib/lib/widget//table.css',
      '../../../../../web/gui2-fw-lib/lib/widget//table.theme.css'
    ]
})
export class AlarmTableComponent extends TableBaseImpl implements OnInit, OnDestroy {
    devId: string;
    selRowAlarmId: string;
    // TODO Add in LION translations
    acknowledgeTip: string = 'Acknowledge';
    clearTip: string = 'Clear';
    AlarmActionEnum: any = AlarmAction;
    alarmAction: AlarmAction = AlarmAction.NONE;
    confirmMsg: string = '';
    ctrlBtnState: CtrlBtnState;

    constructor(
        private route: ActivatedRoute,
        @Inject('Window') private w: any,
        protected log: LogService,
        protected fs: FnService,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'alarmTable');

        this.route.queryParams.subscribe(params => {
            this.devId = params['devId'];

        });

        this.payloadParams = {
            devId: this.devId
        };

        this.responseCallback = this.alarmResponseCb;

        this.sortParams = {
            firstCol: 'alarmTimeRaised',
            firstDir: SortDir.desc,
            secondCol: 'alarmDeviceId',
            secondDir: SortDir.asc,
        };

        this.ctrlBtnState = <CtrlBtnState>{
            acknowledged: false,
            cleared: false
        };
        this.log.debug('AlarmTableComponent constructed');
    }

    ngOnInit() {
        this.init();
        this.log.debug('AlarmTableComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('AlarmTableComponent destroyed');
    }

    alarmResponseCb(data: AlarmTableResponse) {
        this.log.debug('Alarm response received for ', data.alarmTables.length, 'alarm');
    }

    rowSelection(event: any, selRow: any) {
        this.ctrlBtnState.acknowledged = this.selId && selRow && selRow.state === ACKNOWLEDGED;
        this.ctrlBtnState.cleared = this.selId && selRow && selRow.cleared === CLEARED;
        this.ctrlBtnState.selection = this.selId;
        this.selRowAlarmId = selRow.appId;
        this.log.debug('Row ', this.selId, 'selected', this.ctrlBtnState);
    }

    /**
     * Perform one of the alarm actions - acknowledge or clear
     * Raises a dialog which calls back the dOk() below
     */
    confirmAction(action: AlarmAction): void {
        this.alarmAction = action;
        const alarmActionLc = (<string>AlarmAction[this.alarmAction]).toLowerCase();

        this.confirmMsg = alarmActionLc + ' ' + this.selId + '?';

        this.log.debug('Initiating', this.alarmAction, 'of', this.selId);
    }

    /**
     * Callback when the Confirm dialog is shown and a choice is made
     */
    dOk(choice: boolean) {
        const alarmActionLc = (<string>AlarmAction[this.alarmAction]).toLowerCase();
        if (choice) {
            this.log.debug('Confirmed', alarmActionLc, 'on', this.selId);

            /** commented out until backend is implemented
            this.wss.sendEvent(APPMGMTREQ, {
                action: alarmActionLc,
                name: this.selId,
                sortCol: this.sortParams.firstCol,
                sortDir: SortDir[this.sortParams.firstDir],
            });

            this.wss.sendEvent(DETAILSREQ, { id: this.selId });
            */
        } else {
            this.log.debug('Cancelled', alarmActionLc, 'on', this.selId);
        }
        this.confirmMsg = '';
    }

    ackIcon(acknowledged: string): string {
        if (acknowledged === 'true') {
            return 'active';
        } else {
            return 'appInactive';
        }
    }
}
