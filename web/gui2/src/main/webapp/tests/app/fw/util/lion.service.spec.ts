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
import { TestBed, inject } from '@angular/core/testing';
import { of } from 'rxjs';

import { LogService } from '../../../../app/log.service';
import { ConsoleLoggerService } from '../../../../app/consolelogger.service';
import { ActivatedRoute, Params } from '@angular/router';
import { FnService } from '../../../../app/fw/util/fn.service';
import { GlyphService } from '../../../../app/fw/svg/glyph.service';
import { LionService } from '../../../../app/fw/util/lion.service';
import { UrlFnService } from '../../../../app/fw/remote/urlfn.service';
import { WSock } from '../../../../app/fw/remote/wsock.service';
import { WebSocketService, WsOptions } from '../../../../app/fw/remote/websocket.service';

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
