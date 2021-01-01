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
import { Injectable, Inject } from '@angular/core';
import { FnService } from '../util/fn.service';
import { GlyphService } from '../svg/glyph.service';
import { LogService } from '../log.service';
import { UrlFnService } from './urlfn.service';
import { VeilComponent } from '../layer/veil/veil.component';
import { WSock } from './wsock.service';

/**
 * Event Type structure for the WebSocketService
 */
export interface EventType {
    event: string;
    payload: Object;
}

export interface Callback {
    id: number;
    error: string;
    cb(host: string, url: string): void;
}

interface ClusterNode {
    id: string;
    ip: string;
    m_uiAttached: boolean;
}

interface Glyph {
    id: string;
    viewbox: string;
    path: string;
}

interface Bootstrap {
    user: string;
    clusterNodes: ClusterNode[];
    glyphs: Glyph[];
}

interface ErrorData {
    message: string;
}

export interface WsOptions {
    wsport: number;
}

/**
 * ONOS GUI -- Remote -- Web Socket Service
 *
 * To see debug messages add ?debug=txrx to the URL
 */
@Injectable({
  providedIn: 'root',
})
export class WebSocketService {
    // internal state
    private webSockOpts: WsOptions; // web socket options
    private ws: WebSocket = null; // web socket reference
    private wsUp: boolean = false; // web socket is good to go

    // A map of event handler bindings - names and functions (that accept data and return void)
    private handlers = new Map<string, (data: any) => void>([]);
    private pendingEvents: EventType[] = []; // events TX'd while socket not up
    private host: string; // web socket host
    private url; // web socket URL
    private clusterNodes: ClusterNode[] = []; // ONOS instances data for failover
    private clusterIndex = -1; // the instance to which we are connected
    private glyphs: Glyph[] = [];
    private connectRetries: number = 0; // limit our attempts at reconnecting

    // A map of registered Callbacks for websocket open()
    private openListeners = new Map<number, Callback>([]);
    private nextListenerId: number = 1; // internal ID for open listeners
    private loggedInUser = null; // name of logged-in user
    private lcd: any; // The loading component delegate
    private vcd: any; // The veil component delegate

    /**
     * built-in handler for the 'boostrap' event
     */
    private bootstrap(data: Bootstrap) {
        this.loggedInUser = data.user;
        this.log.info('Websocket connection bootstraped', data);

        this.clusterNodes = data.clusterNodes;
        this.clusterNodes.forEach((d, i) => {
            if (d.m_uiAttached) {
                this.clusterIndex = i;
                this.log.info('Connected to cluster node ' + d.ip);
                // TODO: add connect info to masthead somewhere
            }
        });
        this.glyphs = data.glyphs;
        const glyphsMap = new Map<string, string>([]);
        this.glyphs.forEach((d) => {
            glyphsMap.set('_' + d.id, d.viewbox);
            glyphsMap.set(d.id, d.path);
            this.gs.registerGlyphs(glyphsMap);
        });
    }

    private error(data: ErrorData) {
        const m: string = data.message || 'error from server';
        this.log.error(m, data);

        // Unrecoverable error - throw up the veil...
        if (this.vcd) {
            this.vcd.show([
                'Oops!',
                'Server reports error...',
                m,
            ]);
        }
    }

    constructor(
        private fs: FnService,
        private gs: GlyphService,
        private log: LogService,
        private ufs: UrlFnService,
        private wsock: WSock,
        @Inject('Window') private window: any
    ) {
        this.log.debug(window.location.hostname);

        // Bind the boot strap event by default
        this.bindHandlers(new Map<string, (data) => void>([
            ['bootstrap', (data) => this.bootstrap(data)],
            ['error', (data) => this.error(data)]
        ]));

        this.log.debug('WebSocketService constructed');
    }


    // ==========================
    // === Web socket callbacks

