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
import { TableBaseImpl, SortDir } from '../../../fw/widget/table.base';
import { WebSocketService } from '../../../fw/remote/websocket.service';
import { LogService } from '../../../log.service';
import { LoadingService } from '../../../fw/layer/loading.service';
import { FnService } from '../../../fw/util/fn.service';
import { ActivatedRoute } from '@angular/router';

/**
* ONOS GUI -- Partition View Component extends TableBaseImpl
*/
@Component({
    selector: 'onos-partition',
    templateUrl: './partition.component.html',
    styleUrls: ['./partition.component.css', '../../../fw/widget/table.css', '../../../fw/widget/table.theme.css']
})
export class PartitionComponent extends TableBaseImpl implements OnInit, OnDestroy {

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected ls: LoadingService,
        protected as: ActivatedRoute,
        protected wss: WebSocketService,
    ) {
        super(fs, ls, log, wss, 'partition');
        this.sortParams = {
            firstCol: 'name',
            firstDir: SortDir.desc,
            secondCol: 'term',
            secondDir: SortDir.asc,
        };
    }

    ngOnInit() {
        this.init();
        this.log.debug('PartitionComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('PartitionComponent destroyed');
    }

}
