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
import { TestBed, inject } from '@angular/core/testing';

import { LogService } from '../../../../app/log.service';
import { ConsoleLoggerService } from '../../../../app/consolelogger.service';
import { WebSocketService } from '../../../../app/fw/remote/websocket.service';
import { FnService } from '../../../../app/fw/util/fn.service';
import { GlyphService } from '../../../../app/fw/svg/glyph.service';
import { UrlFnService } from '../../../../app/fw/remote/urlfn.service';
import { WSock } from '../../../../app/fw/remote/wsock.service';

class MockFnService {}

class MockGlyphService {}

class MockUrlFnService {}

class MockWSock {}

/**
 * ONOS GUI -- Remote -- Web Socket Service - Unit Tests
 */
describe('WebSocketService', () => {
    let log: LogService;
    const windowMock = <any>{ location: <any> { hostname: 'localhost' } };

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            providers: [WebSocketService,
                { provide: FnService, useClass: MockFnService },
                { provide: LogService, useValue: log },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: UrlFnService, useClass: MockUrlFnService },
                { provide: WSock, useClass: MockWSock },
                { provide: Window, useValue: windowMock },
            ]
        });
    });

    it('should be created', inject([WebSocketService], (service: WebSocketService) => {
        expect(service).toBeTruthy();
    }));
});
