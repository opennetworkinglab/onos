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
import { TestBed, inject } from '@angular/core/testing';
import { of } from 'rxjs';

import { LogService } from '../log.service';
import { ConsoleLoggerService } from '../consolelogger.service';
import { ActivatedRoute, Params } from '@angular/router';
import { FnService } from '../util/fn.service';
import { GlyphService } from '../svg/glyph.service';
import { LionService } from './lion.service';
import { UrlFnService } from '../remote/urlfn.service';
import { WSock } from '../remote/wsock.service';
import { WebSocketService, WsOptions } from '../remote/websocket.service';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockWSock {}

class MockGlyphService {}

class MockUrlFnService {}

/**
 * ONOS GUI -- Lion -- Localization Utilities - Unit Tests
 */
describe('LionService', () => {
    let log: LogService;
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
            providers: [LionService,
                { provide: FnService, useValue: fs },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: LogService, useValue: log },
                { provide: UrlFnService, useClass: MockUrlFnService },
                { provide: WSock, useClass: MockWSock },
                { provide: WebSocketService, useClass: WebSocketService },
                { provide: 'Window', useFactory: (() => windowMock ) },
            ]
        });
    });

    it('should be created', inject([LionService], (service: LionService) => {
        expect(service).toBeTruthy();
    }));
});
