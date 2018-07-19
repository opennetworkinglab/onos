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
import { FnService } from './fn.service';
import { LogService } from '../../log.service';
import { WebSocketService } from '../remote/websocket.service';

/**
 * ONOS GUI -- Util -- User Preference Service
 */
@Injectable({
    providedIn: 'root',
})
export class PrefsService {
    protected Prefs;
    protected handlers: string[] = [];
    cache: any;
    listeners: any;
    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected wss: WebSocketService
    ) {
        this.cache = {};
        this.wss.bindHandlers(new Map<string, (data) => void>([
            [this.Prefs, (data) => this.updatePrefs(data)]
        ]));
        this.handlers.push(this.Prefs);

        this.log.debug('PrefsService constructed');
    }

    setPrefs(name: string, obj: any) {
        // keep a cached copy of the object and send an update to server
        this.cache[name] = obj;
        this.wss.sendEvent('updatePrefReq', { key: name, value: obj });
    }
    updatePrefs(data: any) {
        this.cache = data;
        this.listeners.forEach(function (lsnr) { lsnr(); });
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

    getPrefs(name: string, defaults: any, qparams?: string) {
        const obj = Object.assign({}, defaults || {}, this.cache[name] || {});

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
    mergePrefs(name: string, obj: any) {
        const merged = this.cache[name] || {};
        this.setPrefs(name, Object.assign(merged, obj));
    }

    addListener(listener: any) {
        this.listeners.push(listener);
    }

    removeListener(listener: any) {
        this.listeners = this.listeners.filter(function (obj) { return obj === listener; });
    }

}
