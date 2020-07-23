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
import { LogService } from '../log.service';

const UICONTEXT = '/onos/ui/';
const RSSUFFIX = UICONTEXT + 'rs/';
const WSSUFFIX = UICONTEXT + 'websock/';

/**
 * ONOS GUI -- Remote -- General Purpose URL Functions
 */
@Injectable({
  providedIn: 'root',
})
export class UrlFnService {
    constructor(
        private log: LogService,
        @Inject('Window') private w: any
    ) {
        this.log.debug('UrlFnService constructed');
    }

    matchSecure(protocol: string): string {
        const p: string = this.w.location.protocol;
        const secure: boolean = (p.includes('https') || p.includes('wss'));
        return secure ? protocol + 's' : protocol;
    }

    /* A little bit of funky here. It is possible that ONOS sits
     * behind a proxy and has an app prefix, e.g.
     *      http://host:port/my/app/onos/ui...
     * This bit of regex grabs everything after the host:port and
     * before the UICONTEXT (/onos/ui/) and uses that as an app
     * prefix by inserting it back into the WS URL.
     * If no prefix, then no insert.
     */
    urlBase(protocol: string, port: string = '', host: string = ''): string {
        const match = this.w.location.href.match('.*//[^/]+/(.+)' + UICONTEXT);
        const appPrefix = match ? '/' + match[1] : '';

        return this.matchSecure(protocol) +
            '://' +
            (host === '' ? this.w.location.hostname : host) +
            ':' +
            (port === '' ? this.w.location.port : port) +
            appPrefix;
    }

    httpPrefix(suffix: string): string {
        return this.urlBase('http') + suffix;
    }

    wsPrefix(suffix: string, wsport: string, host: string): string {
        return this.urlBase('ws', wsport, host) + suffix;
    }

    rsUrl(path: string): string {
        return this.httpPrefix(RSSUFFIX) + path;
    }

    wsUrl(path: string, wsport?: string, host?: string): string {
        return this.wsPrefix(WSSUFFIX, wsport, host) + path;
    }
}