    /**
     * Called when WebSocket has just opened
     *
     * Lift the Veil if it is displayed
     * If there are any events pending, send them
     * Mark the WSS as up and inform any listeners for this open event
     */
    handleOpen(): void {
        this.log.info('Web socket open - ', this.url);
        // Hide the veil
        if (this.vcd) {
            this.vcd.hide();
        }

        if (this.fs.debugOn('txrx')) {
            this.log.debug('Sending ' + this.pendingEvents.length + ' pending event(s)...');
        }
        this.pendingEvents.forEach((ev) => {
            this.send(ev);
        });
        this.pendingEvents = [];

        this.connectRetries = 0;
        this.wsUp = true;
        this.informListeners(this.host, this.url);
    }

    /**
     * Function called when WebSocket send a message
     */
    handleMessage(msgEvent: MessageEvent): void {
        let ev: EventType;
        let h;
        try {
            ev = JSON.parse(msgEvent.data.toString()) as EventType;
        } catch (e) {
            this.log.error('Message.data is not valid JSON', msgEvent.data, e);
            return null;
        }
        if (this.fs.debugOn('txrx')) {
            this.log.debug(' << *Rx* ', ev.event, ev.payload);
        }
        h = this.handlers.get(ev.event);
        if (h) {
            try {
                h(ev.payload);
            } catch (e) {
                this.log.error('Problem handling event:', ev, e);
                return null;
            }
        } else {
            this.log.warn('Unhandled event:', ev);
        }
    }

    /**
     * Called by the WebSocket if it is closed from the server end
     *
     * If the loading component is shown, call stop() on it
     * Try to find another node in the cluster to connect to
     * If this is not possible then show the Veil Component
     */
    handleClose(): void {
        this.log.warn('Web socket closed');
        if (this.lcd) {
            this.lcd.stop();
        }
        this.wsUp = false;
        let gsucc;

        if (gsucc = this.findGuiSuccessor()) {
            this.url = this.createWebSocket(this.webSockOpts, gsucc);
        } else {
            // If no controllers left to contact, show the Veil...
            if (this.vcd) {
                this.vcd.show([
                    'Oops!',  // TODO: Localize this
                    'Web-socket connection to server closed...',
                    'Try refreshing the page.',
                ]);
            }
        }
    }

    // ==============================
    // === Private Helper Functions

    /**
     * Find the next node in the ONOS cluster.
     *
     * This is used if the WebSocket connection closes because a
     * node in the cluster ges down - fail over should be automatic
     */
    findGuiSuccessor(): string {
        const ncn = this.clusterNodes.length;
        let ip: string;
        let node;

        while (this.connectRetries < ncn && !ip) {
            this.connectRetries++;
            this.clusterIndex = (this.clusterIndex + 1) % ncn;
            node = this.clusterNodes[this.clusterIndex];
            ip = node && node.ip;
        }

        return ip;
    }

    /**
     * When the WebSocket is opened, inform any listeners that registered
     * for that event
     */
    informListeners(host: string, url: string): void {
        for (const [key, cb] of this.openListeners.entries()) {
            cb.cb(host, url);
        }
    }

    send(ev: EventType): void {
        if (this.fs.debugOn('txrx')) {
            this.log.debug(' *Tx* >> ', ev.event, ev.payload);
        }
        this.ws.send(JSON.stringify(ev));
    }

    /**
     * Check if there are no WSS event handlers left
     */
    noHandlersWarn(handlers: Map<string, Object>, caller: string): boolean {
        if (!handlers || handlers.size === 0) {
            this.log.warn('WSS.' + caller + '(): no event handlers');
            return true;
        }
        return false;
    }

    /* ===================
     * === API Functions
     */

    /**
     * Required for unit tests to set to known state
     */
    resetState(): void {
        this.webSockOpts = undefined;
        this.ws = null;
        this.wsUp = false;
        this.host = undefined;
        this.url = undefined;
        this.pendingEvents = [];
        this.handlers.clear();
        this.clusterNodes = [];
        this.clusterIndex = -1;
        this.glyphs = [];
        this.connectRetries = 0;
        this.openListeners.clear();
        this.nextListenerId = 1;

    }

