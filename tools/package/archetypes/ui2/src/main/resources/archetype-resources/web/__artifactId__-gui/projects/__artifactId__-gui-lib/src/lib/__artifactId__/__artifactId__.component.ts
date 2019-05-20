/*
 * Copyright ${year}-present Open Networking Foundation
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
import { Component, OnInit } from '@angular/core';
import {LogService, WebSocketService} from 'gui2-fw-lib';

const SAMPLE_CUSTOM_DATA_REQ = 'sampleCustomDataRequest';
const SAMPLE_CUSTOM_DATA_RESP = 'sampleCustomDataResponse';

@Component({
    selector: '${artifactId}-app-sample',
    templateUrl: './${artifactId}.component.html',
    styleUrls: ['./${artifactId}.component.css']
})
export class ${appNameCap}${appNameEnd}Component implements OnInit {
    private handlers: string[] = [];
    private openListener: any;

    constructor(
        protected log: LogService,
        protected wss: WebSocketService
    ) {
        this.log.debug('${appNameCap}${appNameEnd}Component constructed');
    }

    ngOnInit() {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            [SAMPLE_CUSTOM_DATA_RESP, (data) => {
                this.log.debug(SAMPLE_CUSTOM_DATA_RESP, data)
            }
            ]
        ]));

        this.handlers.push(SAMPLE_CUSTOM_DATA_RESP);

        // in case we fail over to a new server,
        // listen for wsock-open events
        this.openListener = this.wss.addOpenListener(() => this.wsOpen);
    }

    wsOpen(host: string, url: string) {
        this.log.debug(SAMPLE_CUSTOM_DATA_RESP, ': WSopen - cluster node:', host, 'URL:', url);
        // tell the server we are ready to receive topo events
        this.wss.sendEvent(SAMPLE_CUSTOM_DATA_REQ, {});
    }
}
