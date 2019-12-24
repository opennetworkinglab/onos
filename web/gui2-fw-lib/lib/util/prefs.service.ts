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
import {Inject, Injectable} from '@angular/core';
import { FnService } from './fn.service';
import { LogService } from '../log.service';
import { WebSocketService } from '../remote/websocket.service';

const UPDATE_PREFS: string = 'updatePrefs';
const UPDATE_PREFS_REQ: string = 'updatePrefReq';


/**
 * ONOS GUI -- Util -- User Preference Service
 */
@Injectable({
    providedIn: 'root',
})
export class PrefsService {
    protected handlers: string[] = [];
    cache: Object;
    listeners: ((data) => void)[] = [];
    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService,
        @Inject('Window') private window: any
    ) {
        this.wss.bindHandlers(new Map<string, (data) => void>([
            [UPDATE_PREFS, (data) => this.updatePrefs(data)]
        ]));
        this.handlers.push(UPDATE_PREFS);

        // When index.html is fetched it is served up by MainIndexResource.java
        // which fetches userPrefs in to the global scope.
        // After that updates are done through WebSocket
        this.cache = (<any>Object).assign({}, this.window['userPrefs']);

        this.log.debug('PrefsService constructed');
    }

    setPrefs(name: string, obj: Object) {
        // keep a cached copy of the object and send an update to server
        this.cache[name] = obj;
        this.wss.sendEvent(UPDATE_PREFS_REQ, { key: name, value: obj });
    }
    updatePrefs(data: any) {
        this.cache = data;
        this.listeners.forEach((lsnr) => lsnr(data) );
    }

    asNumbers(obj: any, keys?: any, not?: any) {
        if (!obj) {
            return null;
        }

        const skip = {};
        if (not) {
            keys.forEach(k => {
                skip[k] = 1;
            }
            );
        }

        if (!keys || not) {
            // do them all
            Array.from(obj).forEach((v, k) => {
                if (!not || !skip[k]) {
                    obj[k] = Number(obj[k]);
                }
            });
        } else {
            // do the explicitly named keys
            keys.forEach(k => {
                obj[k] = Number(obj[k]);
            });
        }
        return obj;
    }

    getPrefs(name: string, defaults: Object, qparams?: string) {
        const obj = (<any>Object).assign({}, defaults || {}, this.cache[name] || {});

        // if query params are specified, they override...
        if (this.fs.isO(qparams)) {
            obj.forEach(k => {
                if (qparams.hasOwnProperty(k)) {
                    obj[k] = qparams[k];
                }
            });
        }
        return obj;
    }

    // merge preferences:
    // The assumption here is that obj is a sparse object, and that the
    //  defined keys should overwrite the corresponding values, but any
    //  existing keys that are NOT explicitly defined here should be left
    //  alone (not deleted).
    mergePrefs(name: string, obj: any): void {
        const merged = this.cache[name] || {};
        this.setPrefs(name, (<any>Object).assign(merged, obj));
    }

    /**
     * Add a listener function
     * This will get called back when an 'updatePrefs' message is received on WSS
     * @param listener a function that can accept one param - data
     */
    addListener(listener: (data) => void): void {
        this.listeners.push(listener);
    }

    removeListener(listener: (data) => void) {
        this.listeners = this.listeners.filter((obj) => obj !== listener);
    }

}
