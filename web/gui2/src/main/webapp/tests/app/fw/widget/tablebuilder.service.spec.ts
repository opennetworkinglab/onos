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
import { TableBuilderService } from '../../../../app/fw/widget/tablebuilder.service';
import { FnService } from '../../../../app/fw//util/fn.service';
import { LoadingService } from '../../../../app/fw/layer/loading.service';
import { WebSocketService } from '../../../../app/fw/remote/websocket.service';

class MockFnService {}

class MockLoadingService {}

class MockWebSocketService {}

/*
 ONOS GUI -- Widget -- Table Builder Service - Unit Tests
 */
describe('TableBuilderService', () => {
    let log: LogService;

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            providers: [TableBuilderService,
                { provide: FnService, useClass: MockFnService },
                { provide: LoadingService, useClass: MockLoadingService },
                { provide: LogService, useValue: log },
                { provide: WebSocketService, useClass: MockWebSocketService },

            ]
        });
    });

    it('should be created', inject([TableBuilderService], (service: TableBuilderService) => {
        expect(service).toBeTruthy();
    }));
});
