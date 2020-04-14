/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
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
 * Model of the response from WebSocket
 */
interface LinkTableResponse extends TableResponse {
    links: Link[];
}

/**
 * Model of the links returned from the WebSocket
 */
interface Link {
    one: string;
    two: string;
    type: string;
    direction: string;
    durable: string;
    _iconid_state: string;
}

/**
 * ONOS GUI -- Link View Component
 */
@Component({
    selector: 'onos-link',
    templateUrl: './link.component.html',
    styleUrls: ['./link.component.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.css', '../../../../../../../../gui2-fw-lib/lib/widget/table.theme.css']
})
export class LinkComponent extends TableBaseImpl implements OnInit, OnDestroy {

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService,
    ) {
        super(fs, log, wss, 'link');
        this.responseCallback = this.linkResponseCb;
        this.sortParams = {
            firstCol: 'one',
            firstDir: SortDir.desc,
            secondCol: 'two',
            secondDir: SortDir.asc,
        };
    }

    ngOnInit() {
        this.init();
        this.log.debug('LinkComponent initialized');
    }

    ngOnDestroy() {
        this.destroy();
        this.log.debug('LinkComponent destroyed');
    }

    linkResponseCb(data: LinkTableResponse) {
        this.log.debug('Link response received for ', data.links.length, 'links');
    }
}