    /*
     * Currently supported opts:
     *  wsport: web socket port (other than default 8181)
     *  host:   if defined, is the host address to use
     */
    createWebSocket(opts?: WsOptions, host?: string) {
        this.webSockOpts = opts; // preserved for future calls
        this.host = host === undefined ? this.window.location.host : host;
        this.url = this.ufs.wsUrl('core', opts === undefined ? '' : opts['wsport'].toString(), host);

        this.log.debug('Attempting to open websocket to: ' + this.url);
        this.ws = this.wsock.newWebSocket(this.url);
        if (this.ws) {
            // fat arrow => syntax means that the 'this' context passed will
            // be of WebSocketService, not the WebSocket
            this.ws.onopen = (() => this.handleOpen());
            this.ws.onmessage = ((msgEvent) => this.handleMessage(msgEvent));
            this.ws.onclose = (() => this.handleClose());
            const authToken = this.window['onosAuth'];
            this.log.debug('Auth Token for opening WebSocket', authToken);
            this.sendEvent('authentication', { token: authToken });
        }
        // Note: Wsock logs an error if the new WebSocket call fails
        return this.url;
    }

    /**
     * Tell the WebSocket to close - this should call the handleClose() method
     */
    closeWebSocket() {
        this.ws.close();
    }


    /**
     * Binds the message handlers to their message type (event type) as
     *  specified in the given map. Note that keys are the event IDs; values
     *  are either:
     *     * the event handler function, or
     *     * an API object which has an event handler for the key
     */
    bindHandlers(handlerMap: Map<string, (data) => void>): void {
        const dups: string[] = [];

        if (this.noHandlersWarn(handlerMap, 'bindHandlers')) {
            return null;
        }
        for (const [eventId, api] of handlerMap) {
            this.log.debug('Adding handler for ', eventId);
            const fn = this.fs.isF(api) || this.fs.isF(api[eventId]);
            if (!fn) {
                this.log.warn(eventId + ' handler not a function');
                return;
            }

            if (this.handlers.get(eventId)) {
                dups.push(eventId);
            } else {
                this.handlers.set(eventId, fn);
            }
        }
        if (dups.length) {
            this.log.warn('duplicate bindings ignored:', dups);
        }
    }

    /**
     * Unbinds the specified message handlers.
     *   Expected that the same map will be used, but we only care about keys
     */
    unbindHandlers(handlerIds: string[]): void {
        if ( handlerIds.length === 0 ) {
            this.log.warn('WSS.unbindHandlers(): no event handlers');
            return null;
        }
        for (const eventId of handlerIds) {
            this.handlers.delete(eventId);
        }
    }

    isHandling(handlerId: string): boolean {
        return this.handlers.get(handlerId) !== undefined;
    }

    /**
     * Add a listener function for listening for WebSocket opening.
     * The function must give a host and url and return void
     */
    addOpenListener(callback: (host: string, url: string) => void ): Callback {
        const id: number = this.nextListenerId++;
        const cb = this.fs.isF(callback);
        const o: Callback = <Callback>{ id: id, cb: cb };

        if (cb) {
            this.openListeners.set(id, o);
        } else {
            this.log.error('WSS.addOpenListener(): callback not a function');
            o.error = 'No callback defined';
        }
        return o;
    }

    /**
     * Remove a listener of WebSocket opening
     */
    removeOpenListener(lsnr: Callback): void {
        const id = this.fs.isO(lsnr) && lsnr.id;
        let o;

        if (!id) {
            this.log.warn('WSS.removeOpenListener(): invalid listener', lsnr);
            return null;
        }
        o = this.openListeners[id];

        if (o) {
            this.openListeners.delete(id);
        }
    }

    /**
     * Formulates an event message and sends it via the web-socket.
     * If the websocket is not up yet, we store it in a pending list.
     */
    sendEvent(evType: string, payload: Object ): void {
        const ev = <EventType> {
            event: evType,
            payload: payload
        };

        if (this.wsUp) {
            this.send(ev);
        } else {
            this.pendingEvents.push(ev);
        }
    }

    /**
     * Binds the veil service as a delegate.
     */
    setVeilDelegate(vd: VeilComponent): void {
        this.vcd = vd;
    }

    /**
     * Binds the loading service as a delegate
     */
    setLoadingDelegate(ld: any): void {
        // TODO - Investigate changing Loading Service to LoadingComponent
        this.log.debug('Loading delegate set', ld);
        this.lcd = ld;
    }

    isConnected(): boolean {
        return this.wsUp;
    }
}
