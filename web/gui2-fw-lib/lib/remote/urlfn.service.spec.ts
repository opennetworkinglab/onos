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
import { TestBed, inject } from '@angular/core/testing';

import { LogService } from '../log.service';
import { ConsoleLoggerService } from '../consolelogger.service';
import { UrlFnService } from './urlfn.service';
import { FnService } from '../util/fn.service';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

/**
 * ONOS GUI -- Remote -- General Functions - Unit Tests
 */
describe('UrlFnService', () => {
    let log: LogService;
    let ufs: UrlFnService;
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;

    beforeEach(() => {
        log = new ConsoleLoggerService();
        ar = new MockActivatedRoute({'debug': 'TestService'});
        windowMock = <any>{
            location: <any> {
                hostname: '',
                host: '',
                port: '',
                protocol: '',
                search: { debug: 'true'},
                href: ''
            }
        };

        fs = new FnService(ar, log, windowMock);

        TestBed.configureTestingModule({
            providers: [UrlFnService,
                { provide: LogService, useValue: log },
                { provide: 'Window', useFactory: (() => windowMock ) },
            ]
        });

        ufs = TestBed.get(UrlFnService);
    });

    function setLoc(prot: string, h: string, p: string, ctx: string = '') {
        windowMock.location.host = h;
        windowMock.location.hostname = h;
        windowMock.location.port = p;
        windowMock.location.protocol = prot;
        windowMock.location.href = prot + '://' + h + ':' + p +
            ctx + '/onos/ui/';
    }

    it('should define UrlFnService', () => {
        expect(ufs).toBeDefined();
    });

    it('should define api functions', () => {
        expect(fs.areFunctions(ufs, [
            'rsUrl', 'wsUrl', 'urlBase', 'httpPrefix',
            'wsPrefix', 'matchSecure'
        ])).toBeTruthy();
    });

    it('should return the correct (http) RS url', () => {
        setLoc('http', 'foo', '123');
        expect(ufs.rsUrl('path')).toEqual('http://foo:123/onos/ui/rs/path');
    });

    it('should return the correct (https) RS url', () => {
        setLoc('https', 'foo', '123');
        expect(ufs.rsUrl('path')).toEqual('https://foo:123/onos/ui/rs/path');
    });

    it('should return the correct (ws) WS url', () => {
        setLoc('http', 'foo', '123');
        expect(ufs.wsUrl('path')).toEqual('ws://foo:123/onos/ui/websock/path');
    });

    it('should return the correct (wss) WS url', () => {
        setLoc('https', 'foo', '123');
        expect(ufs.wsUrl('path')).toEqual('wss://foo:123/onos/ui/websock/path');
    });

    it('should allow us to define an alternate WS port', () => {
        setLoc('http', 'foo', '123');
        expect(ufs.wsUrl('xyyzy', '456')).toEqual('ws://foo:456/onos/ui/websock/xyyzy');
    });

    it('should allow us to define an alternate host', () => {
        setLoc('http', 'foo', '123');
        expect(ufs.wsUrl('core', '456', 'bar')).toEqual('ws://bar:456/onos/ui/websock/core');
    });

    it('should allow us to inject an app context', () => {
        setLoc('http', 'foo', '123', '/my/app');
        expect(ufs.wsUrl('path')).toEqual('ws://foo:123/my/app/onos/ui/websock/path');
    });

});
