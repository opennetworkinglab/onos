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

const RESUBMITINTENT = 'resubmitIntent';
const REMOVEINTENT = 'removeIntent';
const REMOVEINTENTS = 'removeIntents';
const INTENTRESUBMITTED = 'Intent resubmitted';
const INTENTWITHDRAWN = 'Intent withdrawn';
const INTENTPURGE = 'Intents purged';

/**
 * Model of the response from WebSocket
 */
interface IntentTableResponse extends TableResponse {
    intents: Intent[];
}

/**
 * Model of the Intents returned from the WebSocket
 */
interface Intent {
    appId: string;
    appName: string;
    key: string;
    type: string;
    priority: string;
    state: string;
    purge: boolean;
}

export enum IntentAction {
    NONE = 0,
    RESUBMIT = 1,
    WITHDRAWN = 2,
    PURGE = 3,
    PURGEWITHDRAWN = 4,
}

/**
* ONOS GUI -- Intents View Component extends TableBaseImpl
*/
@Component({
    selector: 'onos-intent',
    templateUrl: './intent.component.html',
    styleUrls: ['./intent.component.css', './intent-theme.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})
export class IntentComponent extends TableBaseImpl implements OnInit, OnDestroy {

    brief: boolean;
    withdrwanIntent: boolean;
    intentData: Intent;
    warnMsg: string = '';
    intentActionMsg: string;
    intentTableData: IntentTableResponse;
    IntentActionEnum: any = IntentAction;
    intentAction: IntentAction = IntentAction.NONE;

    topoTip = 'Show selected intent on topology view';
    resubmitTip = 'Resubmit selected intent';
    deactivateTip = 'Remove selected intent';
    purgeTip = 'Purge selected intent';
    purgeAllTip = 'Purge withdrawn intents';

    briefTip = 'Switch to brief view';
    detailTip = 'Switch to detailed view';

    constructor(
        protected log: LogService,
        protected fs: FnService,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'intent', 'key');
        this.responseCallback = this.intentResponseCb;
        this.parentSelCb = this.rowSelection;
        this.brief = true;
        this.sortParams = {
            firstCol: 'appId',
            firstDir: SortDir.desc,
            secondCol: 'key',
            secondDir: SortDir.asc,
        };
        this.intentData = <Intent>{};
    }

    ngOnInit() {
        this.init();
        this.log.info('IntentComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.info('IntentComponent distroyed');
    }

    intentResponseCb(data: IntentTableResponse) {
        this.withdrwanIntent = false;
        this.log.debug('Intent response received for ', data.intents.length, 'intent');
        this.intentTableData = data;
        this.isHavingWithdrawn();
    }

    /**
      * called when a row is selected - sets the state of control icons
      */
    rowSelection(event: any, selRow: any) {
        this.intentData.state = selRow.state;
        const selRowAppId = selRow.appId;
        const splittedRowAppId = selRowAppId.split(':');
        this.intentData.appId = splittedRowAppId[0].trim();
        this.intentData.appName = splittedRowAppId[1].trim();
        this.intentData.key = this.selId;
        this.intentData.type = selRow.type;
    }

    briefToggle() {
        this.brief = !this.brief;
    }

    intentState() {
        return this.intentData.state;
    }

    isHavingWithdrawn() {
        if (this.intentTableData !== undefined) {
            for (let i = 0; i < this.intentTableData.intents.length; i++) {
                if (this.intentTableData.intents[i].state === 'Withdrawn') {
                    this.withdrwanIntent = true;
                    return this.withdrwanIntent;
                }
            }

        }
        return this.withdrwanIntent;
    }

    /**
       * Perform one of the intent actions - resubmit, remove, purge or purge withdrawn
       * Raises a dialog which calls back the dOk() below
       */
    confirmAction(action: IntentAction): void {
        this.intentActionMsg = '';
        this.intentAction = action;
        const intentActionLc = (<string>IntentAction[this.intentAction]).toLowerCase();

        if (this.intentAction === IntentAction.PURGE) {
            this.intentData.purge = true;
        } else {
            this.intentData.purge = false;
        }

        if (this.intentAction === IntentAction.PURGEWITHDRAWN) {
            this.warnMsg = 'Are you sure you want to purge all the withdrawn intents?';
        } else {
            this.warnMsg = 'Are you sure you want to ' + intentActionLc + ' the selected intent?';
        }
        this.log.debug('Initiating', this.intentAction, 'of', this.selId);
    }

    /**
       * Callback when the Confirm dialog is shown and a choice is made
       */
    dOk(choice: boolean) {
        const intentActionLc = (<string>IntentAction[this.intentAction]).toLowerCase();
        if (choice) {

            if (this.intentAction === IntentAction.RESUBMIT) {
                this.wss.sendEvent(RESUBMITINTENT, {
                    appId: this.intentData.appId,
                    appName: this.intentData.appName,
                    key: this.intentData.key,
                    purge: this.intentData.purge,
                });
                this.selId = '';
                this.intentActionMsg = INTENTRESUBMITTED;
            } else if ((this.intentAction === IntentAction.PURGE) || (this.intentAction === IntentAction.WITHDRAWN)) {
                this.wss.sendEvent(REMOVEINTENT, {
                    appId: this.intentData.appId,
                    appName: this.intentData.appName,
                    key: this.intentData.key,
                    purge: this.intentData.purge,
                });
                this.selId = '';
                if (this.intentData.purge) {
                    this.intentActionMsg = INTENTPURGE;
                } else {
                    this.intentActionMsg = INTENTWITHDRAWN;
                }
            } else if (this.intentAction === IntentAction.PURGEWITHDRAWN) {
                this.wss.sendEvent(REMOVEINTENTS, {});
                this.intentActionMsg = INTENTPURGE;
            } else {
                this.log.debug('Some worng input provided');
            }

        } else {
            this.log.debug('Cancelled', intentActionLc, 'on', this.selId);
        }
        this.warnMsg = '';
    }
}
