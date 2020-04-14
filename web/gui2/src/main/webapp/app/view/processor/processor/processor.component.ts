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
    WebSocketService,
    TableBaseImpl
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { ActivatedRoute } from '@angular/router';

/**
* ONOS GUI -- Processor View Component extends TableBaseImpl
*/
@Component({
    selector: 'onos-processor',
    templateUrl: './processor.component.html',
    styleUrls: ['./processor.component.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})
export class ProcessorComponent extends TableBaseImpl implements OnInit, OnDestroy {

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected as: ActivatedRoute,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'processor');
    }

    ngOnInit() {
        this.init();
        this.log.debug('ProcessorComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('ProcessorComponent destroyed');
    }
}
