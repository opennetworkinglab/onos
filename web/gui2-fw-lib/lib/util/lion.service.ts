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
 *
 */
import { Injectable } from '@angular/core';
import { LogService } from '../log.service';
import { WebSocketService } from '../remote/websocket.service';

/**
 * A definition of Lion data
 */
export interface Lion {
    locale: any;
    lion: any;
}

/**
 * ONOS GUI -- Lion -- Localization Utilities
 */
@Injectable({
  providedIn: 'root',
})
export class LionService {

    ubercache: any[] = [];
    loadCbs = new Map<string, () => void>([]); // A map of functions

    /**
     * Handler for uberlion event from WSS
     */
    uberlion(data: Lion) {
        this.ubercache = data.lion;

        this.log.info('LION service: Locale... [' + data.locale + ']');
        this.log.info('LION service: Bundles installed...');

        for (const p in this.ubercache) {
            if (this.ubercache[p]) {
                this.log.info('            :=> ', p);
            }
        }
        // If any component had registered a callback, call it now
        // that LION is loaded
        for (const cbname of this.loadCbs.keys()) {
            this.log.debug('Updating', cbname, 'with LION');
            this.loadCbs.get(cbname)();
        }

        this.log.debug('LION service: uber-lion bundle received:', data);
    }

    constructor(
        private log: LogService,
        private wss: WebSocketService
    ) {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            ['uberlion', (data) => this.uberlion(data) ]
        ]));
        this.log.debug('LionService constructed');
    }

    /**
     * Returns a lion bundle (function) for the given bundle ID (string)
     * returns a function that takes a string and returns a string
     */
    bundle(bundleId: string): (string) => string {
        let bundleObj = this.ubercache[bundleId];

        if (!bundleObj) {
            this.log.warn('No lion bundle registered:', bundleId);
            bundleObj = {};
        }

        return (key) =>  {
            return bundleObj[key] || '?' + key + '?';
        };
    }
}
