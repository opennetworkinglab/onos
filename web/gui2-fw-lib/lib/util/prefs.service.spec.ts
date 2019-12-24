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
import { PrefsService } from '../util/prefs.service';
import { FnService } from '../util/fn.service';
import { WebSocketService } from '../remote/websocket.service';

class MockFnService {}

class MockWebSocketService {
    createWebSocket() {}
    isConnected() { return false; }
    unbindHandlers() {}
    bindHandlers() {}
}

/**
 * ONOS GUI -- Util -- User Preference Service - Unit Tests
 */
describe('PrefsService', () => {
    let log: LogService;
    let windowMock: Window;

    windowMock = <any>{
        location: <any> {
            hostname: 'foo',
            host: 'foo',
            port: '80',
            protocol: 'http',
            search: { debug: 'true'},
            href: 'ws://foo:123/onos/ui/websock/path',
            absUrl: 'ws://foo:123/onos/ui/websock/path'
        }
    };

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            providers: [PrefsService,
                { provide: LogService, useValue: log },
                { provide: FnService, useClass: MockFnService },
                { provide: 'Window', useFactory: (() => windowMock ) },
                { provide: WebSocketService, useClass: MockWebSocketService },
            ]
        });
    });

    it('should be created', inject([PrefsService], (service: PrefsService) => {
        expect(service).toBeTruthy();
    }));
});
