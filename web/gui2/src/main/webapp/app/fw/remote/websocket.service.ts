/*
 * Copyright 2015-present Open Networking Foundation
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
import { Injectable } from '@angular/core';
import { FnService } from '../util/fn.service';
import { GlyphService } from '../svg/glyph.service';
import { LogService } from '../../log.service';
import { UrlFnService } from './urlfn.service';
import { WSock } from './wsock.service';

/**
 * Event Type structure for the WebSocketService
 */
interface EventType {
    event: string;
    payload: any;
}

/**
 * ONOS GUI -- Remote -- Web Socket Service
 */
@Injectable()
export class WebSocketService {
    // internal state
    private webSockOpts; // web socket options
    private ws = null; // web socket reference
    private wsUp = false; // web socket is good to go
    private handlers = {}; // event handler bindings
    private pendingEvents: EventType[] = []; // events TX'd while socket not up
    private host: string; // web socket host
    private url; // web socket URL
    private clusterNodes = []; // ONOS instances data for failover
    private clusterIndex = -1; // the instance to which we are connected
    private glyphs = [];
    private connectRetries = 0; // limit our attempts at reconnecting
    private openListeners = {}; // registered listeners for websocket open()
    private nextListenerId = 1; // internal ID for open listeners
    private loggedInUser = null; // name of logged-in user


    constructor(
        private fs: FnService,
        private gs: GlyphService,
        private log: LogService,
        private ufs: UrlFnService,
        private wsock: WSock,
        private window: Window
    ) {
        this.log.debug(window.location.hostname);
        this.log.debug('WebSocketService constructed');
    }

    /* ===================
     * === API Functions
     *
     * Required for unit tests to set to known state
     */
    resetState() {
        this.webSockOpts = undefined;
        this.ws = null;
        this.wsUp = false;
        this.host = undefined;
        this.url = undefined;
        this.pendingEvents = [];
        this.handlers = {};
        this.clusterNodes = [];
        this.clusterIndex = -1;
        this.glyphs = [];
        this.connectRetries = 0;
        this.openListeners = {};
        this.nextListenerId = 1;

    }

    /* Currently supported opts:
     *  wsport: web socket port (other than default 8181)
     *  host:   if defined, is the host address to use
     */
    createWebSocket(opts, _host_: string = '') {
        let wsport = (opts && opts.wsport) || null;

        this.webSockOpts = opts; // preserved for future calls

//        this.host = _host_ || this.host();
        let url = this.ufs.wsUrl('core', wsport, _host_);

        this.log.debug('Attempting to open websocket to: ' + url);
        this.ws = this.wsock.newWebSocket(url);
        if (this.ws) {
            this.ws.onopen = this.handleOpen();
            this.ws.onmessage = this.handleMessage('???');
            this.ws.onclose = this.handleClose();

//            sendEvent('authentication', { token: onosAuth });
            this.sendEvent('authentication token', '');
        }
        // Note: Wsock logs an error if the new WebSocket call fails
        return url;
    }

    handleOpen() {
        this.log.debug('WebSocketService: handleOpen() not yet implemented');
    }

    handleMessage(msgEvent: any) {
        this.log.debug('WebSocketService: handleMessage() not yet implemented');
    }

    handleClose() {
        this.log.debug('WebSocketService: handleClose() not yet implemented');
    }

    /* Formulates an event message and sends it via the web-socket.
     * If the websocket is not up yet, we store it in a pending list.
     */
    sendEvent(evType, payload) {
        let ev = <EventType> {
            event: evType,
            payload: payload
        }

        if (this.wsUp) {
            this._send(ev);
        } else {
            this.pendingEvents.push(ev);
        }
    }

    _send(ev: EventType) {
        if (this.fs.debugOn('txrx')) {
            this.log.debug(' *Tx* >> ', ev.event, ev.payload);
        }
        this.ws.send(JSON.stringify(ev));
    }

}
