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
import {LogService, WebSocketService} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {ForceSvgComponent} from './layer/forcesvg/forcesvg.component';

export namespace TrafficType {
    /**
     * Toggle state for how the traffic should be displayed
     */
    export enum Enum { // Do not add an alias - they need to be number indexed
        FLOWSTATSBYTES, // 0 flowStatsBytes
        PORTSTATSBITSEC, // 1 portStatsBitSec
        PORTSTATSPKTSEC // 2 portStatsPktSec
    }

    /**
     * Add the method 'next()' to the TrafficType enum above
     */
    export function next(current: Enum) {
        if (current === Enum.FLOWSTATSBYTES) {
            return Enum.PORTSTATSBITSEC;
        } else if (current === Enum.PORTSTATSBITSEC) {
            return Enum.PORTSTATSPKTSEC;
        } else if (current === Enum.PORTSTATSPKTSEC) {
            return Enum.FLOWSTATSBYTES;
        } else { // e.g. undefined
            return Enum.PORTSTATSBITSEC;
        }
    }

    export function literal(type: Enum) {
        if (type === Enum.FLOWSTATSBYTES) {
            return 'flowStatsBytes';
        } else if (type === Enum.PORTSTATSBITSEC) {
            return 'portStatsBitSec';
        } else if (type === Enum.PORTSTATSPKTSEC) {
            return 'portStatsPktSec';
        }
    }
}

/**
 * ONOS GUI -- Traffic Service Module.
 */
@Injectable()
export class TrafficService {
    private handlers: string[] = [];
    private openListener: any;
    private trafficType: TrafficType.Enum;

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
    }

    destroy() {
        this.wss.unbindHandlers(this.handlers);
        this.handlers.pop();
    }

    wsOpen(host: string, url: string) {
        this.log.debug('topo2RequestAllTraffic: WSopen - cluster node:', host, 'URL:', url);
        // tell the server we are ready to receive topo events
        this.wss.sendEvent('topo2RequestAllTraffic', {
            trafficType: TrafficType.literal(this.trafficType)
        });
    }

    requestTraffic(trafficType: TrafficType.Enum) {
        // tell the server we are ready to receive topology events
        this.wss.sendEvent('topo2RequestAllTraffic', {
            trafficType: TrafficType.literal(trafficType)
        });
        this.log.debug('Topo2Traffic: Show', trafficType);
    }

    cancelTraffic() {
        this.wss.sendEvent('topo2CancelTraffic', {});
        this.log.debug('Traffic monitoring canceled');
    }
}
