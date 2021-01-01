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
import { FnService } from '../util/fn.service';
import { LogService } from '../log.service';
import { WebSocketService } from '../remote/websocket.service';

import { PanelBaseImpl } from './panel.base';
import { InjectionToken, Inject, Component, Output, EventEmitter, Input } from '@angular/core';

/**
 * A generic model of the data returned from the *DetailsResponse
 */
export interface DetailsResponse {
    details: any;
}

export const TAG = new InjectionToken<string>('tag');

/**
 * Extends the PanelBase abstract class specifically for showing details
 *
 * This makes another call through WSS to the server for specific
 * details to fill the panel with
 *
 * This replaces the detailspanel service in the old gui
 */
@Component({
    template: ''
})
export abstract class DetailsPanelBaseImpl extends PanelBaseImpl {

    @Input() id: string;
    @Output() closeEvent = new EventEmitter<string>();

    private root: string;
    private req: string;
    private resp: string;
    private handlers: string[] = [];
    public detailsData: any = {};
    public closed: boolean = false;

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService,
        @Inject(TAG) protected tag: string,
    ) {
        super(fs, log);
        this.root = tag + 's';
        this.req = tag + 'DetailsRequest';
        this.resp = tag + 'DetailsResponse';
    }

    /**
     * When the details panel is created set up a listener on
     * Web Socket for details responses
     */
    init() {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            [this.resp, (data) => this.detailsPanelResponseCb(data)]
        ]));
        this.handlers.push(this.resp);
    }

    /**
     * When the details panel is destroyed this should be called to
     * de-register from the WebSocket
     */
    destroy() {
        this.wss.unbindHandlers(this.handlers);
    }

    /**
     * A callback that executes when the details data that was requested
     * on WebSocketService arrives.
     */
    detailsPanelResponseCb(data: DetailsResponse) {
        this.detailsData = data['details'];
    }

    /**
     * Details Panel Data Request - should be called whenever row id changes
     */
    requestDetailsPanelData(query: any) {
        this.closed = false;
        // Do not send if the Web Socket hasn't opened
        if (this.wss.isConnected()) {
            if (this.fs.debugOn('panel')) {
                this.log.debug('Details panel data REQUEST:', this.req, query);
            }
            this.wss.sendEvent(this.req, query);
        }
    }

    /**
     * this should be called when the details panel close button is clicked
     */
    close(): void {
        this.closed = true;
        this.id = null;
        this.closeEvent.emit(this.id);
    }

}
