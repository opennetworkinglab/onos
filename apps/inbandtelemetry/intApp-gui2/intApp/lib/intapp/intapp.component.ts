/*
 * Copyright 2020-present Open Networking Foundation
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
import {Component, OnInit, OnDestroy} from '@angular/core';
import { FormBuilder, FormGroup, FormArray, FormControl, Validators } from '@angular/forms';

import {
    FnService,
    LogService,
    WebSocketService,
    SortDir, TableBaseImpl, TableResponse,
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

import {ActivatedRoute, Router} from '@angular/router';

// constants
const intIntentAddReq = 'intIntentAddRequest';
const intIntentDelReq = 'intIntentDelRequest';
const regSendIp = '^([0-9]{1,3}\\.){3}[0-9]{1,3}(\\/[0-9]{1,2})?$'
const regSendPort ='^[0-9]{0,5}$'

export interface Metadata {
    name: string;
    value: string;
}

/**
 * ONOS GUI -- INT App View Component
 */
@Component({
    selector: 'int-app',
    templateUrl: './intapp.component.html',
    styleUrls: ['./intapp.component.css',
        '../../../../../../web/gui2-fw-lib/lib/widget/table.css',
        '../../../../../../web/gui2-fw-lib/lib/widget/table.theme.css'
    ]
})
export class IntAppComponent extends TableBaseImpl implements OnInit, OnDestroy {

    formConf: FormGroup;
    formSend: FormGroup;
    metaData: Metadata[] = [
        { name: 'Switch ID', value : 'SWITCH_ID'},
        { name: 'Port IDs', value:'PORT_ID' },
        { name: 'Hop Latency', value: 'HOP_LATENCY', },
        { name: 'Queue Occupancy', value:'QUEUE_OCCUPANCY' },
        { name: 'Ingress Timestamp', value:'INGRESS_TIMESTAMP' },
        { name: 'Egress Timestamp', value: 'EGRESS_TIMESTAMP' },
        { name: 'Egress Port Tx Utilization', value:'EGRESS_TX_UTIL' },
    ];

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected as: ActivatedRoute,
        protected router: Router,
        protected wss: WebSocketService,
        protected fb: FormBuilder,
    ) {
        super(fs, log, wss, 'intAppIntIntent');

    }

    ngOnInit() {
        this.init();
        this.formSend = this.fb.group({
            name: this.fb.array([]),
            ip4SrcPrefix: new FormControl(null, [Validators.required, Validators.pattern(regSendIp)]),
            ip4DstPrefix: new FormControl(null, [Validators.required, Validators.pattern(regSendIp)]),
            l4SrcPort: new FormControl(null, [ Validators.pattern(regSendPort)]),
            l4DstPort: new FormControl(null, [ Validators.pattern(regSendPort)]),
            protocol: new FormControl(),
            telemetryMode: new FormControl("POSTCARD")
        });
        this.log.debug('IntAppComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('IntAppComponent destroyed');
    }

    navto(path) {
        this.log.debug('navigate');
        if (this.selId) {
            this.router.navigate([path], {queryParams: {itemId: this.selId}});
        }
    }

    onCheckboxChange(name: string, isChecked: boolean) {
        this.log.debug('event'+ isChecked);
        const meta = (this.formSend.controls.name as FormArray);

        if (isChecked) {
            meta.push(new FormControl(name));
        } else {
            const index = meta.controls.findIndex(x => x.value === name);
            meta.removeAt(index);
        }
    }

    sendIntIntentString() {
        if (this.formSend.invalid) {
            return;
        }
        let intentObjectNode = {
            "ip4SrcPrefix": this.formSend.value.ip4SrcPrefix,
            "ip4DstPrefix": this.formSend.value.ip4DstPrefix,
            "l4SrcPort": this.formSend.value.l4SrcPort,
            "l4DstPort": this.formSend.value.l4DstPort,
            "protocol": this.formSend.value.protocol,
            "metadata": this.formSend.value.name,
            "telemetryMode": this.formSend.value.telemetryMode
        };
        this.wss.sendEvent(intIntentAddReq, intentObjectNode);
    }

    delIntIntent(){
        if (this.selId) {
            this.wss.sendEvent(intIntentDelReq,{"intentId": this.selId});
        }
    }
}
