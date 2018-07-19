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
import { LogService } from '../../../log.service';
import { FnService } from '../../../fw/util/fn.service';
import { LoadingService } from '../../../fw/layer/loading.service';
import { WebSocketService } from '../../../fw/remote/websocket.service';
import { ActivatedRoute } from '@angular/router';
import { TableResponse, TableBaseImpl, SortDir } from '../../../fw/widget/table.base';

/**
 * Model of the response from WebSocket
 */
interface GroupTableResponse extends TableResponse {
    groups: Group[];
}

/**
 * Model of the flows returned from the WebSocket
 */
interface Group {
    id: string;
    app_id: string;
    state: string;
    type: string;
    packets: string;
    bytes: string;
}

/**
 * ONOS GUI -- Group View Component
 */
@Component({
    selector: 'onos-group',
    templateUrl: './group.component.html',
    styleUrls: ['./group.component.css', './group.theme.css', '../../../fw/widget/table.css', '../../../fw/widget/table.theme.css']
})
export class GroupComponent extends TableBaseImpl implements OnInit, OnDestroy {
    id: string;
    brief: boolean;

    // TODO: Update for LION
    deviceTip = 'Show device table';
    detailTip = 'Switch to detailed view';
    briefTip = 'Switch to brief view';
    flowTip = 'Show flow view for selected device';
    portTip = 'Show port view for selected device';
    meterTip = 'Show meter view for selected device';
    pipeconfTip = 'Show pipeconf view for selected device';

    constructor(
        protected log: LogService,
        protected fs: FnService,
        protected ls: LoadingService,
        protected wss: WebSocketService,
        protected ar: ActivatedRoute,
    ) {
        super(fs, ls, log, wss, 'group');
        this.ar.queryParams.subscribe(params => {
            this.id = params['devId'];
        });
        this.brief = true;

        this.payloadParams = {
            devId: this.id
        };

        this.responseCallback = this.groupResponseCb;

        this.sortParams = {
            firstCol: 'id',
            firstDir: SortDir.desc,
            secondCol: 'app_id',
            secondDir: SortDir.asc,
        };
    }

    ngOnInit() {
        this.init();
        this.log.info('GroupComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.info('GroupComponent destroyed');
    }

    groupResponseCb(data: GroupTableResponse) {
        this.log.debug('Group response received for ', data.groups.length, 'group');
    }

    briefToggle() {
        this.brief = !this.brief;
    }

}
