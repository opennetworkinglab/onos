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
 */
import { TestBed, inject } from '@angular/core/testing';

import { LogService } from '../../../../app/log.service';
import { ConsoleLoggerService } from '../../../../app/consolelogger.service';
import { EditableTextService } from '../../../../app/fw/layer/editabletext.service';
import { KeyService } from '../../../../app/fw/util/key.service';
import { WebSocketService } from '../../../../app/fw/remote/websocket.service';

class MockKeyService {}

class MockWebSocketService {}

/**
 * ONOS GUI -- Layer -- Editable Text Service - Unit Tests
 */
describe('EditableTextService', () => {
    let log: LogService;

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            providers: [EditableTextService,
                { provide: LogService, useValue: log },
                { provide: KeyService, useClass: MockKeyService },
                { provide: WebSocketService, useClass: MockWebSocketService },
            ]
        });
    });

    it('should be created', inject([EditableTextService], (service: EditableTextService) => {
        expect(service).toBeTruthy();
    }));
});
