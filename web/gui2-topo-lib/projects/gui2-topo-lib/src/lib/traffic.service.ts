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
import { Injectable } from '@angular/core';
import {LogService, WebSocketService} from 'gui2-fw-lib';
import {ForceSvgComponent} from './layer/forcesvg/forcesvg.component';

export enum TrafficType {
    IDLE,
    FLOWSTATSBYTES = 'flowStatsBytes',
    PORTSTATSBITSEC = 'portStatsBitSec',
    PORTSTATSPKTSEC = 'portStatsPktSec',
}

const ALL_TRAFFIC_TYPES = [
    TrafficType.FLOWSTATSBYTES,
    TrafficType.PORTSTATSBITSEC,
    TrafficType.PORTSTATSPKTSEC
];

const ALL_TRAFFIC_MSGS = [
    'Flow Stats (bytes)',
    'Port Stats (bits / second)',
    'Port Stats (packets / second)',
];

/**
 * ONOS GUI -- Traffic Service Module.
 */
@Injectable()
export class TrafficService {
    private handlers: string[] = [];
    private openListener: any;

    constructor(
        protected log: LogService,
        protected wss: WebSocketService
    ) {
        this.log.debug('TrafficService constructed');
    }

    init(force: ForceSvgComponent) {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            ['topo2Highlights', (data) => {
                  force.handleHighlights(data.devices, data.hosts, data.links, 5000);
                }
            ]
        ]));

        this.handlers.push('topo2Highlights');

        // in case we fail over to a new server,
        // listen for wsock-open events
        this.openListener = this.wss.addOpenListener(() => this.wsOpen);

        // tell the server we are ready to receive topology events
        this.wss.sendEvent('topo2RequestAllTraffic', {
            trafficType: TrafficType.FLOWSTATSBYTES
        });
        this.log.debug('Topo2Traffic: Show All Traffic');
    }

    destroy() {
        this.wss.sendEvent('topo2CancelTraffic', {});
        this.wss.unbindHandlers(this.handlers);
        this.handlers.pop();
        this.log.debug('Traffic monitoring canceled');
    }

    wsOpen(host: string, url: string) {
        this.log.debug('topo2RequestAllTraffic: WSopen - cluster node:', host, 'URL:', url);
        // tell the server we are ready to receive topo events
        this.wss.sendEvent('topo2RequestAllTraffic', {
            trafficType: TrafficType.FLOWSTATSBYTES
        });
    }
}
