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
import { SortDir, TableBaseImpl, TableResponse } from '../../../fw/widget/table.base';
import { WebSocketService } from '../../../fw/remote/websocket.service';
import { LogService } from '../../../log.service';
import { LoadingService } from '../../../fw/layer/loading.service';
import { FnService } from '../../../fw/util/fn.service';
import { ActivatedRoute } from '@angular/router';
import { LionService } from '../../../fw/util/lion.service';


/**
 * Model of the response from WebSocket
 */
interface FlowTableResponse extends TableResponse {
    flows: Flow[];
}

/**
 * Model of the flows returned from the WebSocket
 */
interface Flow {
    state: string;
    packets: string;
    duration: string;
    priority: string;
    tableName: string;
    selector: string;
    treatment: string;
    appName: string;
}

/**
 * ONOS GUI -- Flow View Component
 */
@Component({
    selector: 'onos-flow',
    templateUrl: './flow.component.html',
    styleUrls: ['./flow.component.css', './flow.theme.css', '../../../fw/widget/table.css', '../../../fw/widget/table.theme.css']
})
export class FlowComponent extends TableBaseImpl implements OnInit, OnDestroy {

    lionFn; // Function
    id: string;
    brief: boolean;
    selRowAppId: string;

    deviceTip: string;
    detailTip: string;
    briefTip: string;
    portTip: string;
    groupTip: string;
    meterTip: string;
    pipeconfTip: string;

    constructor(protected fs: FnService,
        protected ls: LoadingService,
        protected log: LogService,
        protected as: ActivatedRoute,
        protected wss: WebSocketService,
        protected lion: LionService,
    ) {
        super(fs, ls, log, wss, 'flow');
        this.as.queryParams.subscribe(params => {
            this.id = params['devId'];

        });
        this.brief = true;

        this.payloadParams = {
            devId: this.id
        };

        this.responseCallback = this.flowResponseCb;

        this.sortParams = {
            firstCol: 'state',
            firstDir: SortDir.desc,
            secondCol: 'packets',
            secondDir: SortDir.asc,
        };

        // We want doLion() to be called only after the Lion
        // service is populated (from the WebSocket)
        // If lion is not ready we make do with a dummy function
        // As soon a lion gets loaded this function will be replaced with
        // the real thing
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('flows', () => this.doLion());
        } else {
            this.doLion();
        }

        this.parentSelCb = this.rowSelection;
    }

    ngOnInit() {
        this.init();
        this.log.debug('FlowComponent initialized');
    }

    ngOnDestroy() {
        this.lion.loadCbs.delete('flows');
        this.destroy();
        this.log.debug('FlowComponent destroyed');
    }

    flowResponseCb(data: FlowTableResponse) {
        this.log.debug('Flow response received for ', data.flows.length, 'flow');
    }

    briefToggle() {
        this.brief = !this.brief;
    }

    /**
     * Read the LION bundle for App and set up the lionFn
     */
    doLion() {
        this.lionFn = this.lion.bundle('core.view.Flow');

        this.deviceTip = this.lionFn('tt_ctl_show_device');
        this.detailTip = this.lionFn('tt_ctl_switcth_detailed');
        this.briefTip = this.lionFn('tt_ctl_switcth_brief');
        this.portTip = this.lionFn('tt_ctl_show_port');
        this.groupTip = this.lionFn('tt_ctl_show_group');
        this.meterTip = this.lionFn('tt_ctl_show_meter');
        this.pipeconfTip = this.lionFn('tt_ctl_show_pipeconf');
    }

    rowSelection(event: any, selRow: any) {
        this.selRowAppId = selRow.appId;
    }

}
