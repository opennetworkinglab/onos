/*
 * Copyright 2017-present Open Networking Foundation
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
import { LogService } from '../../log.service';
import { WebSocketService } from '../remote/websocket.service';

/**
 * A definition of Lion data
 */
interface Lion {
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

    ubercache: any[];

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
        let bundle = this.ubercache[bundleId];

        if (!bundle) {
            this.log.warn('No lion bundle registered:', bundleId);
            bundle = {};
        }

        return this.getKey;
    }

    getKey(key: string): string {
        return this.bundle[key] || '%' + key + '%';
    }
}
