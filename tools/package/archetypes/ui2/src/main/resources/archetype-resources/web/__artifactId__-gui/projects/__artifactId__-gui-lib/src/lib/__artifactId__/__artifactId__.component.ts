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
import { Component, OnDestroy, OnInit } from '@angular/core';
import {LogService, WebSocketService} from 'gui2-fw-lib';

const SAMPLE_CUSTOM_DATA_REQ = '${appNameAllLower}DataRequest';
const SAMPLE_CUSTOM_DATA_RESP = '${appNameAllLower}DataResponse';

/**
 * Model of the data sent in ${appNameAllLower}DataResponse from ${appNameTitle}DataRequestHandler
 */
export interface ${appNameTitle}Data {
    number: number;
    square: number;
    cube: number;
    message: string;
}

export interface ${appNameTitle}Req {
    reqnumber: number;
}

@Component({
    selector: '${artifactId}-app-sample',
    templateUrl: './${artifactId}.component.html',
    styleUrls: ['./${artifactId}.component.css']
})
export class ${appNameTitle}Component implements OnInit, OnDestroy {
    private openListener: any;

    socketData: ${appNameTitle}Data = <${appNameTitle}Data>{
        number: 0,
        square: 0,
        cube: 0,
        message: undefined
    };
    requestNumber: number = 0;
    childSelected: string = '(none)';

    constructor(
        protected log: LogService,
        protected wss: WebSocketService
    ) {
        this.log.debug('${appNameTitle}Component constructed');
    }

    ngOnInit() {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            [SAMPLE_CUSTOM_DATA_RESP, (data) => {
                this.socketData = <${appNameTitle}Data>data;
                this.log.debug(SAMPLE_CUSTOM_DATA_RESP, this.socketData);
            }
            ]
        ]));

        // in case we fail over to a new server,
        // listen for wsock-open events
        this.openListener = this.wss.addOpenListener((h, u) => this.wsOpen(h, u));
    }

    ngOnDestroy(): void {
        this.wss.unbindHandlers([SAMPLE_CUSTOM_DATA_RESP]);
    }

    wsOpen(host: string, url: string) {
        this.log.debug(SAMPLE_CUSTOM_DATA_RESP, ': WSopen - cluster node:', host, 'URL:', url);
        // tell the server we are ready to receive topo events
        const requestObj = <${appNameTitle}Req>{
            reqnumber: this.requestNumber
        };
        this.wss.sendEvent(SAMPLE_CUSTOM_DATA_REQ, requestObj);
    }

    // When the FetchData button is clicked
    getData() {
        this.requestNumber++;
        const requestObj = <${appNameTitle}Req>{
            reqnumber: this.requestNumber
        };
        this.wss.sendEvent(SAMPLE_CUSTOM_DATA_REQ, requestObj);
        this.log.debug('Getting data', this.requestNumber);
    }

    // when we recieve an event from the child
    childClicked(colour: string) {
        this.log.debug('Received event', colour);
        this.childSelected = colour;
    }
}
