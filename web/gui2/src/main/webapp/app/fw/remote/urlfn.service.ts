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
import { LogService } from '../../log.service';

const uiContext = '/onos/ui/';
const rsSuffix = uiContext + 'rs/';
const wsSuffix = uiContext + 'websock/';

/**
 * ONOS GUI -- Remote -- General Purpose URL Functions
 */
@Injectable()
export class UrlFnService {
    constructor(
        private log: LogService,
        private window: Window
    ) {
        this.log.debug('UrlFnService constructed');
    }

    matchSecure(protocol: string): string {
        const p: string = window.location.protocol;
        const secure: boolean = (p === 'https' || p === 'wss');
        return secure ? protocol + 's' : protocol;
    }

    /* A little bit of funky here. It is possible that ONOS sits
     * behind a proxy and has an app prefix, e.g.
     *      http://host:port/my/app/onos/ui...
     * This bit of regex grabs everything after the host:port and
     * before the uiContext (/onos/ui/) and uses that as an app
     * prefix by inserting it back into the WS URL.
     * If no prefix, then no insert.
     */
    urlBase(protocol: string, port: string, host: string): string {
        const match = window.location.href.match('.*//[^/]+/(.+)' + uiContext);
        const appPrefix = match ? '/' + match[1] : '';

        return this.matchSecure(protocol) + '://' +
            (host || window.location.hostname) + ':' +
            (port || window.location.port) + appPrefix;
    }

    httpPrefix(suffix: string): string {
        return this.urlBase('http', '', '') + suffix;
    }

    wsPrefix(suffix: string, wsport: any, host: string): string {
        return this.urlBase('ws', wsport, host) + suffix;
    }

    rsUrl(path: string): string {
        return this.httpPrefix(rsSuffix) + path;
    }

    wsUrl(path: string, wsport: any, host: string): string {
        return this.wsPrefix(wsSuffix, wsport, host) + path;
    }
}
